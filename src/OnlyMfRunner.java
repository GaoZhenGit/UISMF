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

        String pathString =  ResultPath + "all." + MfGenerator.methodName() +".txt";

        CommonPredictor.predict(
                totalmatrix,
                totalUserMapping,
                totalItemMapping,
                Parameter.testDataPath,
                pathString);
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
