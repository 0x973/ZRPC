package cn.pingbase.zrpc.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: Zak
 * @date 2022/09/02 23:37
 * @description: TODO
 */
public class ZRPCCollectionUtil {

    private ZRPCCollectionUtil() {
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

    public static boolean isMap(Class<?> clazz) {
        return Map.class.equals(clazz) ||
                HashMap.class.equals(clazz) ||
                ConcurrentHashMap.class.equals(clazz) ||
                LinkedHashMap.class.equals(clazz) ||
                TreeMap.class.equals(clazz) ||
                IdentityHashMap.class.equals(clazz) ||
                WeakHashMap.class.equals(clazz) ||
                EnumMap.class.equals(clazz) ||
                AbstractMap.class.equals(clazz.getSuperclass());
    }
}
