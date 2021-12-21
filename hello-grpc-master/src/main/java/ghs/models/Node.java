package ghs.models;

import generated.MessageHandlerGrpc;
import generated.NodeMessage;
import generated.Ok;
import ghs.messaging.Actor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static generated.NodeMessage.NODE_STATE.FOUND;
import static generated.NodeMessage.NODE_STATE.FIND;
import static generated.NodeMessage.NODE_STATE.SLEEPING;
import static ghs.models.EdgeState.*;

public class Node extends Actor {

    private static final Logger logger = Logger.getLogger(Node.class.getName());
    private int fragmentId;
    private final List<Edge> edges;
    private int fragmentLvl;
    private NodeMessage.NODE_STATE nodeState;
    private int findCount;

    public Edge getBestEdge() {
        return bestEdge;
    }

    private Edge bestEdge;

    public Edge getInBranch() {
        return inBranch;
    }

    private Edge inBranch;
    private Edge testEdge;
    private int bestWeight;
    private boolean halt;

    public List<Edge> getEdges() {
        return edges;
    }

    public Node(int port, List<Edge> edges) {
        super(port);
        this.fragmentId = port;
        this.edges = edges;
        this.nodeState = SLEEPING;
        this.findCount = Integer.MIN_VALUE;
        this.bestEdge = null;
        this.halt = false;
    }

    public List<Edge> getOtherBranchNeighbours(Edge neighbour) {
        return this.edges.stream()
                .filter(edge -> ((edge.getEdgeId() != neighbour.getEdgeId()) && (edge.getState() == BRANCH)))
                .collect(Collectors.toList());
    }

    public List<Edge> getBasicNeighbours() {
        return this.edges.stream()
                .filter(edge -> edge.getState() == BASIC)
                .collect(Collectors.toList());
    }

    private Edge getMessageEdge(NodeMessage nodeMessage) {
        return this.edges.stream()
                .filter(edge -> nodeMessage.getEdgeId() == edge.getEdgeId())
                .findAny().orElseThrow();
    }

    public void wakeup() {
        this.bestEdge = edges.stream().min(Comparator.comparing(Edge::getWeight)).orElseThrow();
        bestEdge.state = BRANCH;
        this.fragmentLvl = 0;
        this.nodeState = FOUND;
        this.findCount = 0;

        logger.info("Node on port " + this.fragmentId + " woke up");

        sendConnect(0);
    }

