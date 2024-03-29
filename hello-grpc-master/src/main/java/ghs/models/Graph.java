package ghs.models;

import com.google.common.base.Stopwatch;
import files.ReadAdjacencyMatrix;
import files.ReadEdgesConvertToMatrix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;


public class Graph {
    private final List<Node> nodes;
    private final int[][] adjacencyMatrix;
    private static final BlockingQueue<Boolean> stop = new LinkedBlockingQueue<>();

    public Graph(int[][] adjacencyMatrix) {
        this.adjacencyMatrix = adjacencyMatrix;
        nodes = createGraph();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Node node : nodes) {
            builder.append(node.getPort())
                    .append(" with neighbours: ")
                    .append(node.getEdges().stream()
                            .map(e -> e.getToNodeId() + " " + e.getWeight()).collect(Collectors.toList()))
                    .append("\n");
        }
        return builder.toString();
    }

    private int getNodeCount() {
        return this.adjacencyMatrix.length;
    }

    private HashMap<Integer, Integer> getWeightedNeighbors(int vertex) {
        HashMap<Integer, Integer> neighbors = new HashMap<>();
        for (int i = 0; i < getNodeCount(); i++) {
            if (adjacencyMatrix[vertex][i] != 0) neighbors.put(i, adjacencyMatrix[vertex][i]);
        }
        return neighbors;
    }

    private List<Node> createGraph() {
        List<Node> nodes = new ArrayList<>();

        for (int i = 0; i < getNodeCount(); i++) {
            HashMap<Integer, Integer> map = getWeightedNeighbors(i);
            List<Edge> neighbours = map.entrySet().stream()
                    .map(entry -> new Edge(entry.getValue(), entry.getKey()))
                    .collect(Collectors.toList());
            nodes.add(new Node(i, neighbours, stop));
        }
        return nodes;
    }

    public static void calculateMST(String filename, String input) throws InterruptedException, IOException {
        final int[][] adjacencyMatrix;

        switch (input) {
            case "edges":
                adjacencyMatrix = ReadEdgesConvertToMatrix.readEdgesFromFile(filename);
                break;
            case "matrix":
                adjacencyMatrix = ReadAdjacencyMatrix.readMatrixFromFile(filename);
                break;
            default:
                adjacencyMatrix = ReadAdjacencyMatrix.readMatrixFromFile("matrix6.txt");
                break;
        }

        Graph graph = new Graph(adjacencyMatrix);
        System.out.println(graph);

        for (Node node : graph.nodes) {
            System.out.println("NodeId " + node.getNodeId() + " with neighbours " + node.getEdges());
        }

        Stopwatch timer = Stopwatch.createStarted();

        for (Node node : graph.nodes) {
            node.initialize();
        }

        for (Node node : graph.nodes) {
            node.wakeUp();
        }

        while (stop.size() == 0) {
            Thread.sleep(1000);
        }

        System.out.println("Algorithm took: " + timer.stop());

        for (Node node : graph.nodes) {
            System.out.println(node.getPort() + " with branch edges = " + node.getBranchEdges()
                    + " found with " + node.getMessageCount() + " messages");
        }

        System.exit(0);
    }
}
