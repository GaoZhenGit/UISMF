
import chosen.nlp.lda.conf.PathConfig;
import chosen.nlp.lda.model.UIS_LDA_Seperated;
import chosen.nlp.lda.test.UISTest;
import chosen.nlp.lda.util.Documents;
import it.unimi.dsi.fastutil.ints.IntList;
import org.mymedialite.data.EntityMapping;
import org.mymedialite.data.IPosOnlyFeedback;
import org.mymedialite.datatype.Matrix;
import org.mymedialite.eval.CandidateItems;
import org.mymedialite.eval.ItemRecommendationEvaluationResults;
import org.mymedialite.eval.Items;
import org.mymedialite.io.ItemData;
import org.mymedialite.itemrec.WRMF;
import org.social.util.FileCacheUtil;
import org.social.util.Parameter;
import org.social.util.UserCounter;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Map;

/**
 * Created by host on 2017/7/16.
 */
public class Test {
    public static void main(String[] args) throws Exception {
        String trainingDataDir = Parameter.communityDir + "all" + "/";
        String trainingDataName = trainingDataDir + Parameter.cname + "0";

        UserCounter.UserItem item = UserCounter.getCommunityCount();
        System.out.println(item.userCount);
        System.out.println(item.itemCount);

        UISTest.main(new String[]{"2",
                String.valueOf(10),
                String.valueOf(0.01), "0.01"});

    }

    private static void Log(String s) {
        System.out.println(s);
    }
    private static void Log(double s) {
        System.out.println(s);
    }
}
