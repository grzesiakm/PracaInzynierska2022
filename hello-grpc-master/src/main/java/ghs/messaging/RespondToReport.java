package ghs.messaging;

import generated.NodeMessage;
import ghs.models.Edge;
import ghs.models.Node;

import static generated.NodeMessage.NODE_STATE.FIND;

public class RespondToReport implements ResponseHandler {

    public static void respond(Node node, NodeMessage nodeMessage) {
        Edge messageEdge = node.getMessageEdge(nodeMessage);

        if (messageEdge.getToNodeId() != node.getInBranch().getToNodeId()) {
            if (nodeMessage.getWeight() < node.getBestWeight()) {
                node.setBestWeight(nodeMessage.getWeight());
                node.setBestEdge(messageEdge);
            }

            node.setRec(node.getRec() + 1);
            ResponseHandler.reportProcedure(node);
        } else {
            if (node.getNodeState() == FIND) {
                //add to the message queue as a last element
                node.enqueue(nodeMessage);
            } else if (nodeMessage.getWeight() > node.getBestWeight()) {
                ResponseHandler.changeCoreProcedure(node);
            } else if (nodeMessage.getWeight() == node.getBestWeight() && nodeMessage.getWeight() == Integer.MAX_VALUE) {
                node.getStop().add(true);
                System.out.println("--------------Algorithm STOPS--------------");
            }
        }

        logger.info("Node " + node.mapToPort(node.getNodeId())
                + " responded to REPORT from "
                + node.mapToPort(nodeMessage.getFromNodeId())
        );
    }
}