    private void sendConnect(int fragmentLvl) {
        ManagedChannel channel = ManagedChannelBuilder
                .forTarget(buildTarget(this.bestEdge.getToNodeId()))
                .usePlaintext()
                .build();

        Ok ok = MessageHandlerGrpc
                .newBlockingStub(channel)
                .handleMessage(NodeMessage.newBuilder()
                        .setFromVertexId(this.fragmentId)
                        .setType(NodeMessage.TYPE.CONNECT)
                        .setEdgeId(this.bestEdge.getEdgeId())
                        .setLvl(fragmentLvl).build());

        try {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        logger.info("Node on port " + this.fragmentId + " sent CONNECT to " + this.bestEdge.getToNodeId());
    }

    protected void respondToConnect(NodeMessage nodeMessage) {
        if (this.nodeState == SLEEPING){
            wakeup();
        }
        Edge messageEdge = getMessageEdge(nodeMessage);
        if (nodeMessage.getLvl() < this.fragmentLvl) {
            this.edges.stream()
                    .filter(edge -> messageEdge.getEdgeId() == edge.getEdgeId())
                    .findAny().orElseThrow()
                    .setState(BRANCH);
            sendInitiate(messageEdge, this.fragmentLvl, this.fragmentId, this.nodeState);
            if (this.nodeState == FIND) {
                this.findCount++;
            }
        } else if (messageEdge.getState() == BASIC) {
            //add to the message queue as a last element
            enqueue(nodeMessage);
        } else {
            //send Initiate with this lvl+1, sender's id and FIND state
            sendInitiate(messageEdge, this.fragmentLvl + 1, nodeMessage.getWeight(), FIND);
        }

        logger.info("Node on port " + this.fragmentId + " responded to CONNECT to " + messageEdge);
    }

    private void sendInitiate(Edge edge, int fragmentLvl, int fragmentId, NodeMessage.NODE_STATE nodeState) {
        ManagedChannel channel = ManagedChannelBuilder
                .forTarget(buildTarget(edge.getToNodeId()))
                .usePlaintext()
                .build();

        Ok ok = MessageHandlerGrpc
                .newBlockingStub(channel)
                .handleMessage(NodeMessage.newBuilder()
                        .setFromVertexId(fragmentId)// this.fragmentId
                        .setType(NodeMessage.TYPE.INITIATE)
                        .setEdgeId(edge.getEdgeId())
                        .setLvl(fragmentLvl)
                        .setNodeState(nodeState).build());

        try {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        logger.info("Node on port " + this.fragmentId + " sent INITIATE");
    }

    private void respondToInitiate(NodeMessage nodeMessage) {
        this.fragmentLvl = nodeMessage.getLvl();
        this.fragmentId = nodeMessage.getFromVertexId();
        this.nodeState = nodeMessage.getNodeState();
        Edge messageEdge = getMessageEdge(nodeMessage);
        this.inBranch = messageEdge;
        this.bestEdge = null;
        this.bestWeight = Integer.MAX_VALUE;

        List<Edge> otherEdges = getOtherBranchNeighbours(messageEdge);

        for (Edge e : otherEdges) {
            sendInitiate(e, nodeMessage.getLvl(), nodeMessage.getFromVertexId(), nodeMessage.getNodeState());
            if (nodeMessage.getNodeState() == FIND) {
                this.findCount++;
            }
        }

        if (nodeMessage.getNodeState() == FIND) {
            testProcedure();
        }

        logger.info("Node on port " + this.fragmentId + " responded to INITIATE");
    }

    public void testProcedure() {
        List<Edge> basicNeighbours = getBasicNeighbours();
        if (basicNeighbours.isEmpty()) {
            this.testEdge = null;
            reportProcedure();
        } else {
            basicNeighbours.sort(Comparator.comparingInt(Edge::getWeight));
            this.testEdge = basicNeighbours.get(0);
            sendTest(this.testEdge, this.fragmentLvl, this.fragmentId);
        }
    }

    private void sendTest(Edge edge, int fragmentLvl, int fragmentId) {
        ManagedChannel channel = ManagedChannelBuilder
                .forTarget(buildTarget(edge.getToNodeId()))
                .usePlaintext()
                .build();

        Ok ok = MessageHandlerGrpc
                .newBlockingStub(channel)
                .handleMessage(NodeMessage.newBuilder()
                        .setFromVertexId(fragmentId)//was with this.
                        .setEdgeId(edge.getEdgeId())
                        .setType(NodeMessage.TYPE.TEST)
                        .setLvl(fragmentLvl).build());

        try {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        logger.info("Node on port " + this.fragmentId + " sent TEST");
    }

    private void respondToTest(NodeMessage nodeMessage) {
        Edge messageEdge = getMessageEdge(nodeMessage);
        if (this.nodeState == SLEEPING){
            wakeup();
        }
        if (nodeMessage.getLvl() > this.fragmentLvl) {
            //add to the message queue as a last element
            enqueue(nodeMessage);
        }
        else if (nodeMessage.getFromVertexId() != this.fragmentId) {
            sendAccept(messageEdge);
        }
        else if (messageEdge.getState() == BASIC) {
            this.edges.stream()
                    .filter(edge -> messageEdge.getEdgeId() == edge.getEdgeId())
                    .findAny().orElseThrow()
                    .setState(REJECTED);
            //todo
            //testEdge null-check
            if (testEdge != null && testEdge.getEdgeId() != messageEdge.getEdgeId()) {
                sendReject(messageEdge);
            }
            else testProcedure();
        }

        logger.info("Node on port " + this.fragmentId + " responded to TEST");
    }

    private void sendAccept(Edge edge) {
        ManagedChannel channel = ManagedChannelBuilder
                .forTarget(buildTarget(edge.getToNodeId()))
                .usePlaintext()
                .build();

        Ok ok = MessageHandlerGrpc
                .newBlockingStub(channel)
                .handleMessage(NodeMessage.newBuilder()
                        .setFromVertexId(this.fragmentId)
                        .setType(NodeMessage.TYPE.ACCEPT)
                        .setEdgeId(edge.getEdgeId())
                        .build());

        try {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        logger.info("Node on port " + this.fragmentId + " sent ACCEPT");
    }

    private void respondToAccept(NodeMessage nodeMessage) {
        Edge messageEdge = getMessageEdge(nodeMessage);
        this.testEdge = null;
        if (nodeMessage.getWeight() < this.bestWeight) {
            this.bestEdge = messageEdge;
            this.bestWeight = nodeMessage.getWeight();
        }
        reportProcedure();

        logger.info("Node on port " + this.fragmentId + " responded to ACCEPT");
    }

    private void sendReject(Edge edge) {
        ManagedChannel channel = ManagedChannelBuilder
                .forTarget(buildTarget(edge.getToNodeId()))
                .usePlaintext()
                .build();

        Ok ok = MessageHandlerGrpc
                .newBlockingStub(channel)
                .handleMessage(NodeMessage.newBuilder()
                        .setFromVertexId(this.fragmentId)
                        .setType(NodeMessage.TYPE.REJECT)
                        .setEdgeId(edge.getEdgeId())
                        .build());

        try {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        logger.info("Node on port " + this.fragmentId + " sent REJECT");
    }

    private void respondToReject(NodeMessage nodeMessage) {
        Edge messageEdge = getMessageEdge(nodeMessage);

        if (messageEdge.getState() == BASIC) {
            this.edges.stream()
                    .filter(edge -> messageEdge.getEdgeId() == edge.getEdgeId())
                    .findAny().orElseThrow()
                    .setState(REJECTED);
            testProcedure();
        }

        logger.info("Node on port " + this.fragmentId + " responded to REJECT");
    }

    public void reportProcedure() {
        if (this.findCount == 0 && testEdge == null) {
            this.nodeState = FOUND;
            sendReport(this.inBranch, this.bestWeight);
        }
    }

    private void sendReport(Edge inBranch, int bestWeight) {
        ManagedChannel channel = ManagedChannelBuilder
                .forTarget(buildTarget(inBranch.getToNodeId()))
                .usePlaintext()
                .build();

        Ok ok = MessageHandlerGrpc
                .newBlockingStub(channel)
                .handleMessage(NodeMessage.newBuilder()
                        .setFromVertexId(this.fragmentId)
                        .setType(NodeMessage.TYPE.REPORT)
                        .setEdgeId(inBranch.getEdgeId())
                        .setWeight(bestWeight).build());

        try {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        logger.info("Node on port " + this.fragmentId + " sent REPORT");
    }

    private void respondToReport(NodeMessage nodeMessage) {
        Edge messageEdge = getMessageEdge(nodeMessage);
        if (messageEdge.getEdgeId() != this.inBranch.getEdgeId()) {
            this.findCount--;
            if (nodeMessage.getWeight() < this.bestWeight) {
                this.bestWeight = nodeMessage.getWeight();
                this.bestEdge = messageEdge;
            }
            reportProcedure();
        } else if (this.nodeState == FIND)
            //add to the message queue as a last element
            enqueue(nodeMessage);
        else if (nodeMessage.getWeight() > this.bestWeight) {
            changeCoreProcedure();
        }
        else if (nodeMessage.getWeight() == this.bestWeight && this.bestWeight == Integer.MAX_VALUE) {
            this.halt = true;
            this.finish();
        }

        logger.info("Node on port " + this.fragmentId + " responded to REPORT " + nodeMessage.getFromVertexId());
    }

    private void changeCoreProcedure() {
        if (this.bestEdge.getState() == BRANCH)
            sendChangeCore();
        else {
            sendConnect(this.fragmentLvl);
            this.edges.stream()
                    .filter(edge -> this.bestEdge.getEdgeId() == edge.getEdgeId())
                    .findAny().orElseThrow()
                    .setState(BRANCH);

        }
    }

    private void sendChangeCore() {
        ManagedChannel channel = ManagedChannelBuilder
                .forTarget(buildTarget(this.bestEdge.getToNodeId()))
                .usePlaintext()
                .build();

        Ok ok = MessageHandlerGrpc
                .newBlockingStub(channel)
                .handleMessage(NodeMessage.newBuilder()
                        .setFromVertexId(this.fragmentId)
                        .setType(NodeMessage.TYPE.CHANGE_CORE)
                        .build());

        try {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        logger.info("Node on port " + this.fragmentId + " sent CHANGE_CORE");
    }

    private void respondToChangeCore() {
        changeCoreProcedure();

        logger.info("Node on port " + this.fragmentId + " responded to CHANGE_CORE");
    }

    @Override
    protected void OnMessageDequeued(NodeMessage nodeMessage) {
        switch (nodeMessage.getType()) {
            case CONNECT:
                respondToConnect(nodeMessage);
                break;
            case INITIATE:
                respondToInitiate(nodeMessage);
                break;
            case TEST:
                respondToTest(nodeMessage);
                break;
            case ACCEPT:
                respondToAccept(nodeMessage);
                break;
            case REJECT:
                respondToReject(nodeMessage);
                break;
            case REPORT:
                respondToReport(nodeMessage);
                break;
            case CHANGE_CORE:
                respondToChangeCore();
                break;
        }
    }
}
