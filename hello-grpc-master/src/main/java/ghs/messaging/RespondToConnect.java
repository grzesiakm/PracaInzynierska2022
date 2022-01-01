package ghs.messaging;

import generated.NodeMessage;
import ghs.models.Edge;
import ghs.models.Node;

import static generated.NodeMessage.NODE_STATE.FIND;
import static generated.NodeMessage.NODE_STATE.SLEEPING;
import static generated.NodeMessage.TYPE.INITIATE;
import static ghs.models.EdgeState.BASIC;
import static ghs.models.EdgeState.BRANCH;

public class RespondToConnect implements ResponseHandler {

    public static void respond(Node node, NodeMessage nodeMessage) {
        if (node.getNodeState() == SLEEPING) {
            node.wakeUp();
        }

        Edge messageEdge = node.getMessageEdge(nodeMessage);

        if (nodeMessage.getLvl() < node.getFragmentLvl()) {
            node.getEdges().stream()
                    .filter(edge -> messageEdge.getToNodeId() == edge.getToNodeId())
                    .findAny().orElseThrow()
                    .setState(BRANCH);

            NodeMessage initiate = NodeMessage.newBuilder()
                    .setFromNodeId(node.getNodeId())
                    .setToNodeId(messageEdge.getToNodeId())
                    .setType(INITIATE)
                    .setLvl(node.getFragmentLvl())
                    .setFragmentId(node.getFragmentId())
                    .setNodeState(node.getNodeState())
                    .build();
            node.setMessageCount(node.getMessageCount() + 1);
            node.sendMessage(initiate);
        } else if (messageEdge.getState() == BASIC) {
            //add to the message queue as a last element
            node.enqueue(nodeMessage);
        } else {
            //send RespondToInitiate with this lvl+1, weight of edge and FIND state
            NodeMessage initiate = NodeMessage.newBuilder()
                    .setFromNodeId(node.getNodeId())
                    .setToNodeId(messageEdge.getToNodeId())
                    .setType(INITIATE)
                    .setLvl(node.getFragmentLvl() + 1)
                    .setFragmentId(messageEdge.getWeight())
                    .setNodeState(FIND)
                    .build();
            node.setMessageCount(node.getMessageCount() + 1);
            node.sendMessage(initiate);
        }

        logger.info("Node " + node.mapToPort(node.getNodeId())
                + " responded to CONNECT from "
                + node.mapToPort(nodeMessage.getFromNodeId())
        );
    }
}
