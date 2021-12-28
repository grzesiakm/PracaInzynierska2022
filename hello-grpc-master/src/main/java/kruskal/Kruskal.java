package kruskal;


import com.google.common.base.Stopwatch;
import files.ReadEdgesConvertToMatrix;

import java.io.IOException;

public class Kruskal {
    private final int V;
    private final int[] parent;

    public Kruskal(int v) {
        V = v;
        this.parent = new int[V];
    }

    //Find union of node i
    public int find(int i) {
        while (parent[i] != i)
            i = parent[i];
        return i;
    }

    //Union of i and j, returns false if i and j are already in the same union in order to not create a cycle
    public void union(int i, int j) {
        int a = find(i);
        int b = find(j);
        parent[a] = b;
    }

    //Kruskal's algorithm
    //    1. Sort all the edges by ascending edge weight.
    //    2. Pick the smallest edge. Check if it forms a cycle with the spanning tree formed so far.
    //       If cycle is not formed, include this edge. Else, discard it.
    //    3. Repeat step 2. until there are (V-1) edges in the spanning tree.
    public void calculateMST(int[][] weight) {
        int minWeight = 0;

        // Initialize sets of disjoint unions
        for (int i = 0; i < V; i++)
            parent[i] = i;

        System.out.println("Edges \tWeight");

        //Include minimum weight edges one by one
        int edge_count = 0;
        while (edge_count < V - 1) {
            int min = Integer.MAX_VALUE, a = -1, b = -1;
            for (int i = 0; i < V; i++) {
                for (int j = 0; j < V; j++) {
                    if (find(i) != find(j) && weight[i][j] < min && weight[i][j] != 0) {
                        min = weight[i][j];
                        a = i;
                        b = j;
                    }
                }
            }
            union(a, b);
            edge_count++;

            System.out.println(a + " - " + b + "\t" + min);
            minWeight += min;
        }

        System.out.println("\nWeight of the minimum Spanning-tree " + minWeight);
    }

    public static void main(String[] args) throws IOException {
//        Kruskal kruskal = new Kruskal(6);
//        int[][] graph = ReadAdjacencyMatrix.readMatrixFromFile("matrix6.txt");

//        Kruskal kruskal = new Kruskal(8);
//        int[][] graph = ReadEdgesConvertToMatrix.readEdgesFromFile("edgesMatrix8.txt");

//        Kruskal kruskal = new Kruskal(10);
//        int[][] graph = ReadEdgesConvertToMatrix.readEdgesFromFile("edgesMatrix10.txt");

        Kruskal kruskal = new Kruskal(12);
        int[][] graph = ReadEdgesConvertToMatrix.readEdgesFromFile("edgesMatrix12.txt");


        Stopwatch timer = Stopwatch.createStarted();
        kruskal.calculateMST(graph);
        System.out.println("Algorithm took: " + timer.stop());
    }
}
