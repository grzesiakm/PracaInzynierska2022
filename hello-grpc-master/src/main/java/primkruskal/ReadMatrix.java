package primkruskal;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

public class ReadMatrix {

    public static int[][] readGraphFromFile(String filename) throws IOException {
        BufferedReader buffer = new BufferedReader(new FileReader(filename));
        int[][] matrix = null;
        String line;
        int row = 0, size = 0;

        while ((line = buffer.readLine()) != null) {
            String[] values = line.trim().split(" ");

            if (matrix == null) {
                size = values.length;
                matrix = new int[size][size];
            }

            for (int col = 0; col < size; col++) {
                matrix[row][col] = Integer.parseInt(values[col]);
            }

            row++;
        }

        System.out.println("Adjacency matrix: " + Arrays.deepToString(matrix));
        return matrix;
    }
}
