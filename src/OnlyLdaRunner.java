import chosen.nlp.lda.conf.PathConfig;
import chosen.nlp.lda.model.UIS_LDA_Seperated;
import chosen.nlp.lda.util.Documents;
import org.mymedialite.data.EntityMapping;
import org.mymedialite.datatype.Matrix;
import org.mymedialite.datatype.MatrixExtensions;
import org.social.util.Parameter;
import org.social.util.UserCounter;

import java.io.*;

/**
 * Created by Gaozhen on 2017/8/4.
 */
public class OnlyLdaRunner {

    private static final String Dir = "./data/dir_lda";
    private static final String MappingF = Dir + "/mapf.txt";
    private static final String MappingG = Dir + "/mapg.txt";
    private static String Result = Dir + "/dir_lda_";

    public static void main(String[] args) throws Exception {

        Result += Parameter.L + "_" + Parameter.iL + ".txt";

        UserCounter.UserItem userItem = UserCounter.getCommunityCount(Parameter.trainingDataPath);
        int f = userItem.userCount;
        int t = Parameter.L;
        int g = userItem.itemCount;
        UIS_LDA_Seperated lda = UIS_LDA_Seperated.read("./data/model/UIS.2");

//        int f = lda.trainSet.docs.size();
//        int t = lda.K;
//        int g = lda.V;
        System.out.println("f:" + f + " t:" + t + " g:" + g);

        Matrix<Double> theta = new Matrix<>(f, t);
        Matrix<Double> phi = new Matrix<>(g, t);

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
//        lda = null;
        System.gc();
        System.out.println("transfer finish");

        BigMatrix<Double> totalmatrix = new BigMatrix<>(f, g);
        for (int i = 0; i < f; i++) {
            for (int j = 0; j < g; j++) {
                double item = MatrixExtensions.rowScalarProduct(theta, i, phi, j);
                totalmatrix.set(i, j, item);
            }
            if (i % 100 ==0) {
                System.out.println("multi:" + ((double) i / f));
            }
        }
        System.out.println("multiply finish");

        theta = null;
        phi = null;
        System.gc();

        Documents documents = new Documents();
        documents.readDocs(PathConfig.followerPath);
        documents.readDocs(PathConfig.followeePath);

        File dirFile = new File(Dir);
        if (!dirFile.exists()) {
            dirFile.mkdir();
        }

        //构造follower的mapping文件
        File mapf = new File(MappingF);
        PrintWriter writerf = new PrintWriter(mapf);
        writerf.write("" + f + "\n");
        for (int i = 0; i < f; i++) {
            String originId = String.valueOf(lda.trainSet.docs.get(i).docName);
            writerf.write(originId + " " + i + "\n");
        }
        writerf.flush();
        writerf.close();

        //构造followee的mapping文件
        File mapg = new File(MappingG);
        PrintWriter writerg = new PrintWriter(mapg);
        writerg.write("" + g + "\n");
        for (int i = 0; i < g; i++) {
            String originId = documents.indexToTermMap.get(i);
            writerg.write(originId + " " + i + "\n");
        }
        writerg.flush();
        writerg.close();



        EntityMapping fmap = new EntityMapping();
        fmap.loadMapping(new BufferedReader(new FileReader(mapf)));

        EntityMapping gmap = new EntityMapping();
        gmap.loadMapping(new BufferedReader(new FileReader(mapg)));

        CommonPredictor.predict(
                totalmatrix,
                fmap,
                gmap,
                Parameter.testDataPath,
                Result);
    }
}
