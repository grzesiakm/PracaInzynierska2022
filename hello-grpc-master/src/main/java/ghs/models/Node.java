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
import static generated.NodeMessage.TYPE.*;
import static ghs.models.EdgeState.*;

public class Node extends Actor {

    private static final Logger logger = Logger.getLogger(Node.class.getName());

    public int getNodeId() {
        return nodeId;
    }

    private int nodeId;
    private int fragmentId;
    private int fragmentLvl;
    private int bestWeight;
    private NodeMessage.NODE_STATE nodeState;
    private final List<Edge> edges;
    private int rec;
    private Edge bestEdge;
    private Edge testEdge;
    private Edge inBranch;
    private boolean halt;

    public Edge getBestEdge() {
        return bestEdge;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public Node(int nodeId, List<Edge> edges) {
        super(50050 + nodeId);
        this.nodeId = nodeId;
        this.edges = edges;
        this.halt = false;


//        Edge minWeightEdge = edges.stream().min(Comparator.comparing(Edge::getWeight)).orElseThrow();
//        this.edges.stream()
//                    .filter(edge -> minWeightEdge.getToNodeId() == edge.getToNodeId())
//                    .findAny().orElseThrow()
//                    .setState(BRANCH);

        fragmentId = nodeId;
        fragmentLvl = 0;
        nodeState = SLEEPING;
//        nodeState = FOUND;
        bestWeight = Integer.MAX_VALUE;
        bestEdge = null;
        inBranch = null;
        testEdge = null;
        rec = 0;

//        logger.info("Node " + this.nodeId + " woke up");

//        NodeMessage connect = NodeMessage.newBuilder()
//                .setFromNodeId(this.nodeId)
//                .setToNodeId(minWeightEdge.getToNodeId())
//                .setType(CONNECT)
//                .setLvl(fragmentLvl)
//                .build();
//
//        sendMessage(connect);
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

    private Edge getMessageEdge(NodeMessage nodeMessage) {
        return this.edges.stream()
                .filter(edge -> nodeMessage.getFromNodeId() == edge.getToNodeId())
                .findAny().orElseThrow();
    }

    public void wakeup() {

        Edge minWeightEdge = edges.stream().min(Comparator.comparing(Edge::getWeight)).orElseThrow();
        this.edges.stream()
                .filter(edge -> minWeightEdge.getToNodeId() == edge.getToNodeId())
                .findAny().orElseThrow()
                .setState(BRANCH);
        nodeState = FOUND;

        logger.info("Node " + this.nodeId + " woke up and it's minWeightEdge is " + minWeightEdge.getToNodeId());

        NodeMessage connect = NodeMessage.newBuilder()
                .setFromNodeId(this.nodeId)
                .setToNodeId(minWeightEdge.getToNodeId())
                .setType(CONNECT)
                .setLvl(fragmentLvl)
                .build();

        sendMessage(connect);
    }

    protected void respondToConnect(NodeMessage nodeMessage) {
        if (this.nodeState == SLEEPING){
            wakeup();
        }
        Edge messageEdge = getMessageEdge(nodeMessage);

        if (nodeMessage.getLvl() < fragmentLvl) {
            this.edges.stream()
                    .filter(edge -> messageEdge.getToNodeId() == edge.getToNodeId())
                    .findAny().orElseThrow()
                    .setState(BRANCH);

            NodeMessage initiate = NodeMessage.newBuilder()
                    .setFromNodeId(this.nodeId)
                    .setToNodeId(messageEdge.getToNodeId())
                    .setType(INITIATE)
                    .setLvl(fragmentLvl)
                    .setFragmentId(fragmentId)
                    .setNodeState(nodeState)
                    .build();

            sendMessage(initiate);
        }

        else if (messageEdge.getState() == BASIC) {
            //add to the message queue as a last element
            enqueue(nodeMessage);
        }

        else {
            //send Initiate with this lvl+1, weight of edge and FIND state
            NodeMessage initiate = NodeMessage.newBuilder()
                    .setFromNodeId(this.nodeId)
                    .setToNodeId(messageEdge.getToNodeId())
                    .setType(INITIATE)
                    .setLvl(fragmentLvl + 1)
                    .setFragmentId(messageEdge.getWeight())
                    .setNodeState(FIND)
                    .build();

            sendMessage(initiate);
        }

        logger.info("Node " + this.nodeId + " responded to CONNECT from " + nodeMessage.getFromNodeId());
    }

    private void respondToInitiate(NodeMessage nodeMessage) {
        Edge messageEdge = getMessageEdge(nodeMessage);
        inBranch = messageEdge;

        fragmentLvl = nodeMessage.getLvl();
        fragmentId = nodeMessage.getFragmentId();
        nodeState = nodeMessage.getNodeState();


        bestEdge = null;
        bestWeight = Integer.MAX_VALUE;
        testEdge = null;

        List<Edge> otherEdges = getOtherBranchNeighbours(messageEdge);

        for (Edge e : otherEdges) {
            NodeMessage initiate = NodeMessage.newBuilder()
                    .setFromNodeId(this.nodeId)
                    .setToNodeId(e.getToNodeId())
                    .setType(INITIATE)
                    .setLvl(fragmentLvl)
                    .setFragmentId(fragmentId)
                    .setNodeState(nodeState)
                    .build();

            sendMessage(initiate);
        }

        if (nodeState == FIND) {
            rec = 0;
            testProcedure();
        }

        logger.info("Node " + this.nodeId + " responded to INITIATE from " + nodeMessage.getFromNodeId());
    }

    public void testProcedure() {
        Edge edgeToSend = null;
        int minWeight = Integer.MAX_VALUE;

        for (Edge e : getBasicNeighbours()) {
            if (e.getWeight() < minWeight) {
                minWeight = e.getWeight();
                edgeToSend = e;
            }
        }

        if (edgeToSend != null) {
            testEdge = edgeToSend;

            NodeMessage test = NodeMessage.newBuilder()
                    .setFromNodeId(this.nodeId)
                    .setToNodeId(testEdge.getToNodeId())
                    .setType(TEST)
                    .setLvl(fragmentLvl)
                    .setFragmentId(fragmentId)
                    .build();

            sendMessage(test);
        }

        else {
            testEdge = null;
            reportProcedure();
        }

//        List<Edge> basicNeighbours = getBasicNeighbours();
//        if (basicNeighbours.isEmpty()) {
//            this.testEdge = null;
//            reportProcedure();
//        } else {
//            basicNeighbours.sort(Comparator.comparingInt(Edge::getWeight));
//            this.testEdge = basicNeighbours.get(0);
//            sendTest(this.testEdge, this.fragmentLvl, this.fragmentId);
//        }
    }

    private void respondToTest(NodeMessage nodeMessage) {
        Edge messageEdge = getMessageEdge(nodeMessage);
        if (this.nodeState == SLEEPING){
            wakeup();
        }
        if (nodeMessage.getLvl() > fragmentLvl) {
            //add to the message queue as a last element
            enqueue(nodeMessage);
        }

        else if (nodeMessage.getFragmentId() == this.fragmentId) {
            if (messageEdge.getState() == BASIC) {
                this.edges.stream()
                        .filter(edge -> messageEdge.getToNodeId() == edge.getToNodeId())
                        .findAny().orElseThrow()
                        .setState(REJECTED);
            }

            if (testEdge != null && testEdge.getToNodeId() != messageEdge.getToNodeId()) {
                NodeMessage reject = NodeMessage.newBuilder()
                        .setFromNodeId(this.nodeId)
                        .setToNodeId(messageEdge.getToNodeId())
                        .setType(REJECT)
                        .build();

                sendMessage(reject);
            }

            else {
                testProcedure();
            }
        }

        else {
            NodeMessage accept = NodeMessage.newBuilder()
                    .setFromNodeId(this.nodeId)
                    .setToNodeId(messageEdge.getToNodeId())
                    .setType(ACCEPT)
                    .build();

            sendMessage(accept);
        }

        logger.info("Node " + this.nodeId + " responded to TEST from " + nodeMessage.getFromNodeId());
    }

    private void respondToAccept(NodeMessage nodeMessage) {
        Edge messageEdge = getMessageEdge(nodeMessage);
        testEdge = null;

        if (messageEdge.getWeight() < bestWeight) {
            bestWeight = messageEdge.getWeight();
            bestEdge = messageEdge;
        }

        reportProcedure();

        logger.info("Node " + this.nodeId + " responded to ACCEPT from " + nodeMessage.getFromNodeId());
    }

    private void respondToReject(NodeMessage nodeMessage) {
        Edge messageEdge = getMessageEdge(nodeMessage);

        if (messageEdge.getState() == BASIC) {
            this.edges.stream()
                    .filter(edge -> messageEdge.getToNodeId() == edge.getToNodeId())
                    .findAny().orElseThrow()
                    .setState(REJECTED);
        }

        testProcedure();

        logger.info("Node " + this.nodeId + " responded to REJECT from " + nodeMessage.getFromNodeId());
    }

    public void reportProcedure() {
        int size = 0;

        for (Edge e : getOtherBranchNeighbours(inBranch)){
            size++;
        }

        if (rec == size && testEdge == null) {
            nodeState = FOUND;
            NodeMessage report = NodeMessage.newBuilder()
                    .setFromNodeId(this.nodeId)
                    .setToNodeId(inBranch.getToNodeId())
                    .setType(REPORT)
                    .setWeight(bestWeight)
                    .build();

            sendMessage(report);
        }
//        if (this.findCount == 0 && testEdge == null) {
//            this.nodeState = FOUND;
//            sendReport(this.inBranch, this.bestWeight);
//        }
    }

    private void respondToReport(NodeMessage nodeMessage) {
        Edge messageEdge = getMessageEdge(nodeMessage);

        if (messageEdge.getToNodeId() != inBranch.getToNodeId()) {
            if (nodeMessage.getWeight() < bestWeight) {
                bestWeight = nodeMessage.getWeight();
                bestEdge = messageEdge;
            }

            rec += 1;
            reportProcedure();
        }

        else {
            if (nodeState == FIND) {
                //add to the message queue as a last element
                enqueue(nodeMessage);
            }

            else if (nodeMessage.getWeight() > bestWeight) {
                changeCoreProcedure();
            }

            else if (nodeMessage.getWeight() == bestWeight && nodeMessage.getWeight() == Integer.MAX_VALUE) {
                halt = true;
                System.out.println("------------Algorithm STOPS--------------");
                this.finish();
            }
        }

        logger.info("Node " + this.nodeId + " responded to REPORT from " + nodeMessage.getFromNodeId());
    }

    private void changeCoreProcedure() {
        if (bestEdge.getState() == BRANCH) {
            NodeMessage changeCore = NodeMessage.newBuilder()
                    .setFromNodeId(this.nodeId)
                    .setToNodeId(bestEdge.getToNodeId())
                    .setType(CHANGE_CORE)
                    .build();

            sendMessage(changeCore);
        }

        else {
            this.edges.stream()
                    .filter(edge -> this.bestEdge.getToNodeId() == edge.getToNodeId())
                    .findAny().orElseThrow()
                    .setState(BRANCH);

            NodeMessage connect = NodeMessage.newBuilder()
                    .setFromNodeId(this.nodeId)
                    .setToNodeId(bestEdge.getToNodeId())
                    .setType(CONNECT)
                    .setLvl(fragmentLvl)
                    .build();

            sendMessage(connect);
        }
    }

    private void respondToChangeCore(NodeMessage nodeMessage) {
        changeCoreProcedure();

        logger.info("Node " + this.nodeId + " responded to CHANGE_CORE to " + nodeMessage.getFromNodeId());
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
                respondToChangeCore(nodeMessage);
                break;
        }
    }
}
