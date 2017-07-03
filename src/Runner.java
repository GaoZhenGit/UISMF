import chosen.nlp.lda.conf.LDAParameter;
import chosen.nlp.lda.test.ExtractTest;
import chosen.nlp.lda.test.FilterDocTest;
import chosen.nlp.lda.test.UISTest;
import org.social.test.ItemPredictorMultiTest;
import org.social.util.Parameter;

/**
 * Created by host on 2017/7/2.
 */
public class Runner {
    public static void main(String[] args) throws Exception {

        LDAParameter.K = Integer.parseInt(args[0]);

        FilterDocTest.main(new String[]{"1", "0.01"});

        FilterDocTest.main(new String[]{"0", "0.01"});

        ExtractTest.main(new String[]{"1"});

        UISTest.main(new String[]{"2", args[1], "0.05", "0.01"});

        UISTest.main(new String[]{"2", "read", "0.05"});

        ItemPredictorMultiTest.main(new String[]{"2", "2", args[0], args[1], "0"});

        ItemPredictorMultiTest.main(new String[]{"2", "1", args[0], args[1], "0"});
    }
}
