package org.social.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by Gaozhen on 2017/7/18.
 */
public class MfIdMaper {
    private static class Item {
        public String f;
        public String g;
        public Item(String f, String g) {
            this.f = f;
            this.g = g;
        }
    }
    public static void idToIndex(String idPath, String indexPath, String mapPath) {
        try {
            List<Item> relationList = new LinkedList<>();
            Map<String, Integer> userMap = new HashMap<>();
            List<String> userList = new LinkedList<>();

            File idFile = new File(idPath);
            Scanner scanner = new Scanner(idFile);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] ids = line.split(" ");
                String fId = ids[0];
                String gId = ids[1];

                relationList.add(new Item(fId,gId));

                if (!userMap.containsKey(fId)) {
                    userMap.put(fId, userList.size());
                    userList.add(fId);
                }
                if (!userMap.containsKey(gId)) {
                    userMap.put(gId, userList.size());
                    userList.add(gId);
                }
            }

            File mapFile = new File(mapPath);
            PrintWriter mapWriter = new PrintWriter(mapFile);
            for (int i = 0; i < userList.size(); i++) {
                mapWriter.write(userList.get(i) + " " + i + "\n");
            }
            mapWriter.flush();
            mapWriter.close();

            File indexFile = new File(indexPath);
            PrintWriter indexWriter = new PrintWriter(indexFile);
            for (int i = 0; i < relationList.size(); i++) {
                Item item = relationList.get(i);
                int fIndex = userMap.get(item.f);
                int gIndex = userMap.get(item.g);
                indexWriter.write("" + fIndex + " " + gIndex + "\n");
            }
            indexWriter.flush();
            indexWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
