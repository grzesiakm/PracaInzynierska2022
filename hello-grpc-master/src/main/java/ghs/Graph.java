package ghs;

import com.google.common.base.Stopwatch;
import ghs.models.Edge;
import ghs.models.Node;
import primkruskal.ReadMatrix;

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
                    .append(node.getEdges().stream().map(e -> e.getToNodeId() + " " + e.getWeight()).collect(Collectors.toList()))
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

    public static void main(String[] args) throws InterruptedException, IOException {
        final int[][] adjacencyMatrix = ReadMatrix.readGraphFromFile("matrix6.txt");

        Graph graph = new Graph(adjacencyMatrix);
        System.out.println(graph);

        for (Node node : graph.nodes) {
            System.out.println("NodeId "+node.getNodeId()+" with neighbours "+node.getEdges());
        }

        Stopwatch timer = Stopwatch.createStarted();

        for (Node node : graph.nodes) {
            node.initialize();
        }

        for (Node node : graph.nodes) {
            node.wakeup();
        }

        while (stop.size() == 0) {
            Thread.sleep(1000);
        }

        System.out.println("Algorithm took: " + timer.stop());

        for (Node node : graph.nodes) {
            System.out.println(node.getPort() + " with branch edges = " + node.getBranchEdges());
        }

        System.exit(0);
    }
}
