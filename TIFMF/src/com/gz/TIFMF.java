package com.gz;

import chosen.nlp.lda.conf.PathConfig;
import chosen.nlp.lda.model.UIS_LDA_Seperated;
import chosen.nlp.lda.util.Documents;
import org.mymedialite.data.EntityMapping;
import org.mymedialite.data.IPosOnlyFeedback;
import org.mymedialite.datatype.IBooleanMatrix;
import org.mymedialite.datatype.Matrix;
import org.mymedialite.itemrec.MF;
import org.social.util.Parameter;
import org.social.util.UserCounter;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Scanner;

/**
 * Created by host on 2018/1/24.
 */
public class TIFMF extends MF {

    public static String pyPath;
    public static int tifmfFitNum;

    static Matrix<Double> theta;
    static Matrix<Double> phi;
    private int userCount;
    private int itemCount;
    private Matrix<Double> fitMatrix;

    private static final String DIR = "./data/LdaResult/";
    private static final String FMAP = DIR + "tifmf-fmap.txt";
    private static final String GMAP = DIR + "tifmf-gmap.txt";
    private static EntityMapping fmap;
    private static EntityMapping gmap;
    private String pMatrix;
    private String qMatrix;
    private String inputMatrix;

    public TIFMF() {
    }

    @Override
    public void setFeedback(IPosOnlyFeedback feedback) {
        super.setFeedback(feedback);
        userCount = feedback.maxUserID();
        itemCount = feedback.maxItemID();
        IBooleanMatrix matrix = feedback.userMatrix();
        try {
            prepareLdaParam();
            fitMatrixWithLda(matrix);
            printFitMatrix();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void prepareLdaParam() throws Exception {
        if (theta == null || phi == null) {
            synchronized (TIFMF.class) {
                if (theta == null || phi == null) {
                    UserCounter.UserItem userItem = UserCounter.getCommunityCount(Parameter.trainingDataPath);
                    int f = userItem.userCount;
                    int t = Parameter.L;
                    int g = userItem.itemCount;
                    UIS_LDA_Seperated lda = UIS_LDA_Seperated.read("./data/model/UIS.2");
                    System.out.println("f:" + f + " t:" + t + " g:" + g);
                    theta = new Matrix<>(f, t);
                    phi = new Matrix<>(g, t);

                    for (int i = 0; i < f; i++) {
                        for (int j = 0; j < t; j++) {
                            double item = lda.theta[i][j];
                            theta.set(i, j, item);
                        }
                    }

                    for (int i = 0; i < t; i++) {
                        for (int j = 0; j < g; j++) {
                            double item = lda.phi[i][j];
                            phi.set(j, i, item);
                        }
                    }

                    Documents documents = new Documents();
                    documents.readDocs(PathConfig.followerPath);
                    documents.readDocs(PathConfig.followeePath);

                    //构造follower的mapping文件
                    File mapf = new File(FMAP);
                    PrintWriter writerf = new PrintWriter(mapf);
                    writerf.write("" + f + "\n");
                    for (int i = 0; i < f; i++) {
                        String originId = String.valueOf(lda.trainSet.docs.get(i).docName);
                        writerf.write(originId + " " + i + "\n");
                    }
                    writerf.flush();
                    writerf.close();

                    //构造followee的mapping文件
                    File mapg = new File(GMAP);
                    PrintWriter writerg = new PrintWriter(mapg);
                    writerg.write("" + g + "\n");
                    for (int i = 0; i < g; i++) {
                        String originId = documents.indexToTermMap.get(i);
                        writerg.write(originId + " " + i + "\n");
                    }
                    writerg.flush();
                    writerg.close();

                    fmap = new EntityMapping();
                    fmap.loadMapping(new BufferedReader(new FileReader(mapf)));

                    gmap = new EntityMapping();
                    gmap.loadMapping(new BufferedReader(new FileReader(mapg)));

                    mapf.delete();
                    mapg.delete();

                    File dir = new File(DIR + "temp");
                    if (dir.listFiles() != null) {
                        for (File it : dir.listFiles()) {
                            it.delete();
                        }
                    }
                }
            }
        }
    }

    private void fitMatrixWithLda(IBooleanMatrix matrix) {
        fitMatrix = new Matrix<Double>(userCount, itemCount);
        for (int i = 0; i < userCount; i++) {
            for (int j = 0; j < itemCount; j++) {
                if (matrix.get(i, j)) {
                    fitMatrix.set(i, j, Double.valueOf(tifmfFitNum));
                } else {
                    fitMatrix.set(i, j, 0d);
                }
            }
        }
    }

    private void printFitMatrix() throws FileNotFoundException {
        long current = (long)(Math.random()) + System.currentTimeMillis();
        inputMatrix = DIR + "temp/" + current + "in.txt";
        pMatrix = DIR + "temp/" + current + "p.txt";
        qMatrix = DIR + "temp/" + current + "q.txt";
        File file = new File(inputMatrix);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        } else {
            file.delete();
        }
        PrintWriter printWriter = new PrintWriter(file);
        printWriter.println(fitMatrix.dim1 + "*" + fitMatrix.dim2);

        for (int i = 0; i < fitMatrix.dim1; i++) {
            for (int j = 0; j < fitMatrix.dim2; j++) {
                double value = fitMatrix.get(i,j);
                if (value > 0) {
                    printWriter.print(j + ":" + fitMatrix.get(i,j) + " ");
                }
            }
            printWriter.println();
        }
        printWriter.flush();
        printWriter.close();
    }

    volatile boolean running = false;
    @Override
    public void iterate() {
        if (!running) {
            execute();
            running = true;
        }
    }

    private void execute() {
        String[] cmd = new String[]{
                "python",
                pyPath + "mf.py",
                inputMatrix,
                pMatrix,
                qMatrix,
                this.numFactors + ""
        };
        CommandUtil.exec(cmd);
    }

    @Override
    public void saveModel(String filename) throws IOException {
        Scanner scannerp = new Scanner(new File(pMatrix));
        Matrix<Double> userFactors = getUserFactors();

        for (int i = 0; i < userCount; i++) {
            String line = scannerp.nextLine();
            String[] lineItems = line.split(" ");
            for (int j = 0; j < numFactors; j++) {
                userFactors.set(i, j, Double.valueOf(lineItems[j]));
            }
        }
        Scanner scannerq = new Scanner(new File(qMatrix));
        Matrix<Double> itemFactors = getItemFactors();
        for (int i = 0; i < numFactors; i++) {
            String line = scannerq.nextLine();
            String[] lineItems = line.split(" ");
            for (int j = 0; j < itemCount; j++) {
                itemFactors.set(j, i, Double.valueOf(lineItems[j]));
            }
        }
        super.saveModel(filename);
    }

    public static void main(String[] args) throws IOException {
        TIFMF tifmf = new TIFMF();
        tifmf.pMatrix = "D:\\JavaProject\\UISMF\\data\\LdaResult\\temp\\1521909851772p.txt";
        tifmf.qMatrix = "D:\\JavaProject\\UISMF\\data\\LdaResult\\temp\\1521909851772q.txt";
        tifmf.userCount = 287;
        tifmf.itemCount = 2607;
        tifmf.numFactors = 16;
        tifmf.userFactors = new Matrix<Double>(tifmf.userCount, tifmf.numFactors);
        tifmf.itemFactors = new Matrix<Double>(tifmf.itemCount, tifmf.numFactors);
        tifmf.saveModel("abc.txt");
    }

    @Override
    public double computeLoss() {
        return 0;
    }
}
