import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.annotation.JSONType;

/**
 * Created by host on 2017/7/4.
 */
public class Config {
    public int dataFilter;
    public int topicCount;
    public int interestTopicCount;
    public String ldaMode;
    public RunModule runModule;
    public boolean dynamicThread;
    public double ldaThreadHold;
    public int ldaIter;
    public int mfFactors;
    public int mfIter;
    public int mfThreadCount;
    public int sumThreadCount;
    public String dataSetPath;
    public String mfMethod;


    @JSONType
    public static class RunModule {
        public boolean mostpop;
        public boolean FilterDocTest1;
        public boolean FilterDocTest2;
        public boolean ExtractTest;
        public boolean dirLda;
        public boolean dirMf;
        public boolean UISTest1;
        public boolean UISTest2;
        public boolean ItemPredictorMultiTest1;
        public boolean multiply;
        public boolean score;
        public boolean predict;
    }
}
