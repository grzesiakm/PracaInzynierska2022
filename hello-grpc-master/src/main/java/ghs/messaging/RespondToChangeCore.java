package ghs.messaging;

import generated.NodeMessage;
import ghs.models.Node;

public class RespondToChangeCore implements ResponseHandler {

    public static void respond(Node node, NodeMessage nodeMessage) {
        ResponseHandler.changeCoreProcedure(node);

        logger.info("Node " + node.mapToPort(node.getNodeId())
                + " responded to CHANGE_CORE to "
                + node.mapToPort(nodeMessage.getFromNodeId())
        );
    }
}
