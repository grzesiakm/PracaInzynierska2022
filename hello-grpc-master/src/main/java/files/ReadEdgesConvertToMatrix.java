package files;

import java.io.*;
import java.util.Arrays;
import java.util.List;

public class ReadEdgesConvertToMatrix {

    public static int[][] readEdgesFromFile(String filename) throws IOException {
        File file = new File(filename);
        BufferedReader br = new BufferedReader(new FileReader(file));
        int numOfNodes = Integer.parseInt(br.readLine());
        int[][] matrix = new int[numOfNodes][numOfNodes];
        int wmax = 0;
        String line;
        int numOfEdges = 0;

        while ((line = br.readLine()) != null) {
            numOfEdges += 1;
            line = line.substring(1, line.length() - 1);
            List<String> sp = Arrays.asList(line.split(","));

            for (int i = 0; i < numOfEdges; i++) {
                int n1 = Integer.parseInt(sp.get(0).trim());
                int n2 = Integer.parseInt(sp.get(1).trim());
                int w = Integer.parseInt(sp.get(2).trim());
                wmax = Math.max(n1, wmax);
                wmax = Math.max(n2, wmax);
                matrix[n1][n2] = w;
                matrix[n2][n1] = w;
            }
        }

        System.out.println("Adjacency matrix: " + Arrays.deepToString(matrix));
        return matrix;
    }
}
