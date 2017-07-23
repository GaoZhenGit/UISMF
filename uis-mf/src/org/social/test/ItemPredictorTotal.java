package org.social.test;

import it.unimi.dsi.fastutil.ints.IntList;
import org.mymedialite.data.EntityMapping;
import org.mymedialite.data.IPosOnlyFeedback;
import org.mymedialite.data.WeightedItem;
import org.mymedialite.datatype.IBooleanMatrix;
import org.mymedialite.datatype.IMatrix;
import org.mymedialite.datatype.Matrix;
import org.mymedialite.io.ItemData;
import org.mymedialite.itemrec.WRMF;
import org.social.util.FileCacheUtil;
import org.social.util.Parameter;
import org.social.util.UserCounter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/**
 * 将矩阵合并的步奏，修改以前的计算方式不对
 */
public class ItemPredictorTotal {

    public static int[] predictNum = new int[]{5, 10, 15, 20, 25, 30, 35, 40, 50};

    private static String totalUserMappingPath = Parameter.matrixPath + ".f_map";

    private static String totalItemMappingPath = Parameter.matrixPath + ".g_map";

    public static void main(String[] args) {
//        multiply();
//        score();
        predict();
    }

    private static void multiply() {
        ExecutorService exec = Executors.newFixedThreadPool(1);
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
                System.out.println(i + "result get");
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
            saveMapping(totalUserMapping, totalUserMappingPath);
            saveMapping(totalItemMapping, totalItemMappingPath);
            FileCacheUtil.saveDiskCache(totalMatrix, Parameter.matrixPath + "Total");
        }

    }

    private static void predict() {
        //全局用户id映射
        EntityMapping totalUserMapping = loadMapping(totalUserMappingPath);
        //全局物品id映射
        EntityMapping totalItemMapping = loadMapping(totalItemMappingPath);
        try {
            //读取测试集
            IPosOnlyFeedback testDataWrapper = ItemData.read(Parameter.testDataPath, totalUserMapping, totalItemMapping, false);
            //读取总矩阵
            Matrix<Double> totalmatrix = (Matrix<Double>) FileCacheUtil.loadDiskCache(Parameter.matrixPath + "Total");

            IntList testUsers = testDataWrapper.allUsers();
            IBooleanMatrix itemGetter = testDataWrapper.userMatrix();

            for (Integer testUserId : testUsers) {
                //取出矩阵的一行，然后根据score大到小排序，然后将下标排成List，形成推荐列表
                List<Double> scoreList = totalmatrix.getRow(testUserId);
                List<WeightedItem> tmpList = new ArrayList<>(scoreList.size());
                for (int i = 0; i < scoreList.size(); i++) {
                    Double score = scoreList.get(i);
                    WeightedItem item = new WeightedItem();
                    item.item_id = i;
                    item.weight = score;
                    tmpList.add(item);
                }
                scoreList = null;
                Collections.sort(tmpList, Collections.reverseOrder());
                List<Integer> predictList = new ArrayList<>(tmpList.size());//该list为当前测试用户的推荐列表
                for (WeightedItem item : tmpList) {
                    predictList.add(item.item_id);
                }
                tmpList = null;



            }
        } catch (Exception e) {

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


    private static class MatrixMultiplier implements Callable<IMatrix<Double>> {
        String modelPath;
        String savePath;

        public MatrixMultiplier(String modelPath, String savePath) {
            this.modelPath = modelPath;
            this.savePath = savePath;
        }

        @Override
        public IMatrix<Double> call() throws Exception {

            WRMF wrmf = new WRMF();
            wrmf.loadModel(modelPath);
            int userNum = wrmf.maxUserID() + 1;
            int itemNum = wrmf.maxItemID() + 1;

            Matrix<Double> mutil = new Matrix<>(userNum, itemNum);
            for (int i = 0; i < userNum; i++) {
                for (int j = 0; j < itemNum; j++) {
                    mutil.set(i, j, wrmf.predict(i, j));
                }
            }
            FileCacheUtil.saveDiskCache(mutil, savePath);
            return mutil;
        }
    }
}
