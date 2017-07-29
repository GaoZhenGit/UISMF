package org.social.mf;

import org.mymedialite.data.IPosOnlyFeedback;
import org.mymedialite.itemrec.MF;

/**
 * Created by host on 2017/7/29.
 */
public class MfGenerator {

    public static String className = "org.mymedialite.itemrec.WRMF";

    public static String methodName() {
        String[] a = className.split(".");
        return a[a.length - 1];
    }

    public static Class<? extends MF> generate() {
        try {
            return (Class<? extends MF>) Class.forName(className);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
