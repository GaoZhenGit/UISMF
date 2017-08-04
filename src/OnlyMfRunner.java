import it.unimi.dsi.fastutil.ints.IntList;
import org.mymedialite.data.EntityMapping;
import org.mymedialite.data.IPosOnlyFeedback;
import org.mymedialite.data.WeightedItem;
import org.mymedialite.datatype.IBooleanMatrix;
import org.mymedialite.datatype.Matrix;
import org.mymedialite.eval.measures.PrecisionAndRecall;
import org.mymedialite.io.ItemData;
import org.mymedialite.itemrec.MF;
import org.social.mf.MfGenerator;
import org.social.test.ItemPredictorTotal;
import org.social.util.FileCacheUtil;
import org.social.util.Parameter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.*;

import static org.social.test.ItemPredictorTotal.predictNum;

/**
 * Created by Gaozhen on 2017/8/2.
 */
public class OnlyMfRunner {

    public static String Dir = "./data/dir_mf/";
    public static String MfResultPath = Dir + "mfResult.txt";
    public static String MulitiplyResultPath = Dir + "matrix";
    public static String UserMappingPath = Dir + "user_mapping";
    public static String ItemMappingPath = Dir + "item_mapping";
    public static String ResultPath = Dir + "dir_mf.";

    public static String TimePath = "./data/time/dir_" + MfGenerator.className + ".txt";

    public static void main(String[] args) throws Exception {

        File dir = new File(Dir);
        if (!dir.exists()) {
            dir.mkdir();
        }

        mf();
        Matrix<Double> matrix = multiply();
        predict(matrix);
    }

    private static void mf() throws Exception {
        MF mf = MfGenerator.generate().newInstance();

        mf.setTimeRecorder(new PrintWriter(new File(TimePath)));

        mf.numFactors = Parameter.mfFactors;
        mf.numIter = Parameter.mfIter;

        EntityMapping userMapping = new EntityMapping();
        EntityMapping itemMapping = new EntityMapping();

        IPosOnlyFeedback training_data = ItemData.read(Parameter.trainingDataPath, userMapping, itemMapping, false);
        mf.setFeedback(training_data);
        int userNum = mf.maxUserID() + 1;
        int itemNum = mf.maxItemID() + 1;

        saveMapping(userMapping, UserMappingPath);
        saveMapping(itemMapping, ItemMappingPath);
        System.out.println("start training " + userNum + ":" + itemNum);
        mf.train();
        System.out.println("start saving");
        mf.saveModel(MfResultPath);
    }

    private static Matrix<Double> multiply() throws Exception{
        System.out.println("start multiply");
        MF mf = MfGenerator.generate().newInstance();
        mf.loadModel(MfResultPath);
        int userNum = mf.maxUserID() + 1;
        int itemNum = mf.maxItemID() + 1;

        Matrix<Double> mutil = new Matrix<>(userNum, itemNum);
        for (int i = 0; i < userNum; i++) {
            for (int j = 0; j < itemNum; j++) {
                mutil.set(i, j, mf.predict(i, j));
            }
            if (i %1000==0) {
                double percent = ((double)i) / userNum;
                System.out.println(percent);
            }
        }
        System.out.println("start saving");
//        FileCacheUtil.saveDiskCache(mutil, MulitiplyResultPath);
        return mutil;
    }

    private static void predict(Matrix<Double> totalmatrix) throws Exception {
        //全局用户id映射
        EntityMapping totalUserMapping = loadMapping(UserMappingPath);
        //全局物品id映射
        EntityMapping totalItemMapping = loadMapping(ItemMappingPath);

        //读取测试集
        IPosOnlyFeedback testDataWrapper = ItemData.read(Parameter.testDataPath, totalUserMapping, totalItemMapping, false);
        System.out.println("test set readed");

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

        String pathString =  ResultPath + "all." + MfGenerator.methodName() +".txt";
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

    private static EntityMapping loadMapping(String path) {
        BufferedReader reader = null;

        try {
            EntityMapping mapping = new EntityMapping();
            reader = new BufferedReader(new FileReader(path));
            mapping.loadMapping(reader);
            reader.close();
            return mapping;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void saveMapping(EntityMapping mapping, String path) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(path);
            mapping.saveMapping(writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
