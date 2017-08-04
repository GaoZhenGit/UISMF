
import chosen.nlp.lda.conf.PathConfig;
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
//
//        String fmap = trainingDataDir + Parameter.cfmap + "0";
//        String gmap = trainingDataDir + Parameter.cgmap + "0";
//        WRMF mf = new WRMF();
//        EntityMapping userMapping = new EntityMapping();
//        EntityMapping itemMapping = new EntityMapping();
//        IPosOnlyFeedback training_data = ItemData.read(trainingDataName, userMapping, itemMapping, false);
//        userMapping.saveMapping(new PrintWriter(fmap));
//        itemMapping.saveMapping(new PrintWriter(gmap));
//        System.out.println(training_data.allUsers().size());
//        mf.setFeedback(training_data);
//        mf.train();
//        mf.saveModel(Parameter.IFMFPath +  "all.0");

//        WRMF mf = new WRMF();
//        mf.loadModel(Parameter.IFMFPath +  "all.0");
//
//        int userNum = mf.maxUserID() + 1;
//        int itemNum = mf.maxItemID() + 1;
//
//        Matrix<Double> mutil = new Matrix<Double>(userNum,itemNum);
//        for (int i = 0; i < userNum; i++) {
//            for (int j = 0; j < itemNum; j++) {
//                mutil.set(i,j,mf.predict(i,j));
//            }
//        }
//
//        FileCacheUtil.saveDiskCache(mutil, "./data/mutil.cache");

//        Matrix<Double> mutil = new Matrix<Double>(0,0);
//        mutil.data = (Object[]) FileCacheUtil.loadDiskCache("./data/mutil.cache");
//        Log(mutil.data.length);

        UserCounter.UserItem item = UserCounter.getCommunityCount();
        System.out.println(item.userCount);
        System.out.println(item.itemCount);
        double a = 0.12345678901234567890;
        int b = 10;

        DecimalFormat df=new DecimalFormat("000");
        System.out.print(df.format(b));
    }

    private static void Log(String s) {
        System.out.println(s);
    }
    private static void Log(double s) {
        System.out.println(s);
    }
}
