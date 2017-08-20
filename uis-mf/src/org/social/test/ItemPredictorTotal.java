package org.social.test;

import it.unimi.dsi.fastutil.ints.IntList;
import org.mymedialite.data.EntityMapping;
import org.mymedialite.data.IPosOnlyFeedback;
import org.mymedialite.data.WeightedItem;
import org.mymedialite.datatype.IBooleanMatrix;
import org.mymedialite.datatype.IMatrix;
import org.mymedialite.datatype.Matrix;
import org.mymedialite.eval.measures.NDCG;
import org.mymedialite.eval.measures.PrecisionAndRecall;
import org.mymedialite.eval.measures.ReciprocalRank;
import org.mymedialite.io.ItemData;
import org.mymedialite.itemrec.MF;
import org.mymedialite.util.Utils;
import org.social.mf.MfGenerator;
import org.social.util.FileCacheUtil;
import org.social.util.Parameter;
import org.social.util.UserCounter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.*;

/**
 * 将矩阵合并的步奏，修改以前的计算方式不对
 */
public class ItemPredictorTotal {

    public static int mfThreadCount = 1;

    public static int[] predictNum = new int[]{5, 10, 15, 20, 25, 30, 35, 40, 50};

    private static String totalUserMappingPath = Parameter.matrixPath + ".f_map";

