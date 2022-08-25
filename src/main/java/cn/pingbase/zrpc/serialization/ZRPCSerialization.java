package cn.pingbase.zrpc.serialization;

import com.alibaba.fastjson2.JSON;

import java.util.List;

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

    public static String toJSONString(Object obj) {
        return JSON.toJSONString(obj);
    }
}
