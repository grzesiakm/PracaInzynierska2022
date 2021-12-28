package ghs.messaging;

import generated.NodeMessage;
import ghs.models.Edge;
import ghs.models.Node;

import java.util.logging.Logger;

import static generated.NodeMessage.NODE_STATE.FOUND;
import static generated.NodeMessage.TYPE.*;
import static generated.NodeMessage.TYPE.CONNECT;
import static ghs.models.EdgeState.BRANCH;

public interface ResponseHandler {

    Logger logger = Logger.getLogger(ResponseHandler.class.getName());

    static void testProcedure(Node node) {
        Edge edgeToSend = null;
        int minWeight = Integer.MAX_VALUE;

        for (Edge e : node.getBasicNeighbours()) {
            if (e.getWeight() < minWeight) {
                minWeight = e.getWeight();
                edgeToSend = e;
            }
        }

        if (edgeToSend != null) {
            node.setTestEdge(edgeToSend);

            NodeMessage test = NodeMessage.newBuilder()
                    .setFromNodeId(node.getNodeId())
                    .setToNodeId(node.getTestEdge().getToNodeId())
                    .setType(TEST)
                    .setLvl(node.getFragmentLvl())
                    .setFragmentId(node.getFragmentId())
                    .build();
            node.setMessageCount(node.getMessageCount() + 1);
            node.sendMessage(test);
        } else {
            node.setTestEdge(null);
            reportProcedure(node);
        }
    }

    static void reportProcedure(Node node) {
        int size = node.getOtherBranchNeighbours(node.getInBranch()).size();

        if (node.getRec() == size && node.getTestEdge() == null) {
            node.setNodeState(FOUND);

            NodeMessage report = NodeMessage.newBuilder()
                    .setFromNodeId(node.getNodeId())
                    .setToNodeId(node.getInBranch().getToNodeId())
                    .setType(REPORT)
                    .setWeight(node.getBestWeight())
                    .build();
            node.setMessageCount(node.getMessageCount() + 1);
            node.sendMessage(report);
        }
    }

    static void changeCoreProcedure(Node node) {
        if (node.getBestEdge() != null && node.getBestEdge().getState() == BRANCH) {
            NodeMessage changeCore = NodeMessage.newBuilder()
                    .setFromNodeId(node.getNodeId())
                    .setToNodeId(node.getBestEdge().getToNodeId())
                    .setType(CHANGE_CORE)
                    .build();
            node.setMessageCount(node.getMessageCount() + 1);
            node.sendMessage(changeCore);
        } else if (node.getBestEdge() != null) {
            node.getEdges().stream()
                    .filter(edge -> node.getBestEdge().getToNodeId() == edge.getToNodeId())
                    .findAny().orElseThrow()
                    .setState(BRANCH);

            NodeMessage connect = NodeMessage.newBuilder()
                    .setFromNodeId(node.getNodeId())
                    .setToNodeId(node.getBestEdge().getToNodeId())
                    .setType(CONNECT)
                    .setLvl(node.getFragmentLvl())
                    .build();
            node.setMessageCount(node.getMessageCount() + 1);
            node.sendMessage(connect);
        } else {
            System.err.println("bestEdge is null --> GHS will not work this time...");
        }
    }
}
