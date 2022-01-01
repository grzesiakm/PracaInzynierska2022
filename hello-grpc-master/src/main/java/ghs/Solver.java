package ghs;

import ghs.models.Graph;

import java.io.IOException;

public class Solver {
    public static void main(String[] args) throws IOException, InterruptedException {
        /*
         * First argument is filename with input list of edges or adjacency matrix
         * Second one can be 'edges' or 'matrix' (as default is set matrix with 6 nodes)
         */
        Graph.calculateMST("test8.txt", "edges");
    }
}
