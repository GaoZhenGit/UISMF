package main;

import algorithms.MF_fastALS;
import data_structure.DenseMatrix;
import data_structure.Rating;
import data_structure.SparseMatrix;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by host on 2017/4/10.
 */
public class Runner {

    double w0 = 10;
    boolean showProgress = false;
    boolean showLoss = true;
    int factors = 8;
    int maxIter = 10;
    double reg = 0.01;
    double alpha = 0.75;
    int matrixCount = 15;
    int maxThread;
    String inputPrefix = "z_";
    String inputDir;
    String outputDir;

    int topK = 100;
    int threadNum = 10;

    double init_mean = 0;
    double init_stdev = 0.01;

    public static void main(String[] args) throws FileNotFoundException, ParseException {
        final Runner runner = new Runner();
//        runner.readConfig("./data/config.json");
        runner.readConfig(args[0]);
        System.out.println(args[0]);
        ExecutorService service = Executors.newFixedThreadPool(runner.maxThread);
        for (int i = 0; i < runner.matrixCount; i++) {
            final int finalI = i;
            service.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        runner.doMf(finalI);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        service.shutdown();
    }

    private void readConfig(String configPath) throws FileNotFoundException, ParseException {
        File configFile = new File(configPath);
        StringBuilder stringBuilder = new StringBuilder();
        Scanner scanner = new Scanner(configFile);
        while (scanner.hasNextLine()) {
            stringBuilder.append(scanner.nextLine().trim());
        }
        JSONParser jsonParser = new JSONParser();
        JSONObject configJson = (JSONObject) jsonParser.parse(stringBuilder.toString());

        factors = (int) (long) configJson.get("factors");
        maxIter = (int) (long) configJson.getOrDefault("maxIter", 10);
        alpha = (long) (double) configJson.getOrDefault("alpha", 0.75);
        matrixCount = (int) (long) configJson.getOrDefault("matrixCount", 15);
        inputPrefix = (String) configJson.getOrDefault("inputPrefix", "z_0");
        inputDir = (String) configJson.getOrDefault("input", "./");
        outputDir = (String) configJson.getOrDefault("output", "./");
        maxThread = (int) (long)configJson.getOrDefault("maxThread",matrixCount);
    }

    private void doMf(int index) throws FileNotFoundException {
        System.out.println(index + " start");
        String matrixPath = inputDir + inputPrefix + index;
        SparseMatrix matrix = MatrixReader.getMatrix(matrixPath);
        MF_fastALS fals = new MF_fastALS(matrix, new ArrayList<Rating>(), topK, threadNum,
                factors, maxIter, w0, alpha, reg, init_mean, init_stdev, showProgress, showLoss);
        fals.buildModel();
        printMatrix(fals.U, "p_" + index);
        printMatrix(fals.V.transpose(), "q_" + index);
        System.out.println(index + " end");
    }

    private void printMatrix(DenseMatrix matrix, String name) throws FileNotFoundException {
        int row = matrix.numRows();
        int col = matrix.numColumns();
        String output = outputDir + name;
        File outFile = new File(output);
        PrintWriter writer = new PrintWriter(outFile);
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                writer.print(matrix.get(i, j) + " ");
            }
            writer.print('\n');
        }
        writer.flush();
        writer.close();
    }
}

