package ghs.models;

import generated.NodeMessage;
import ghs.messaging.*;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static generated.NodeMessage.NODE_STATE.FOUND;
import static generated.NodeMessage.NODE_STATE.SLEEPING;
import static generated.NodeMessage.TYPE.*;
import static ghs.models.EdgeState.*;

public class Node extends Actor {

    private static final Logger logger = Logger.getLogger(Node.class.getName());

    private final int nodeId;
    private int fragmentId;
    private int fragmentLvl;
    private int bestWeight;
    private NodeMessage.NODE_STATE nodeState;
    private final List<Edge> edges;
    private int rec;
    private Edge bestEdge;
    private Edge testEdge;
    private Edge inBranch;
    private int messageCount;
    private final BlockingQueue<Boolean> stop;

    public List<Edge> getEdges() {
        return edges;
    }

    public int getNodeId() {
        return nodeId;
    }

    public int getFragmentId() {
        return fragmentId;
    }

    public void setFragmentId(int fragmentId) {
        this.fragmentId = fragmentId;
    }

    public int getFragmentLvl() {
        return fragmentLvl;
    }

    public void setFragmentLvl(int fragmentLvl) {
        this.fragmentLvl = fragmentLvl;
    }

    public int getBestWeight() {
        return bestWeight;
    }

    public void setBestWeight(int bestWeight) {
        this.bestWeight = bestWeight;
    }

    public NodeMessage.NODE_STATE getNodeState() {
        return nodeState;
    }

    public void setNodeState(NodeMessage.NODE_STATE nodeState) {
        this.nodeState = nodeState;
    }

    public int getRec() {
        return rec;
    }

    public void setRec(int rec) {
        this.rec = rec;
    }

    public Edge getBestEdge() {
        return bestEdge;
    }

    public void setBestEdge(Edge bestEdge) {
        this.bestEdge = bestEdge;
    }

    public Edge getTestEdge() {
        return testEdge;
    }

    public void setTestEdge(Edge testEdge) {
        this.testEdge = testEdge;
    }

    public Edge getInBranch() {
        return inBranch;
    }

    public void setInBranch(Edge inBranch) {
        this.inBranch = inBranch;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public BlockingQueue<Boolean> getStop() {
        return stop;
    }

    public Node(int nodeId, List<Edge> edges, BlockingQueue<Boolean> stop) {
        super(50050 + nodeId);
        this.nodeId = nodeId;
        this.edges = edges;
        this.messageCount = 0;
        this.stop = stop;

        fragmentId = this.nodeId;
        fragmentLvl = 0;
        nodeState = SLEEPING;
        bestWeight = Integer.MAX_VALUE;
        bestEdge = null;
        inBranch = null;
        testEdge = null;
        rec = 0;
    }

    public List<Edge> getOtherBranchNeighbours(Edge neighbour) {
        return this.edges.stream()
                .filter(edge -> ((edge.getToNodeId() != neighbour.getToNodeId()) && (edge.getState() == BRANCH)))
                .collect(Collectors.toList());
    }

    public List<Edge> getBranchEdges() {
        return this.edges.stream()
                .filter(edge -> edge.getState() == BRANCH)
                .collect(Collectors.toList());
    }

    public List<Edge> getBasicNeighbours() {
        return this.edges.stream()
                .filter(edge -> edge.getState() == BASIC)
                .collect(Collectors.toList());
    }

    public Edge getMessageEdge(NodeMessage nodeMessage) {
        return this.edges.stream()
                .filter(edge -> nodeMessage.getFromNodeId() == edge.getToNodeId())
                .findAny().orElseThrow();
    }

    public void wakeUp() {

        Edge minWeightEdge = edges.stream().min(Comparator.comparing(Edge::getWeight)).orElseThrow();
        this.edges.stream()
                .filter(edge -> minWeightEdge.getToNodeId() == edge.getToNodeId())
                .findAny().orElseThrow()
                .setState(BRANCH);

        nodeState = FOUND;

        logger.info("Node " + mapToPort(this.nodeId) + " woke up and it's minWeightEdge is connected to " + mapToPort(minWeightEdge.getToNodeId()));

        NodeMessage connect = NodeMessage.newBuilder()
                .setFromNodeId(this.nodeId)
                .setToNodeId(minWeightEdge.getToNodeId())
                .setType(CONNECT)
                .setLvl(fragmentLvl)
                .build();

        sendMessage(connect);
    }

    @Override
    protected void onMessageDequeued(NodeMessage nodeMessage) {
        if (stop.size() > 0) {
            super.finish();
        } else {
            switch (nodeMessage.getType()) {
                case CONNECT:
                    RespondToConnect.respond(this, nodeMessage);
                    break;
                case INITIATE:
                    RespondToInitiate.respond(this, nodeMessage);
                    break;
                case TEST:
                    RespondToTest.respond(this, nodeMessage);
                    break;
                case ACCEPT:
                    RespondToAccept.respond(this, nodeMessage);
                    break;
                case REJECT:
                    RespondToReject.respond(this, nodeMessage);
                    break;
                case REPORT:
                    RespondToReport.respond(this, nodeMessage);
                    break;
                case CHANGE_CORE:
                    RespondToChangeCore.respond(this, nodeMessage);
                    break;
                default:
                    System.err.println("Invalid message type!");
            }
        }
    }
}
