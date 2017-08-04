package main;

import data_structure.SparseMatrix;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * Created by host on 2017/4/9.
 */
public class MatrixReader {
    public static SparseMatrix getMatrix(String path) throws FileNotFoundException {
        File file = new File(path);
        Scanner scanner = new Scanner(file);
        String firstLine = scanner.nextLine();
        String[] rowAndCol = firstLine.split("\\*");
        int row = Integer.parseInt(rowAndCol[0]);
        int col = Integer.parseInt(rowAndCol[1]);
        SparseMatrix matrix = new SparseMatrix(row,col);
        int lineCount = 0;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] items = line.split(" ");
            for (String it : items) {
                int index = Integer.parseInt(it.split(":")[0]);
                double value = Double.parseDouble(it.split(":")[1]);
                matrix.setValue(lineCount,index,value);
            }
            lineCount++;
        }
        return matrix;
    }

    public static void main(String[] args) throws FileNotFoundException {
        SparseMatrix matrix = getMatrix("./data/z_0");
        System.out.print(matrix.getValue(0,0));
    }
}
