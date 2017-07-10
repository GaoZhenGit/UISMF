import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.annotation.JSONType;

/**
 * Created by host on 2017/7/4.
 */
public class Config {
    public int topicCount;
    public int interestTopicCount;
    public RunModule runModule;
    public boolean dynamicThread;
    public double ldaThreadHold;
    public int ldaIter;
    public int mfThreadCount;
    public int sumThreadCount;
    public String dataSetPath;


    @JSONType
    public static class RunModule {
        public boolean FilterDocTest1;
        public boolean FilterDocTest2;
        public boolean ExtractTest;
        public boolean UISTest1;
        public boolean UISTest2;
        public boolean ItemPredictorMultiTest1;
        public boolean ItemPredictorMultiTest2;
    }
}
