import chosen.nlp.lda.conf.LDAParameter;
import chosen.nlp.lda.conf.PathConfig;
import chosen.nlp.lda.model.UIS_LDA_Seperated;
import chosen.nlp.lda.util.Documents;
import chosen.social.lda.util.CommunityData;
import it.unimi.dsi.fastutil.ints.IntList;
import org.mymedialite.data.EntityMapping;
import org.mymedialite.data.IPosOnlyFeedback;
import org.mymedialite.data.WeightedItem;
import org.mymedialite.datatype.IBooleanMatrix;
import org.mymedialite.datatype.Matrix;
import org.mymedialite.datatype.MatrixExtensions;
import org.mymedialite.eval.measures.PrecisionAndRecall;
import org.mymedialite.io.ItemData;
import org.social.mf.MfGenerator;
import org.social.test.ItemPredictorTotal;
import org.social.util.Parameter;
import org.social.util.UserCounter;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

import static org.social.test.ItemPredictorTotal.predictNum;

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

        Matrix<Double> totalmatrix = new Matrix<>(f, g);
        for (int i = 0; i < f; i++) {
            for (int j = 0; j < g; j++) {
                double item = MatrixExtensions.rowScalarProduct(theta, i, phi, j);
                totalmatrix.set(i, j, item);
            }
            System.out.println(((double) i / f));
        }

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

        File mapf = new File(MappingF);
        PrintWriter writerf = new PrintWriter(mapf);
        int mapfSize = lda.trainSet.docs.size();
        writerf.write("" + mapfSize + "\n");
        for (int i = 0; i < mapfSize; i++) {
            String originId = String.valueOf(lda.trainSet.docs.get(i).docName);
            writerf.write(originId + " " + i + "\n");
        }
        writerf.flush();
        writerf.close();

        File mapg = new File(MappingG);
        PrintWriter writerg = new PrintWriter(mapg);
        int mapgSize = documents.indexToTermMap.size();
        writerg.write("" + mapgSize + "\n");
        for (int i = 0; i < mapgSize; i++) {
            String originId = documents.indexToTermMap.get(i);
            writerg.write(originId + " " + i + "\n");
        }
        writerg.flush();
        writerg.close();



        EntityMapping fmap = new EntityMapping();
        fmap.loadMapping(new BufferedReader(new FileReader(mapf)));

        EntityMapping gmap = new EntityMapping();
        gmap.loadMapping(new BufferedReader(new FileReader(mapg)));

        IPosOnlyFeedback testDataWrapper = ItemData.read(Parameter.testDataPath, fmap, gmap, false);

        IntList testUsers = testDataWrapper.allUsers();
        //获得测试集矩阵，用于获得当前用户的正确推荐items
        IBooleanMatrix itemGetter = testDataWrapper.userMatrix();

        List<Map<Integer, Double>> precList = new ArrayList<>();
        List<Map<Integer, Double>> recallList = new ArrayList<>();
        List<Map<Integer, Double>> ndcgList = new ArrayList<>();
        List<Map<Integer, Boolean>> conList = new ArrayList<>();
        List<Map<Integer, Double>> mrrList = new ArrayList<>();

        int testUserSize = testUsers.size();
        for (int ti = 0; ti < testUserSize; ti++) {
            Integer testUserId = testUsers.get(ti);
            if (ti % 100 == 0) {
                System.out.println((double) ti / testUserSize);
            }
            if (testUserId >= totalmatrix.dim1) {
                continue;
            }
            //取出矩阵的一行，然后根据score大到小排序，然后将下标排成List，形成推荐列表
            List<Double> scoreList = totalmatrix.getRow(testUserId);
            List<WeightedItem> tmpList = new ArrayList<>(scoreList.size());
            for (int i = 0; i < scoreList.size(); i++) {
                Double score = scoreList.get(i);
                WeightedItem item = new WeightedItem();
                item.item_id = i;
                item.weight = score == null ? 0 : score;
                tmpList.add(item);
            }
            Collections.sort(tmpList, Collections.reverseOrder());
            //该list为当前测试用户的推荐列表
            List<Integer> predictList = new ArrayList<>(tmpList.size());
            for (WeightedItem item : tmpList) {
                predictList.add(item.item_id);
            }
            tmpList = null;

            Collection<Integer> correct_items = itemGetter.get(testUserId);


            Map<Integer, Double> prec = PrecisionAndRecall.precisionAt(predictList, correct_items, new HashSet<Integer>(), predictNum);
            precList.add(prec);
            Map<Integer, Double> recall = PrecisionAndRecall.recallAt(predictList, correct_items, new HashSet<Integer>(), predictNum);
            recallList.add(recall);
            Map<Integer, Double> ndcg = ItemPredictorTotal.ndcg(predictList, correct_items, predictNum);
            ndcgList.add(ndcg);
            Map<Integer, Boolean> con = ItemPredictorTotal.conversionHit(predictList, correct_items, predictNum);
            conList.add(con);
            Map<Integer, Double> mrr = ItemPredictorTotal.mrr(predictList, correct_items, predictNum);
            mrrList.add(mrr);
        }

        Map<Integer, Double> prec = ItemPredictorTotal.totalRate(precList);
        Map<Integer, Double> recall = ItemPredictorTotal.totalRate(recallList);
        Map<Integer, Double> ndcg = ItemPredictorTotal.totalRate(ndcgList);
        Map<Integer, Double> con = ItemPredictorTotal.totalConversion(conList);
        Map<Integer, Double> mrr = ItemPredictorTotal.totalRate(mrrList);

        String pathString =  Result;
        System.out.println("output path: " + pathString);
        PrintWriter resultWriter = new PrintWriter(new File(pathString));
        DecimalFormat df = new DecimalFormat("0.000000000000000000");
        DecimalFormat df1 = new DecimalFormat("00");
        for (int n : predictNum) {
            double precV = prec.get(n);
            double recallV = recall.get(n);
            double f1 = (2 * precV * recallV) / (precV + recallV);
            resultWriter.write(df1.format(n) + ":");
            resultWriter.write(" prec=" + df.format(prec.get(n)));
            resultWriter.write(" recall=" + df.format(recall.get(n)));
            resultWriter.write(" f1=" + df.format(f1));
            resultWriter.write(" ndcg=" + df.format(ndcg.get(n)));
            resultWriter.write(" con=" + df.format(con.get(n)));
            resultWriter.write(" mrr=" + df.format(mrr.get(n)));
            resultWriter.write("\n");
        }
        resultWriter.flush();
        resultWriter.close();
    }
}
