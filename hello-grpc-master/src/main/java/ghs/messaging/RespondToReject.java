package ghs.messaging;

import generated.NodeMessage;
import ghs.models.Edge;
import ghs.models.Node;

import static ghs.models.EdgeState.BASIC;
import static ghs.models.EdgeState.REJECTED;

public class RespondToReject implements ResponseHandler {

    public static void respond(Node node, NodeMessage nodeMessage) {
        Edge messageEdge = node.getMessageEdge(nodeMessage);

        if (messageEdge.getState() == BASIC) {
            node.getEdges().stream()
                    .filter(edge -> messageEdge.getToNodeId() == edge.getToNodeId())
                    .findAny().orElseThrow()
                    .setState(REJECTED);
        }

        ResponseHandler.testProcedure(node);

        logger.info("Node " + node.mapToPort(node.getNodeId())
                + " responded to REJECT from "
                + node.mapToPort(nodeMessage.getFromNodeId())
        );
    }
}
