package cn.pingbase.zrpc.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: Zak
 * @date 2022/08/18 11:47
 * @description: TODO
 */
public class RemoteServiceBeanStore {
    private static final Map<String, Object> serviceBeans = new ConcurrentHashMap<>();

    public static boolean contains(String serviceIdentifier) {
        return serviceBeans.containsKey(serviceIdentifier);
    }

    public static void put(String serviceIdentifier, Object bean) {
        serviceBeans.put(serviceIdentifier, bean);
    }

    public static Object get(String serviceIdentifier) {
        return serviceBeans.get(serviceIdentifier);
    }

    public static Map<String, Object> getCopyMap() {
        return new ConcurrentHashMap<>(serviceBeans);
    }
}
