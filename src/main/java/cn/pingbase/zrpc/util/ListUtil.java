package cn.pingbase.zrpc.util;

import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: Zak
 * @date 2022/09/02 23:37
 * @description: TODO
 */
public class ListUtil {
    public static boolean isArrayOrList(Class<?> clazz) {
        return List.class.equals(clazz) ||
                ArrayList.class.equals(clazz) ||
                AbstractList.class.equals(clazz.getSuperclass()) ||
                Array.class.equals(clazz);
    }
}
