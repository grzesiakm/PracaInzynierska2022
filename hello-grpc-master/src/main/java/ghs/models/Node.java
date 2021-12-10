package ghs.models;

import generated.MessageHandlerGrpc;
import generated.NodeMessage;
import generated.Ok;
import ghs.messaging.Actor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Node extends Actor {

    private int fragmentId;
    private final List<Edge> edges;
    private int fragmentLvl;
    private NodeState nodeState;
    private int findCount;
    private Edge bestEdge;
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
        this.nodeState = NodeState.SLEEPING;
        this.findCount = Integer.MIN_VALUE;
        this.bestEdge = null;
        this.halt = false;
    }

    public List<Edge> getOtherNeighbours(Edge neighbour) {
        return edges.stream().filter(edge -> ((edge != neighbour) && (edge.getState()
                .equals(EdgeState.BRANCH)))).collect(Collectors.toList());
    }

    public List<Edge> getBasicNeighbours() {
        return edges.stream().filter(edge -> edge.getState().equals(EdgeState.BASIC))
                .collect(Collectors.toList());
    }

    public void wakeup() {
        this.bestEdge = edges.stream().min(Comparator.comparing(Edge::getWeight)).orElseThrow();
        bestEdge.state = EdgeState.BRANCH;
        this.fragmentLvl = 0;
        this.nodeState = NodeState.FOUND;
        this.findCount = 0;

        sendConnect(bestEdge, this.fragmentLvl);
    }

    private void sendConnect(Edge minEdge, int fragmentLvl) {
        ManagedChannel channel = ManagedChannelBuilder
                .forTarget(buildTarget(minEdge.getToFragmentId()))
                .usePlaintext()
                .build();

        Ok ok = MessageHandlerGrpc
                .newBlockingStub(channel)
                .handleMessage(NodeMessage.newBuilder().setFrom(fragmentId).setType(NodeMessage.TYPE.CONNECT).setLvl(fragmentLvl).build());

        try {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void respondToConnect(Edge sender, int lvl) {
        if (lvl < fragmentLvl) {
            edges.stream().filter(sender::equals).findAny().ifPresent(edge -> edge.setState(EdgeState.BRANCH));
            sendInitiate(sender, fragmentLvl, nodeState);
            if (nodeState == NodeState.FIND) findCount++;
        } else if (sender.getState() == EdgeState.BASIC) {
            //add to the message queue as a last element
            enqueue(NodeMessage.newBuilder().setType(NodeMessage.TYPE.CONNECT).setFrom(sender.getToFragmentId()).build());
        } else {
            //send Initiate with this lvl+1, sender's id and FIND state
            sendInitiate(sender, fragmentLvl + 1, NodeState.FIND);
        }

        System.out.println("Node on port " + this.fragmentId + " responded to CONNECT");
    }

    private void sendInitiate(Edge edge, int fragmentLvl, NodeState nodeState) {
        ManagedChannel channel = ManagedChannelBuilder
                .forTarget(buildTarget(edge.getToFragmentId()))
                .usePlaintext()
                .build();

        Ok ok = MessageHandlerGrpc
                .newBlockingStub(channel)
                .handleMessage(NodeMessage.newBuilder().setFrom(fragmentId).setType(NodeMessage.TYPE.INITIATE).setLvl(fragmentLvl)
                        .setNodeState(nodeState.toString()).build());

        try {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void respondToInitiate(Edge sender, int lvl, NodeState state) {
        fragmentLvl = lvl;
        fragmentId = sender.getToFragmentId();
        nodeState = state;
        inBranch = sender;
        bestEdge = null;
        bestWeight = Integer.MAX_VALUE;

        List<Edge> otherEdges = getOtherNeighbours(sender);
        for (Edge e : otherEdges) {
            sendInitiate(e, fragmentLvl, nodeState);
            if (nodeState == NodeState.FIND) findCount++;
        }

        if (nodeState == NodeState.FIND) testProcedure();

        System.out.println("Node on port " + this.fragmentId + " responded to INITIATE");
    }

    public void testProcedure() {
        List<Edge> basicNeighbours = getBasicNeighbours();
        if (basicNeighbours.isEmpty()) {
            testEdge = null;
            reportProcedure();
        } else {
            basicNeighbours.sort(Comparator.comparingInt(Edge::getWeight));
            testEdge = basicNeighbours.get(0);
            sendTest(testEdge, fragmentLvl);
        }
    }

    private void sendTest(Edge edge, int fragmentLvl) {
        ManagedChannel channel = ManagedChannelBuilder
                .forTarget(buildTarget(edge.getToFragmentId()))
                .usePlaintext()
                .build();

        Ok ok = MessageHandlerGrpc
                .newBlockingStub(channel)
                .handleMessage(NodeMessage.newBuilder().setFrom(fragmentId).setType(NodeMessage.TYPE.TEST)
                        .setLvl(fragmentLvl).build());

        try {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void respondToTest(Edge sender, int lvl) {
        if (lvl > fragmentLvl)
            //add to the message queue as a last element
            enqueue(NodeMessage.newBuilder().setType(NodeMessage.TYPE.TEST).setFrom(sender.getToFragmentId()).setLvl(lvl)
                    .build());
        else if (sender.getToFragmentId() != fragmentId) sendAccept(sender);
        else if (this.getBasicNeighbours().contains(sender)) {
            for (Edge edge : edges) {
                if (edge.equals(sender)) edge.setState(EdgeState.REJECTED);
            }
            if (testEdge != sender) sendReject(sender);
            else testProcedure();
        }

        System.out.println("Node on port " + this.fragmentId + " responded to LINK-TEST-PROBE");
    }

    private void sendAccept(Edge edge) {
        ManagedChannel channel = ManagedChannelBuilder
                .forTarget(buildTarget(edge.getToFragmentId()))
                .usePlaintext()
                .build();

        Ok ok = MessageHandlerGrpc
                .newBlockingStub(channel)
                .handleMessage(NodeMessage.newBuilder().setFrom(fragmentId).setType(NodeMessage.TYPE.ACCEPT).build());

        try {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void respondToAccept(Edge sender) {
        testEdge = null;
        if (sender.getWeight() < bestWeight) {
            bestEdge = sender;
            bestWeight = sender.getWeight();
        }
        reportProcedure();

        System.out.println("Node on port " + this.fragmentId + " responded to ACCEPT");
    }

    private void sendReject(Edge edge) {
        ManagedChannel channel = ManagedChannelBuilder
                .forTarget(buildTarget(edge.getToFragmentId()))
                .usePlaintext()
                .build();

        Ok ok = MessageHandlerGrpc
                .newBlockingStub(channel)
                .handleMessage(NodeMessage.newBuilder().setFrom(fragmentId).setType(NodeMessage.TYPE.REJECT).build());

        try {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void respondToReject(Edge sender) {
        if (this.getBasicNeighbours().contains(sender)) {
            for (Edge edge : edges) {
                if (edge.equals(sender)) edge.setState(EdgeState.REJECTED);
            }
        }
        testProcedure();

        System.out.println("Node on port " + this.fragmentId + " responded to REJECT");
    }

    public void reportProcedure() {
        if (findCount == 0 && testEdge == null) {
            nodeState = NodeState.FOUND;
            sendReport(inBranch, bestWeight);
        }
    }

    private void sendReport(Edge inBranch, int bestWeight) {
        ManagedChannel channel = ManagedChannelBuilder
                .forTarget(buildTarget(inBranch.getToFragmentId()))
                .usePlaintext()
                .build();

        Ok ok = MessageHandlerGrpc
                .newBlockingStub(channel)
                .handleMessage(NodeMessage.newBuilder().setFrom(fragmentId).setType(NodeMessage.TYPE.REPORT)
                        .setWeight(bestWeight).build());

        try {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void respondToReport(Edge sender){
        if (sender != inBranch) {
            findCount--;
            if (sender.getWeight() < bestWeight) {
                bestWeight = sender.getWeight();
                bestEdge = sender;
            }
            reportProcedure();
        }
        else if (nodeState.equals(NodeState.FIND))
            //add to the message queue as a last element
            enqueue(NodeMessage.newBuilder().setType(NodeMessage.TYPE.REPORT).setFrom(sender.getToFragmentId())
                    .setWeight(sender.getWeight()).build());
        else if (sender.getWeight() > bestWeight) changeCoreProcedure();
        else if (sender.getWeight() == bestWeight && bestWeight == Integer.MAX_VALUE) halt = true;

        System.out.println("Node on port " + this.fragmentId + " responded to REPORT");
    }

    private void changeCoreProcedure() {
        if (bestEdge.getState().equals(EdgeState.BRANCH))
            sendChangeCore(bestEdge);
        else {
            sendConnect(bestEdge, fragmentLvl);
            edges.stream().filter(bestEdge::equals).findAny().ifPresent(edge -> edge.setState(EdgeState.BRANCH));
        }
    }

    private void sendChangeCore(Edge bestEdge) {
        ManagedChannel channel = ManagedChannelBuilder
                .forTarget(buildTarget(bestEdge.getToFragmentId()))
                .usePlaintext()
                .build();

        Ok ok = MessageHandlerGrpc
                .newBlockingStub(channel)
                .handleMessage(NodeMessage.newBuilder().setFrom(fragmentId).setType(NodeMessage.TYPE.CHANGE_CORE).build());

        try {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void respondToChangeCore() {
        changeCoreProcedure();

        System.out.println("Node on port " + this.fragmentId + " responded to CHANGE-CORE");
    }

    @Override
    protected void OnMessageDequeued(NodeMessage nodeMessage) {

        switch (nodeMessage.getType()) {
            case CONNECT:
                respondToConnect(new Edge(nodeMessage.getWeight(), nodeMessage.getFrom()), nodeMessage.getLvl());
                break;
            case INITIATE:
                break;
        }
    }
}