    private static String totalItemMappingPath = Parameter.matrixPath + ".g_map";

    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            multiply();
            score();
            predict(Parameter.maxF1Path + "all." + MfGenerator.methodName() +"." + Parameter.L + ".I" + Parameter.iL);
        } else {
            switch (args[0]) {
                case "1":
                    multiply();
                    break;
                case "2":
                    score();
                    break;
                case "3":
                    predict(args[1]);
                    break;
                default:
                    break;
            }
        }
    }

    private static void multiply() {
        ExecutorService exec = Executors.newFixedThreadPool(mfThreadCount);
        List<Future<IMatrix<Double>>> taskList = new ArrayList<>();
        for (int i = 0; i < Parameter.L; i++) {
            MatrixMultiplier matrixMultiplier = new MatrixMultiplier(Parameter.IFMFPath + "all." + i, Parameter.matrixPath + i);
            taskList.add(exec.submit(matrixMultiplier));
        }
        for (int i = 0; i < Parameter.L; i++) {
            try {
                taskList.get(i).get();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                System.out.println(i + " result get");
            }

        }
        exec.shutdown();
        System.out.println("multiply all finish");
    }

    private static void score() {
        UserCounter.UserItem item = UserCounter.getCommunityCount();
        int totalUser = item.userCount;
        int totalItem = item.itemCount;

        //全局用户id映射
        EntityMapping totalUserMapping = new EntityMapping();
        //全局物品id映射
        EntityMapping totalItemMapping = new EntityMapping();

        Matrix<Double> totalMatrix = new Matrix<>(totalUser, totalItem);

        for (int i = 0; i < Parameter.L; i++) {
            //从硬盘读取当前社区的相乘后的矩阵
            Matrix<Double> communityMatrix = (Matrix<Double>) FileCacheUtil.loadDiskCache(Parameter.matrixPath + i);
            System.out.println(communityMatrix.dim1 + ":" + communityMatrix.dim2);
            //读取当前社区的id映射
            String userMapPath = Parameter.communityDir + "all/" + Parameter.cfmap + i;
            String itemMapPath = Parameter.communityDir + "all/" + Parameter.cgmap + i;
            EntityMapping communityUserMapping = loadMapping(userMapPath);
            EntityMapping communityItemMapping = loadMapping(itemMapPath);

            for (int j = 0; j < communityMatrix.dim1; j++) {
                for (int k = 0; k < communityMatrix.dim2; k++) {
                    //将社区矩阵单元索引转化为总矩阵下标索引
                    int totalUserIndex = totalUserMapping.toInternalID(communityUserMapping.toOriginalID(j));
                    int totalItemIndex = totalItemMapping.toInternalID(communityItemMapping.toOriginalID(k));
                    double communityValue = communityMatrix.get(j, k);

                    max(totalMatrix, totalUserIndex, totalItemIndex, communityValue);
                }
            }
            System.out.println("matix " + i + " added");
        }
        FileCacheUtil.saveDiskCache(totalMatrix, Parameter.matrixPath + "Total");
        saveMapping(totalUserMapping, totalUserMappingPath);
        saveMapping(totalItemMapping, totalItemMappingPath);
        System.out.println("total matrix finish");
    }

    private static void predict(String outputPath) {
        //全局用户id映射
        EntityMapping totalUserMapping = loadMapping(totalUserMappingPath);
        //全局物品id映射
        EntityMapping totalItemMapping = loadMapping(totalItemMappingPath);
        try {
            //读取测试集
            IPosOnlyFeedback testDataWrapper = ItemData.read(Parameter.testDataPath, totalUserMapping, totalItemMapping, false);
            System.out.println("test set readed");
            //读取总矩阵
            Matrix<Double> totalmatrix = (Matrix<Double>) FileCacheUtil.loadDiskCache(Parameter.matrixPath + "Total");
            System.out.println("total matrix readed");

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
                Map<Integer, Double> ndcg = ndcg(predictList, correct_items, predictNum);
                ndcgList.add(ndcg);
                Map<Integer, Boolean> con = conversionHit(predictList, correct_items, predictNum);
                conList.add(con);
                Map<Integer, Double> mrr = mrr(predictList, correct_items, predictNum);
                mrrList.add(mrr);
            }

            Map<Integer, Double> prec = totalRate(precList);
            Map<Integer, Double> recall = totalRate(recallList);
            Map<Integer, Double> ndcg = totalRate(ndcgList);
            Map<Integer, Double> con = totalConversion(conList);
            Map<Integer, Double> mrr = totalRate(mrrList);

            String pathString =  outputPath;
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
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void max(Matrix<Double> totalMatrix, int ti, int tj, double communityValue) {
        Double originValue = totalMatrix.get(ti, tj);
        if (originValue == null) {
            originValue = 0.0;
        }
        if (communityValue > originValue) {
            totalMatrix.set(ti, tj, communityValue);
        }
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

    public static Map<Integer, Double> ndcg(Collection<Integer> predictList, Collection<Integer> correctItem, int[] nums) {
        Map<Integer, Double> result = new HashMap<>();
        for (int n : nums) {
            List<Integer> topPredict = new ArrayList<>(predictList).subList(0, n);
            double ndcg = NDCG.compute(topPredict, correctItem, new HashSet<Integer>());
            result.put(n, ndcg);
        }
        return result;
    }

    public static Map<Integer, Double> mrr(Collection<Integer> predictList, Collection<Integer> correctItem, int[] nums) {
        Map<Integer, Double> result = new HashMap<>();
        for (int n : nums) {
            List<Integer> topPredict = new ArrayList<>(predictList).subList(0, n);
            double ndcg = ReciprocalRank.compute(topPredict, correctItem, new HashSet<Integer>());
            result.put(n, ndcg);
        }
        return result;
    }

    public static Map<Integer, Boolean> conversionHit(Collection<Integer> predictList, Collection<Integer> correctItem, int[] nums) {
        Map<Integer, Boolean> result = new HashMap<>();
        for (int n : nums) {
            List<Integer> topPredict = new ArrayList<>(predictList).subList(0, n);
            int hitCount = Utils.intersect(topPredict, correctItem).size();
            result.put(n, hitCount > 0);
        }
        return result;
    }

    public static Map<Integer, Double> totalRate(List<Map<Integer, Double>> list) {
        int size = list.size();
        Map<Integer, Double> result = new HashMap<>();
        for (int i = 0; i < size; i++) {
            Map<Integer, Double> item = list.get(i);
            for (int n : predictNum) {
                Double r = result.get(n);
                if (r == null) {
                    r = 0.0;
                }
                Double it = item.get(n);
                result.put(n, r + it);
            }
        }
        for (int n : predictNum) {
            Double r = result.get(n);
            result.put(n, r / size);
        }
        return result;
    }

    public static Map<Integer, Double> totalConversion(List<Map<Integer, Boolean>> list) {
        int size = list.size();
        Map<Integer, Double> result = new HashMap<>();
        for (int n : predictNum) {
            int hitCount = 0;
            for (int i = 0; i < size; i++) {
                if (list.get(i).get(n)) {
                    hitCount++;
                }
            }
            result.put(n, (double) hitCount / size);
        }
        return result;
    }


    private static class MatrixMultiplier implements Callable<IMatrix<Double>> {
        String modelPath;
        String savePath;

        public MatrixMultiplier(String modelPath, String savePath) {
            this.modelPath = modelPath;
            this.savePath = savePath;
        }

        @Override
        public IMatrix<Double> call() throws Exception {

            MF mf = MfGenerator.generate().newInstance();
            mf.loadModel(modelPath);
            int userNum = mf.maxUserID() + 1;
            int itemNum = mf.maxItemID() + 1;

            Matrix<Double> mutil = new Matrix<>(userNum, itemNum);
            for (int i = 0; i < userNum; i++) {
                for (int j = 0; j < itemNum; j++) {
                    mutil.set(i, j, mf.predict(i, j));
                }
            }
            FileCacheUtil.saveDiskCache(mutil, savePath);
            return mutil;
        }
    }

    private static class MatrixAdder implements Callable<String> {

        public MatrixAdder(int index, EntityMapping totalUserMapping, EntityMapping totalItemMapping) {

        }

        @Override
        public String call() throws Exception {
            return null;
        }
    }
}
