package ghs.models;

import generated.NodeMessage;
import ghs.messaging.Actor;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static generated.NodeMessage.NODE_STATE.FOUND;
import static generated.NodeMessage.NODE_STATE.FIND;
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

        logger.info("Node " + mapToPort(this.nodeId) + " woke up and it's minWeightEdge is connected to " + mapToPort(minWeightEdge.getToNodeId()));

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
            messageCount++;
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
            messageCount++;
            sendMessage(initiate);
        }

        logger.info("Node "+mapToPort(this.nodeId)+" responded to CONNECT from "+mapToPort(nodeMessage.getFromNodeId()));
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
            messageCount++;
            sendMessage(initiate);
        }

        if (nodeState == FIND) {
            rec = 0;
            testProcedure();
        }

        logger.info("Node "+mapToPort(this.nodeId)+" responded to INITIATE from "+mapToPort(nodeMessage.getFromNodeId()));
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
            messageCount++;
            sendMessage(test);
        }

        else {
            testEdge = null;
            reportProcedure();
        }
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
                messageCount++;
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
                messageCount++;
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
            messageCount++;
            sendMessage(accept);
        }

        logger.info("Node "+mapToPort(this.nodeId)+" responded to TEST from "+mapToPort(nodeMessage.getFromNodeId()));
    }

    private void respondToAccept(NodeMessage nodeMessage) {
        Edge messageEdge = getMessageEdge(nodeMessage);
        testEdge = null;

        if (messageEdge.getWeight() < bestWeight) {
            bestWeight = messageEdge.getWeight();
            bestEdge = messageEdge;
        }

        reportProcedure();

        logger.info("Node "+mapToPort(this.nodeId)+" responded to ACCEPT from "+mapToPort(nodeMessage.getFromNodeId()));
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

        logger.info("Node "+mapToPort(this.nodeId)+" responded to REJECT from "+mapToPort(nodeMessage.getFromNodeId()));
    }

    public void reportProcedure() {
        int size = getOtherBranchNeighbours(inBranch).size();

        if (rec == size && testEdge == null) {
            nodeState = FOUND;

            NodeMessage report = NodeMessage.newBuilder()
                    .setFromNodeId(this.nodeId)
                    .setToNodeId(inBranch.getToNodeId())
                    .setType(REPORT)
                    .setWeight(bestWeight)
                    .build();
            messageCount++;
            sendMessage(report);
        }
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
                stop.add(true);
                System.out.println("--------------Algorithm STOPS--------------");
            }
        }

        logger.info("Node "+mapToPort(this.nodeId)+" responded to REPORT from "+mapToPort(nodeMessage.getFromNodeId()));
    }

    private void changeCoreProcedure() {
        if (bestEdge != null && bestEdge.getState() == BRANCH) {
            NodeMessage changeCore = NodeMessage.newBuilder()
                    .setFromNodeId(this.nodeId)
                    .setToNodeId(bestEdge.getToNodeId())
                    .setType(CHANGE_CORE)
                    .build();
            messageCount++;
            sendMessage(changeCore);
        }

        else if (bestEdge != null){
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
            messageCount++;
            sendMessage(connect);
        }

        else {
            System.out.println("bestEdge is null");
        }
    }

    private void respondToChangeCore(NodeMessage nodeMessage) {
        changeCoreProcedure();

        logger.info("Node "+mapToPort(this.nodeId)+" responded to CHANGE_CORE to "+mapToPort(nodeMessage.getFromNodeId()));
    }

    @Override
    protected void onMessageDequeued(NodeMessage nodeMessage) {
        if (stop.size() > 0) {
            super.finish();
        }
        else {
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
}
