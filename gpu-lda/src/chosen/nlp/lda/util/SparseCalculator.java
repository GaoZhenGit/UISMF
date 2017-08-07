package chosen.nlp.lda.util;

import chosen.nlp.lda.conf.LDAParameter;
import chosen.nlp.lda.conf.PathConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by host on 2017/8/7.
 */
public class SparseCalculator {
    public static void main(String[] args) throws FileNotFoundException {
        int topicCount = Integer.parseInt(args[0]);
        File output = new File(LDAParameter.SparsePath);
        PrintWriter writer = new PrintWriter(output);
        for (int i = 0; i < topicCount; i++) {
            File c = new File(PathConfig.LdaResultsPath + "/all/c_" + i);
            Scanner scanner = new Scanner(c);
            Map<Integer, Integer> fcount = new HashMap<>();
            Map<Integer, Integer> gcount = new HashMap<>();
            int lineCount = 0;
            while (scanner.hasNextLine()) {
                String[] kv = scanner.nextLine().split(" ");
                int f = Integer.parseInt(kv[0]);
                int g = Integer.parseInt(kv[1]);

                if (fcount.get(f) == null) {
                    fcount.put(f, 1);
                }

                if (gcount.get(g) == null) {
                    gcount.put(g, 1);
                }
                lineCount++;
            }
            int fc = fcount.size();
            int gc = gcount.size();
            double sparse = 1 - ((double)lineCount) / fc / gc;
            writer.write("matrix" + i +": user:" + fc + " item:" + gc + " sparse:" + sparse + "\n");
        }
        writer.flush();
        writer.close();
    }
}
