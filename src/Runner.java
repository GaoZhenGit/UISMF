import chosen.nlp.lda.conf.LDAParameter;
import chosen.nlp.lda.test.ExtractTest;
import chosen.nlp.lda.test.FilterDocTest;
import chosen.nlp.lda.test.UISTest;
import com.alibaba.fastjson.JSON;
import org.social.test.ItemPredictorMultiTest;
import org.social.util.Parameter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * Created by host on 2017/7/2.
 */
public class Runner {
    public static void main(String[] args) throws Exception {
        Config config = getConfig(args[0]);

        LDAParameter.K = config.topicCount;
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
            UISTest.main(new String[]{"2",
                    String.valueOf(config.topicCount),
                    String.valueOf(config.ldaThreadHold), "0.01"});
        }

        if (config.runModule.UISTest2) {
            UISTest.main(new String[]{"2", "read", String.valueOf(config.ldaThreadHold)});
        }

        if (config.runModule.ItemPredictorMultiTest1) {
            ItemPredictorMultiTest.main(new String[]{"2", "2",
                    String.valueOf(config.topicCount),
                    String.valueOf(config.interestTopicCount), "0"});
        }

        if (config.runModule.ItemPredictorMultiTest2) {
            ItemPredictorMultiTest.main(new String[]{"2", "1",
                    String.valueOf(config.topicCount),
                    String.valueOf(config.interestTopicCount), "0"});
        }
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
}
