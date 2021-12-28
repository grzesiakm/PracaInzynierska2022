package ghs.messaging;

import generated.NodeMessage;
import ghs.models.Edge;
import ghs.models.Node;

public class RespondToAccept implements ResponseHandler {

    public static void respond(Node node, NodeMessage nodeMessage) {
        Edge messageEdge = node.getMessageEdge(nodeMessage);
        node.setTestEdge(null);

        if (messageEdge.getWeight() < node.getBestWeight()) {
            node.setBestWeight(messageEdge.getWeight());
            node.setBestEdge(messageEdge);
        }

        ResponseHandler.reportProcedure(node);

        logger.info("Node " + node.mapToPort(node.getNodeId())
                + " responded to ACCEPT from "
                + node.mapToPort(nodeMessage.getFromNodeId())
        );
    }
}
