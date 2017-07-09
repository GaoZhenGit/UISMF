package org.social.util;

import java.io.*;

/**
 * Created by host on 2017/7/8.
 */
public class FileCacheUtil {
    public static void saveDiskCache(Serializable serializable, String path) {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        } else {
            File parent = file.getParentFile();
            if (!parent.exists()) {
                parent.mkdir();
            }
        }
        FileOutputStream out;
        try {
            out = new FileOutputStream(file);
            ObjectOutputStream objOut = new ObjectOutputStream(out);
            objOut.writeObject(serializable);
            objOut.flush();
            objOut.close();
//            System.out.println("write object success!");
        } catch (IOException e) {
            System.out.println("write object failed");
            e.printStackTrace();
        }
    }

    public static Object loadDiskCache(String path) {
        Object temp = null;
        File file = new File(path);
        FileInputStream in;
        try {
            in = new FileInputStream(file);
            ObjectInputStream objIn = new ObjectInputStream(in);
            temp = objIn.readObject();
            objIn.close();
//            System.out.println("read object success!");
        } catch (IOException e) {
            System.out.println("read object failed");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return temp;
    }
}
