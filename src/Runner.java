import chosen.nlp.lda.conf.LDAParameter;
import chosen.nlp.lda.conf.PathConfig;
import chosen.nlp.lda.test.ExtractTest;
import chosen.nlp.lda.test.FilterDocTest;
import chosen.nlp.lda.test.UISTest;
import com.alibaba.fastjson.JSON;
import org.social.mf.MfGenerator;
import org.social.test.ItemPredictorMultiTest;
import org.social.test.ItemPredictorTotal;
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
        LDAParameter.iterations = config.ldaIter;
        Parameter.mfFactors = config.mfFactors;
        Parameter.mfIter = config.mfIter;
        Parameter.iL = config.interestTopicCount;
        FilterDocTest.filterBehind = config.dataFilter;
        MfGenerator.className = config.mfMethod;

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

        if (config.runModule.UISTest1) {
            long start = System.currentTimeMillis();
            UISTest.main(new String[]{"2",
                    String.valueOf(config.interestTopicCount),
                    String.valueOf(config.ldaThreadHold), "0.01"});
            long end = System.currentTimeMillis();
            recordTime(start, end, "lda");
        }

        if (config.runModule.UISTest2) {
            UISTest.main(new String[]{"2", "read", String.valueOf(config.ldaThreadHold)});
        }

        if (config.dynamicThread) {
            ItemPredictorTotal.mfThreadCount = (int) Math.sqrt(config.topicCount);
            ItemPredictorMultiTest.mfThreadCount = (int) Math.sqrt(config.topicCount);
            ItemPredictorMultiTest.sumThreadCount = (int) Math.sqrt(config.topicCount);
        } else {
            ItemPredictorMultiTest.mfThreadCount = config.mfThreadCount;
            ItemPredictorTotal.mfThreadCount = config.mfThreadCount;
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
            ItemPredictorTotal.main(new String[]{"3"});
            long end = System.currentTimeMillis();
            recordTime(start, end, "predict");
        }

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
}
