package cn.pingbase.zrpc.serialization;

import cn.pingbase.zrpc.util.SetUtil;
import com.alibaba.fastjson2.JSON;

import java.util.AbstractSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author: Zak
 * @date 2022/08/25 10:45
 * @description: Serialization utils
 */
public class ZRPCSerialization {
    public static <T> T parseObject(String json, Class<T> clazz) {
        return JSON.parseObject(json, clazz);
    }

    public static <T> List<T> parseArray(String json, Class<T> clazz) {
        return JSON.parseArray(json, clazz);
    }

    @SuppressWarnings("unchecked")
    public static <E> Set<E> parseSet(String json, Class<?> setType, Class<E> elementType) {
        if (!SetUtil.isSet(setType)) {
            return null;
        }

        try {
            if (!Set.class.equals(setType)) {
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
