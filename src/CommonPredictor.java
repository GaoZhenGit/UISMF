import it.unimi.dsi.fastutil.ints.IntList;
import org.mymedialite.data.EntityMapping;
import org.mymedialite.data.IPosOnlyFeedback;
import org.mymedialite.data.WeightedItem;
import org.mymedialite.datatype.IBooleanMatrix;
import org.mymedialite.eval.measures.PrecisionAndRecall;
import org.mymedialite.io.ItemData;

import java.io.File;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.*;


/**
 * Created by host on 2017/8/14.
 */
public class CommonPredictor {
    public static void predict(
            BigMatrix<Double> totalmatrix,
            EntityMapping fmap,
            EntityMapping gmap,
            String testSetPath,
            String resultPath) throws Exception {
        IPosOnlyFeedback testDataWrapper = ItemData.read(testSetPath, fmap, gmap, false);

        IntList testUsers = testDataWrapper.allUsers();
        //获得测试集矩阵，用于获得当前用户的正确推荐items
        IBooleanMatrix itemGetter = testDataWrapper.userMatrix();

        List<Map<Integer, Double>> precList = new ArrayList<>();
        List<Map<Integer, Double>> recallList = new ArrayList<>();
        List<Map<Integer, Double>> ndcgList = new ArrayList<>();
        List<Map<Integer, Boolean>> conList = new ArrayList<>();
        List<Map<Integer, Double>> mrrList = new ArrayList<>();

        int missCount = 0;
        int testUserSize = testUsers.size();
        for (int ti = 0; ti < testUserSize; ti++) {
            Integer testUserId = testUsers.get(ti);
            if (ti % 100 == 0) {
                System.out.println("predict:" + (double) ti / testUserSize);
            }
            if (testUserId >= totalmatrix.dim1) {
                missCount++;
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


            Map<Integer, Double> prec = PrecisionAndRecall.precisionAt(predictList, correct_items, new HashSet<Integer>(), ItemPredictorTotal.predictNum);
            precList.add(prec);
            Map<Integer, Double> recall = PrecisionAndRecall.recallAt(predictList, correct_items, new HashSet<Integer>(), ItemPredictorTotal.predictNum);
            recallList.add(recall);
            Map<Integer, Double> ndcg = ItemPredictorTotal.ndcg(predictList, correct_items, ItemPredictorTotal.predictNum);
            ndcgList.add(ndcg);
            Map<Integer, Boolean> con = ItemPredictorTotal.conversionHit(predictList, correct_items, ItemPredictorTotal.predictNum);
            conList.add(con);
            Map<Integer, Double> mrr = ItemPredictorTotal.mrr(predictList, correct_items, ItemPredictorTotal.predictNum);
            mrrList.add(mrr);
        }

        Map<Integer, Double> prec = ItemPredictorTotal.totalRate(precList);
        Map<Integer, Double> recall = ItemPredictorTotal.totalRate(recallList);
        Map<Integer, Double> ndcg = ItemPredictorTotal.totalRate(ndcgList);
        Map<Integer, Double> con = ItemPredictorTotal.totalConversion(conList);
        Map<Integer, Double> mrr = ItemPredictorTotal.totalRate(mrrList);

        System.out.println("output path: " + resultPath);
        PrintWriter resultWriter = new PrintWriter(new File(resultPath));
        DecimalFormat df = new DecimalFormat("0.000000000000000000");
        DecimalFormat df1 = new DecimalFormat("00");
        for (int n : ItemPredictorTotal.predictNum) {
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
        resultWriter.write("miss test users:" + missCount);
        resultWriter.flush();
        resultWriter.close();
        System.out.println("miss test users:" + missCount);
    }
}
