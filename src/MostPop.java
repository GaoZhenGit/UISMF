import org.mymedialite.eval.measures.PrecisionAndRecall;
import org.social.util.Parameter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.*;


/**
 * Created by GaoZhen on 2017/8/6.
 */
public class MostPop {

    public static final String Dir = "./data/mosttop/";
    public static final String ResultPath = Dir + "mostpop.txt";

    public static void main(String[] args) throws FileNotFoundException {

        File dir = new File(Dir);
        if (!dir.exists()) {
            dir.mkdir();
        }

        Map<Integer, List<Integer>> testData = getTestList();
        List<Integer> predictList = getRecommendList();
        Set<Integer> testSet = testData.keySet();

        List<Map<Integer, Double>> precList = new ArrayList<>();
        List<Map<Integer, Double>> recallList = new ArrayList<>();
        List<Map<Integer, Double>> ndcgList = new ArrayList<>();
        List<Map<Integer, Boolean>> conList = new ArrayList<>();
        List<Map<Integer, Double>> mrrList = new ArrayList<>();

        for (Integer f : testSet) {
            List<Integer> correct_items = testData.get(f);
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


        System.out.println("output path: " + ResultPath);
        PrintWriter resultWriter = new PrintWriter(new File(ResultPath));
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
        resultWriter.flush();
        resultWriter.close();
    }

    private static List<Integer> getRecommendList() throws FileNotFoundException {
        File trainFile = new File(Parameter.trainingDataPath);
        Scanner scanner = new Scanner(trainFile);
        Map<Integer, Gcount> id2count = new HashMap<>();
        while (scanner.hasNextLine()) {
            String[] kv = scanner.nextLine().split(" ");
            String f = kv[0];
            int g = Integer.valueOf(kv[1]);
            if (id2count.get(g) == null) {
                Gcount item = new Gcount();
                item.g = g;
                item.count = 1;
                id2count.put(g, item);
            } else {
                Gcount item = id2count.get(g);
                item.count++;
            }
        }

        List<Gcount> recommendList = new ArrayList<>(id2count.size());
        recommendList.addAll(id2count.values());
        Collections.sort(recommendList, Collections.<Gcount>reverseOrder());
        List<Integer> recommendItemList = new ArrayList<>(recommendList.size());
        for (Gcount item : recommendList) {
            recommendItemList.add(item.g);
        }
        return recommendItemList;
    }

    private static Map<Integer, List<Integer>> getTestList() throws FileNotFoundException {
        File testFile = new File(Parameter.testDataPath);
        Scanner scanner = new Scanner(testFile);
        Map<Integer, List<Integer>> result = new HashMap<>();
        while (scanner.hasNextLine()) {
            String[] kv = scanner.nextLine().split(" ");
            int f = Integer.parseInt(kv[0]);
            int g = Integer.parseInt(kv[1]);

            List<Integer> correctList;
            if (result.get(f) == null) {
                correctList = new ArrayList<>();
                result.put(f, correctList);
            } else {
                correctList = result.get(f);
            }

            correctList.add(g);
        }

        return result;
    }

    private static class Gcount implements Comparable<Gcount> {
        public int g;
        public int count;

        @Override
        public int compareTo(Gcount o) {
            return Integer.valueOf(count).compareTo(Integer.valueOf(o.count));
        }
    }
}
