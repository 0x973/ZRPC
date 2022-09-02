package cn.pingbase.zrpc.util;

import java.util.*;

/**
 * @author: Zak
 * @date 2022/09/02 23:39
 * @description: TODO
 */
public class SetUtil {
    public static boolean isSet(Class<?> clazz) {
        return Set.class.equals(clazz) ||
                HashSet.class.equals(clazz) ||
                LinkedHashSet.class.equals(clazz) ||
                TreeSet.class.equals(clazz) ||
                AbstractSet.class.equals(clazz.getSuperclass());
    }
}
