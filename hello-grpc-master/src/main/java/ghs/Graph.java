package ghs;

import ghs.models.Edge;
import ghs.models.Node;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;


public class Graph {
    private final List<Node> nodes;
    private final int[][] adjacencyMatrix;

    public Graph(int[][] adjacencyMatrix) {
        this.adjacencyMatrix = adjacencyMatrix;
        nodes = createGraph(adjacencyMatrix);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Node node : nodes) {
            builder.append(node.getPort())
                    .append(" with neighbours: ")
                    .append(node.getEdges().stream().map(e -> e.getToFragmentId() + " " + e.getWeight()).collect(Collectors.toList()))
                    .append("\n");
        }
        return builder.toString();
    }

    private int getNodeCount() {
        return this.adjacencyMatrix.length;
    }

    public static void main(String[] args) {
        final int[][] adjacencyMatrix = {
                {2, 0, 0, 0, 9, 0},
                {6, 0, 0, 5, 0, 0},
                {0, 2, 6, 0, 0, 3},
                {0, 0, 5, 0, 4, 0},
                {0, 9, 0, 4, 0, 1},
                {3, 0, 0, 0, 1, 0}

        };

        Graph graph = new Graph(adjacencyMatrix);
        System.out.println(graph);
    }


    private List<Node> createGraph(int[][] adjacencyMatrix) {
        List<Node> nodes = new ArrayList<>();

        for (int i = 0; i < getNodeCount(); i++) {
            HashMap<Integer, Integer> map = getWeightedNeighbors(i);
            List<Edge> neighbours = map.entrySet().stream()
                    .map(entry -> new Edge(entry.getValue(), 50050 + entry.getKey()))
                    .collect(Collectors.toList());
            nodes.add(new Node(i + 50050, neighbours));
        }
        return nodes;
    }

    private HashMap<Integer, Integer> getWeightedNeighbors(int vertex) {
        HashMap<Integer, Integer> neighbors = new HashMap<>();
        for (int i = 0; i < getNodeCount(); i++) {
            if (adjacencyMatrix[vertex][i] != 0) {
                neighbors.put(i, adjacencyMatrix[vertex][i]);
            }
        }
        return neighbors;
    }
}