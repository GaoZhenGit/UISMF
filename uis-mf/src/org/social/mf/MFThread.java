package org.social.mf;

import org.mymedialite.data.EntityMapping;
import org.mymedialite.data.IPosOnlyFeedback;
import org.mymedialite.io.ItemData;
import org.mymedialite.itemrec.MF;
import org.social.util.Parameter;

import java.io.PrintWriter;
import java.util.concurrent.Callable;

/**
 * Created by GaoZhen on 2017/7/29.
 */
public class MFThread implements Callable<MF>{

    private MF mf;
    private Class<? extends MF> mfClazz;
    private String mfPath;
    private String savePath;
    String medium;
    IPosOnlyFeedback testData;
    int tnum;
    EntityMapping userMapping;
    String userMapPath;
    EntityMapping itemMapping;
    String itemMapPath;

    public MFThread(String dataPath,
                    String path,
                    String userMapPath,
                    String itemMapPath,
                    String medium,
                    IPosOnlyFeedback testData,
                    int tnum) {
        this.mfClazz = MfGenerator.generate();
        mfPath = dataPath;
        savePath = path;
        this.userMapPath = userMapPath;
        this.itemMapPath = itemMapPath;
        this.medium = medium;
        this.testData = testData;
        this.tnum = tnum;
    }

    @Override
    public MF call() throws Exception {
        mf = mfClazz.newInstance();
        mf.numFactors = Parameter.mfFactors;
        mf.numIter = Parameter.mfIter;
        userMapping = new EntityMapping();
        itemMapping = new EntityMapping();
        IPosOnlyFeedback training_data = ItemData.read(mfPath, userMapping, itemMapping, false);

        PrintWriter userWriter = new PrintWriter(userMapPath);
        userMapping.saveMapping(userWriter);
        userWriter.flush();
        userWriter.close();

        PrintWriter itemWriter = new PrintWriter(itemMapPath);
        itemMapping.saveMapping(itemWriter);
        itemWriter.flush();
        itemWriter.close();

        mf.setFeedback(training_data);

        userMapping = null;
        itemMapping = null;

        mf.train();
        mf.saveModel(savePath);
        return mf;
    }
}
