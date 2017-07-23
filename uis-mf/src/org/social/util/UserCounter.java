package org.social.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * 用于计算每个社区的用户数量和总体用户数量（前人代码没写这部分统计，补锅专用）
 * Created by GaoZhen on 2017/7/23.
 */
public class UserCounter {
    private static Map<String, UserItem> pathCache = new HashMap<>();
    private static UserItem allCache;

    public static UserItem getCommunityCount(String path) {
        UserItem item = pathCache.get(path);
        if (item != null) {
            return item;
        }
        try {
            Scanner com = new Scanner(new File(path));
            Map<String, Integer> userMap = new HashMap<>();
            Map<String, Integer> itemMap = new HashMap<>();
            while (com.hasNextLine()) {
                String[] line = com.nextLine().split(" ");
                userMap.put(line[0], 1);
                itemMap.put(line[1], 1);
            }
            item = new UserItem();
            item.userCount = userMap.size();
            item.itemCount = itemMap.size();
            pathCache.put(path, item);
            return item;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static UserItem getCommunityCount() {
        if (allCache != null) {
            return allCache;
        }
        String basePath = Parameter.communityDir + "all" + "/" + Parameter.cname;
        Map<String, Integer> userMap = new HashMap<>();
        Map<String, Integer> itemMap = new HashMap<>();
        try {
            for (int i = 0; i < Parameter.L; i++) {
                Scanner com = new Scanner(new File(basePath + i));
                while (com.hasNextLine()) {
                    String[] line = com.nextLine().split(" ");
                    userMap.put(line[0], 1);
                    itemMap.put(line[1], 1);
                }
                com.close();
            }
            allCache = new UserItem();
            allCache.userCount = userMap.size();
            allCache.itemCount = itemMap.size();
            return allCache;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



    public static class UserItem {
        public int userCount;
        public int itemCount;
    }
}
