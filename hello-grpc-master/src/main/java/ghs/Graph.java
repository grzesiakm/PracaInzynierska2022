package ghs;

import ghs.models.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Graph {
    private final int[][] adjacencyMatrix = {{0, 2, 6, 0, 0, 3},
            {2, 0, 0, 0, 9, 0},
            {6, 0, 0, 5, 0, 0},
            {0, 0, 5, 0, 4, 0},
            {0, 9, 0, 4, 0, 1},
            {3, 0, 0, 0, 1, 0}};

    int numberOfNodes = adjacencyMatrix[0].length;

    ArrayList<BlockingQueue<Message>> msgFifo = new ArrayList<>();
    List<Boolean> stop = new ArrayList<>();
    BlockingQueue<Boolean> halt = new LinkedBlockingQueue<>();

    public HashMap<Integer, Integer> getWeightedNeighbors(int vertex) {
        HashMap<Integer, Integer> neighbors = new HashMap<>();
        for (int i = 0; i < numberOfNodes; i++) {
            if (adjacencyMatrix[vertex][i] != 0) {
                neighbors.put(i, adjacencyMatrix[vertex][i]);
            }
        }
        return neighbors;
    }

    public void startGHS() throws InterruptedException {
        stop.add(false);
        Graph testG = new Graph();
        HashMap<Integer, Integer> weightedNeighboursMap = new HashMap<>();
        List<Node> allNodes = new ArrayList<>();
        for (int i = 0; i < numberOfNodes; i++) {
            weightedNeighboursMap = testG.getWeightedNeighbors(i);
            //allNodes.add(new Node(i, weightedNeighboursMap, msgFifo, halt));
        }

        List<Thread> allServers = new ArrayList<>();
        for (int i = 0; i < numberOfNodes; i++) {
            allServers.add(new Thread(allServers.get(i)));
        }

        for (int i = 0; i < numberOfNodes; i++) {
            allServers.get(i).start();
        }

        for (int i = 0; i < numberOfNodes; i++) {
            allServers.get(i).join();
        }

//        for (Node node: allNodes){
//            HashMap<Integer, Integer> mp = node.linkStatusNeighboursMap;
//            HashMap<Integer, Integer> wtmp = node.weightedNeighboursMap;
//            for (Integer i: mp.keySet()){
//                if (mp.get(i) == 2){
//                }
//            }
//        }

    }


}
