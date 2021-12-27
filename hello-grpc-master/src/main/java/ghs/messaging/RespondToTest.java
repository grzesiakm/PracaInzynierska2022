package ghs.messaging;

import generated.NodeMessage;
import ghs.models.Edge;
import ghs.models.Node;

import static generated.NodeMessage.NODE_STATE.SLEEPING;
import static generated.NodeMessage.TYPE.ACCEPT;
import static generated.NodeMessage.TYPE.REJECT;
import static ghs.models.EdgeState.BASIC;
import static ghs.models.EdgeState.REJECTED;

public class RespondToTest implements ResponseHandler {

    public static void respond(Node node, NodeMessage nodeMessage) {
        Edge messageEdge = node.getMessageEdge(nodeMessage);

        if (node.getNodeState() == SLEEPING) {
            node.wakeup();
        }

        if (nodeMessage.getLvl() > node.getFragmentLvl()) {
            //add to the message queue as a last element
            node.enqueue(nodeMessage);
        } else if (nodeMessage.getFragmentId() == node.getFragmentId()) {
            if (messageEdge.getState() == BASIC) {
                node.setMessageCount(node.getMessageCount() + 1);
                node.getEdges().stream()
                        .filter(edge -> messageEdge.getToNodeId() == edge.getToNodeId())
                        .findAny().orElseThrow()
                        .setState(REJECTED);
            }

            if (node.getTestEdge() != null && node.getTestEdge().getToNodeId() != messageEdge.getToNodeId()) {
                NodeMessage reject = NodeMessage.newBuilder()
                        .setFromNodeId(node.getNodeId())
                        .setToNodeId(messageEdge.getToNodeId())
                        .setType(REJECT)
                        .build();
                node.setMessageCount(node.getMessageCount() + 1);
                node.sendMessage(reject);
            } else {
                ResponseHandler.testProcedure(node);
            }
        } else {
            NodeMessage accept = NodeMessage.newBuilder()
                    .setFromNodeId(node.getNodeId())
                    .setToNodeId(messageEdge.getToNodeId())
                    .setType(ACCEPT)
                    .build();
            node.setMessageCount(node.getMessageCount() + 1);
            node.sendMessage(accept);
        }

        logger.info("Node " + node.mapToPort(node.getNodeId())
                + " responded to TEST from "
                + node.mapToPort(nodeMessage.getFromNodeId())
        );
    }
}
