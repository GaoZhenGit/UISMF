import chosen.nlp.lda.conf.LDAParameter;
import chosen.nlp.lda.conf.PathConfig;
import chosen.nlp.lda.test.ExtractTest;
import chosen.nlp.lda.test.FilterDocTest;
import chosen.nlp.lda.test.LdaTest;
import chosen.nlp.lda.test.UISTest;
import chosen.nlp.lda.util.SparseCalculator;
import com.alibaba.fastjson.JSON;
import com.gz.TIFMF;
import org.social.mf.MfGenerator;
import org.social.test.ItemPredictorMultiTest;
import org.social.util.Parameter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 * Created by host on 2017/7/2.
 */
public class Runner {

    private static PrintWriter mTimeWriter;

    public static void main(String[] args) throws Exception {
        Config config = getConfig(args[0]);

        LDAParameter.K = Parameter.L = config.topicCount;
        LDAParameter.iterations = config.ldaIter + 1;
        LDAParameter.saveStep = config.ldaIter;
        LDAParameter.beginSaveIters = config.ldaIter;
        Parameter.mfFactors = config.mfFactors;
        Parameter.mfIter = config.mfIter;
        Parameter.iL = config.interestTopicCount;
        FilterDocTest.filterBehind = config.dataFilter;
        MfGenerator.className = config.mfMethod;
        com.gz.TIFMF.pyPath = config.tifmfPath;
        TIFMF.tifmfFitNum = config.tifmfFitNum;

        long totalStart = System.currentTimeMillis();

        if (config.dataSetPath != null && config.dataSetPath.length() != 0) {
            PathConfig.twitterUserLinksFile = config.dataSetPath;
            System.out.println("load dataset:" + config.dataSetPath);
        }

        if (config.runModule.FilterDocTest1) {
            FilterDocTest.main(new String[]{"1", "0.01"});
        }

        if (config.runModule.FilterDocTest2) {
            FilterDocTest.main(new String[]{"0", "0.01"});
        }

        if (config.runModule.ExtractTest) {
            ExtractTest.main(new String[]{"1"});
        }

        if (config.runModule.mostpop) {
            MostPop.main(null);
            long spendTime = System.currentTimeMillis() - totalStart;
            recordTime(spendTime, "data/time/mostop.txt");
            return;
        }

        if (config.runModule.dirMf) {
            OnlyMfRunner.main(null);
            long spendTime = System.currentTimeMillis() - totalStart;
            recordTime(spendTime, "data/time/dirmf.txt");
            return;
        }

        if (config.runModule.UISTest1) {
            long start = System.currentTimeMillis();
            if (config.ldaMode.equals("normal")) {
                System.out.println("start normal lda");
                UISTest.main(new String[]{"2",
                        String.valueOf(config.interestTopicCount),
                        String.valueOf(config.ldaThreadHold), "0.01"});
            } else if (config.ldaMode.equals("cb")) {
                System.out.println("start cb lda");
                LdaTest.main(new String[]{
                        String.valueOf(config.ldaThreadHold)
                });
            }
            long end = System.currentTimeMillis();
            recordTime(start, end, "lda");
        }

        if (config.runModule.UISTest2) {
            UISTest.main(new String[]{"2", "read", String.valueOf(config.ldaThreadHold)});
            SparseCalculator.main(new String[]{String.valueOf(config.topicCount)});
        }

        if (config.runModule.dirLda) {
            OnlyLdaRunner.main(null);
            long spendTime = System.currentTimeMillis() - totalStart;
            recordTime(spendTime, "data/time/dirlda.txt");
            return;
        }

        if (config.dynamicThread) {
            ItemPredictorTotal.multiplyThreadCount = (int) Math.sqrt(config.topicCount);
            ItemPredictorMultiTest.mfThreadCount = (int) Math.sqrt(config.topicCount);
            ItemPredictorMultiTest.sumThreadCount = (int) Math.sqrt(config.topicCount);
        } else {
            ItemPredictorMultiTest.mfThreadCount = config.mfThreadCount;
            ItemPredictorTotal.multiplyThreadCount = config.multiplyThreadCount;
            ItemPredictorMultiTest.sumThreadCount = config.sumThreadCount;
        }

        if (config.runModule.ItemPredictorMultiTest1) {
            long start = System.currentTimeMillis();
            ItemPredictorMultiTest.main(new String[]{"2", "2",
                    String.valueOf(config.topicCount),
                    String.valueOf(config.interestTopicCount), "0"});
            long end = System.currentTimeMillis();
            recordTime(start, end, "mf");
        }

        if (config.runModule.multiply) {
            long start = System.currentTimeMillis();
            ItemPredictorTotal.main(new String[]{"1"});
            long end = System.currentTimeMillis();
            recordTime(start, end, "multiply");
        }

        if (config.runModule.score) {
            long start = System.currentTimeMillis();
            ItemPredictorTotal.main(new String[]{"2"});
            long end = System.currentTimeMillis();
            recordTime(start, end, "score");
        }

        if (config.runModule.predict) {
            long start = System.currentTimeMillis();
            ItemPredictorTotal.main(new String[]{
                    "3",
                    Parameter.maxF1Path + config.ldaMode + ".all." + MfGenerator.methodName() +"." + Parameter.L + ".I" + Parameter.iL});
            long end = System.currentTimeMillis();
            recordTime(start, end, "predict");
        }

        long totalEnd = System.currentTimeMillis();
        recordTime(totalStart, totalEnd, "tota;");

        mTimeWriter.flush();
        mTimeWriter.close();
    }

    private static Config getConfig(String path) throws FileNotFoundException {
        File configJson = new File(path);
        Scanner scanner = new Scanner(configJson);
        StringBuilder stringBuilder = new StringBuilder();
        while (scanner.hasNextLine()) {
            stringBuilder.append(scanner.nextLine());
        }
        String configString = stringBuilder.toString();
        Config config = JSON.parseObject(configString, Config.class);
        return config;
    }

    private static void recordTime(long start, long end, String op) {
        try {
            if (mTimeWriter == null) {
                String pathString = "all." + MfGenerator.methodName() + "." + Parameter.L + ".I" + Parameter.iL;
                File file = new File("data/time/" + pathString + ".txt");
                if (!file.exists()) {
                    file.getParentFile().mkdir();
                }
                mTimeWriter = new PrintWriter(file);
            }
            long spendTime = end - start;
            String line = op + ":" + spendTime + "\n";
            mTimeWriter.write(line);
            mTimeWriter.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void recordTime(long spend, String path) {
        File file = new File(path + ".txt");
        if (!file.exists()) {
            file.getParentFile().mkdir();
        }
        try {
            PrintWriter printWriter = new PrintWriter(file);
            printWriter.write("spend:" + spend);
            printWriter.flush();
            printWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
