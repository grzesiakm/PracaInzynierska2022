package primkruskal;


public class Kruskal {
    static int V = 6;
    static int[] parent = new int[V];

    //Find union of node i
    static int find(int i) {
        while (parent[i] != i)
            i = parent[i];
        return i;
    }

    //Union of i and j, returns false if i and j are already in the same union in order to not create a cycle
    static void union(int i, int j) {
        int a = find(i);
        int b = find(j);
        parent[a] = b;
    }

    //Kruskal's algorithm
    //    1. Sort all the edges by ascending edge weight.
    //    2. Pick the smallest edge. Check if it forms a cycle with the spanning tree formed so far.
    //       If cycle is not formed, include this edge. Else, discard it.
    //    3. Repeat step 2. until there are (V-1) edges in the spanning tree.
    static void kruskalMST(int[][] weight) {
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

    public static void main(String[] args) {
        int[][] graph = {{0, 2, 6, 0, 0, 3},
                {2, 0, 0, 0, 9, 0},
                {6, 0, 0, 5, 0, 0},
                {0, 0, 5, 0, 4, 0},
                {0, 9, 0, 4, 0, 1},
                {3, 0, 0, 0, 1, 0}};

        kruskalMST(graph);
    }
}
