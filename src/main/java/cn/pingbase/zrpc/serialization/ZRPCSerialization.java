package cn.pingbase.zrpc.serialization;

import com.alibaba.fastjson2.JSON;

import java.lang.reflect.Array;
import java.util.*;

/**
 * @author: Zak
 * @date 2022/08/25 10:45
 * @description: Serialization utils
 */
public class ZRPCSerialization {
    public static <T> T parseObject(String json, Class<T> clazz) {
        return JSON.parseObject(json, clazz);
    }

    @SuppressWarnings("unchecked")
    public static <E> E[] parseArray(String json, Class<E> elementType) {
        List<E> list = JSON.parseArray(json, elementType);
        E[] array = (E[]) Array.newInstance(elementType, list.size());
        return list.toArray(array);
    }

    @SuppressWarnings("unchecked")
    public static <E> List<E> parseList(String json, Class<?> listType, Class<E> elementType) {
        try {
            if (!listType.isInterface()) {
                Class<? extends AbstractList<E>> type = (Class<? extends AbstractList<E>>) listType;
                AbstractList<E> list = type.newInstance();
                list.addAll(JSON.parseArray(json, elementType));
                return list;
            }

            return JSON.parseArray(json, elementType);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <E> Set<E> parseSet(String json, Class<?> setType, Class<E> elementType) {
        try {
            if (!setType.isInterface()) {
                Class<? extends AbstractSet<E>> type = (Class<? extends AbstractSet<E>>) setType;
                AbstractSet<E> set = type.newInstance();
                set.addAll(JSON.parseArray(json, elementType));
                return set;
            }

            return new HashSet<>(JSON.parseArray(json, elementType));
        } catch (Exception e) {
            return null;
        }
    }

    public static String toJSONString(Object obj) {
        return JSON.toJSONString(obj);
    }
}
