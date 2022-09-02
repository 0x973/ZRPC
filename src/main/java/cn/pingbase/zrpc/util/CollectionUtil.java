package cn.pingbase.zrpc.util;

import java.lang.reflect.Array;
import java.util.*;

/**
 * @author: Zak
 * @date 2022/09/02 23:37
 * @description: TODO
 */
public class CollectionUtil {
    
    public static boolean isArray(Class<?> clazz) {
        return Array.class.equals(clazz) || Array.class.equals(clazz.getSuperclass());
    }

    public static boolean isList(Class<?> clazz) {
        return List.class.equals(clazz) ||
                ArrayList.class.equals(clazz) ||
                LinkedList.class.equals(clazz) ||
                Vector.class.equals(clazz) ||
                AbstractList.class.equals(clazz.getSuperclass());
    }

    public static boolean isSet(Class<?> clazz) {
        return Set.class.equals(clazz) ||
                HashSet.class.equals(clazz) ||
                LinkedHashSet.class.equals(clazz) ||
                TreeSet.class.equals(clazz) ||
                AbstractSet.class.equals(clazz.getSuperclass());
    }
}
