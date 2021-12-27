package ghs.messaging;

import generated.NodeMessage;
import ghs.models.Edge;
import ghs.models.Node;

import java.util.List;

import static generated.NodeMessage.NODE_STATE.FIND;
import static generated.NodeMessage.TYPE.INITIATE;

public class RespondToInitiate implements ResponseHandler {

    public static void respond(Node node, NodeMessage nodeMessage) {
        Edge messageEdge = node.getMessageEdge(nodeMessage);
        node.setInBranch(messageEdge);

        node.setFragmentLvl(nodeMessage.getLvl());
        node.setFragmentId(nodeMessage.getFragmentId());
        node.setNodeState(nodeMessage.getNodeState());


        node.setBestEdge(null);
        node.setBestWeight(Integer.MAX_VALUE);
        node.setTestEdge(null);

        List<Edge> otherEdges = node.getOtherBranchNeighbours(messageEdge);

        for (Edge e : otherEdges) {
            NodeMessage initiate = NodeMessage.newBuilder()
                    .setFromNodeId(node.getNodeId())
                    .setToNodeId(e.getToNodeId())
                    .setType(INITIATE)
                    .setLvl(node.getFragmentLvl())
                    .setFragmentId(node.getFragmentId())
                    .setNodeState(node.getNodeState())
                    .build();
            node.setMessageCount(node.getMessageCount() + 1);
            node.sendMessage(initiate);
        }

        if (node.getNodeState() == FIND) {
            node.setRec(0);
            ResponseHandler.testProcedure(node);
        }

        logger.info("Node " + node.mapToPort(node.getNodeId())
                + " responded to INITIATE from "
                + node.mapToPort(nodeMessage.getFromNodeId())
        );
    }
}
