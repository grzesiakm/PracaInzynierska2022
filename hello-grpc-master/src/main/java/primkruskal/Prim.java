package primkruskal;


import com.google.common.base.Stopwatch;
import java.io.IOException;

class Prim {
    public static int V = 6;

    //Find index of min-weight node from set of unvisited nodes
    static int findMinNode(boolean[] visited, int[] weights) {
        int index = -1; //index of min-weight node
        int minWeight = Integer.MAX_VALUE;

        for (int i = 0; i < V; i++) {
            if (!visited[i] && weights[i] < minWeight) {
                minWeight = weights[i];
                index = i;
            }
        }
        return index;
    }

    static void printMinimumSpanningTree(int[][] graph, int[] parent) {
        int MST = 0; //total weight of mst

        for (int i = 1; i < V; i++) {
            MST += graph[i][parent[i]];
        }

        System.out.println("Edges \tWeight");
        for (int i = 1; i < V; i++)
            System.out.println(parent[i] + " - " + i + " \t" + graph[i][parent[i]]);

        System.out.println("\nWeight of the minimum Spanning-tree " + MST);
    }

    /*
      Prim's algorithm
      1) Create a set mstSet that keeps track of vertices already included in MST.
      2) Assign a key value to all vertices in the input graph. Initialize all key values as INFINITE.
         Assign key value as 0 for the first vertex so that it is picked first.
      3) While mstSet does not include all vertices
        a) Pick a vertex u which is not there in mstSet and has minimum key value.
        b) Include u to mstSet.
        c) Update key value of all adjacent vertices of u.
           For every adjacent vertex v, if weight of edge u-v is less than the previous key value of v,
           update the key value as weight of u-v.
    */
    static void primMST(int[][] graph) {
        boolean[] isVisited = new boolean[V]; //Table with information if the node is isVisited or not
        int[] weights = new int[V]; //Table of minimum weight of graph to connect an edge with the current node
        int[] parent = new int[V]; //Table with the parent node of the current

        //Initialization of isVisited and weights tables
        for (int i = 0; i < V; i++) {
            isVisited[i] = false;
            weights[i] = Integer.MAX_VALUE;
        }

        //Include 1st node in MST
        weights[0] = Integer.MIN_VALUE;
        parent[0] = -1;

        //Search for other (V-1) nodes
        for (int i = 0; i < V - 1; i++) {

            int minVertexIndex = findMinNode(isVisited, weights); //index of min-weight node from a set of unvisited
            isVisited[minVertexIndex] = true; //mark as visited

            // Update adjacent vertices of the current isVisited node
            for (int j = 0; j < V; j++) {
                // If there is an edge between j and current isVisited vertex and also j is unvisited vertex
                if (graph[j][minVertexIndex] != 0 && !isVisited[j]) {
                    //If graph[v][x] is greater than weight[v]
                    if (graph[j][minVertexIndex] < weights[j]) {
                        weights[j] = graph[j][minVertexIndex];
                        parent[j] = minVertexIndex;
                    }
                }
            }
        }

        printMinimumSpanningTree(graph, parent);
    }

    public static void main(String[] args) throws IOException {
        int[][] graph = ReadMatrix.readGraphFromFile("matrix6.txt");
//                {{0, 2, 6, 0, 0, 3},
//                {2, 0, 0, 0, 9, 0},
//                {6, 0, 0, 5, 0, 0},
//                {0, 0, 5, 0, 4, 0},
//                {0, 9, 0, 4, 0, 1},
//                {3, 0, 0, 0, 1, 0}};

        Stopwatch timer = Stopwatch.createStarted();
        primMST(graph);
        System.out.println("Algorithm took: " + timer.stop());
    }
}
