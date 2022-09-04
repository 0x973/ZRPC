package cn.pingbase.zrpc.util;

import cn.pingbase.zrpc.annotation.ZRPCSerializeBinder;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author: Zak
 * @date 2022/09/04 23:55
 * @description: TODO
 */
public class ZRPCSerializeBinderUtil {

    private ZRPCSerializeBinderUtil() {
    }

    public static Class<?> convertResultType(String resultType, ZRPCSerializeBinder[] annotations) throws ClassNotFoundException {
        if (annotations == null) {
            return Class.forName(resultType);
        }

        if (resultType.startsWith("[")) {
            // Array
            for (ZRPCSerializeBinder annotation : annotations) {
                String remoteClassName = annotation.remoteClassName();
                if (resultType.contains(remoteClassName)) {
                    return Class.forName(resultType.replaceAll(remoteClassName, annotation.currentClass().getName()));
                }
            }
        } else {
            Optional<ZRPCSerializeBinder> serializer = Arrays.stream(annotations)
                    .filter(a -> a.remoteClassName().equals(resultType))
                    .findFirst();
            if (serializer.isPresent()) {
                return serializer.get().currentClass();
            }
        }

        return Class.forName(resultType);
    }

    public static String convertSerializerType(String needMappingTypeName, ZRPCSerializeBinder[] annotations) {
        if (annotations == null) {
            return needMappingTypeName;
        }

        if (needMappingTypeName.startsWith("[")) {
            // Array
            for (ZRPCSerializeBinder annotation : annotations) {
                String currentRuntimeClassName = annotation.currentClass().getName();
                if (needMappingTypeName.contains(currentRuntimeClassName)) {
                    return needMappingTypeName.replaceAll(currentRuntimeClassName, annotation.remoteClassName());
                }
            }
        } else {
            // If a matching annotation is found, use it remoteClassName
            // Otherwise, use argValue class name by default.
            Optional<ZRPCSerializeBinder> serializer = Arrays.stream(annotations)
                    .filter(a -> a.currentClass().getName().equals(needMappingTypeName))
                    .findFirst();
            if (serializer.isPresent()) {
                return serializer.get().remoteClassName();
            }
        }

        return needMappingTypeName;
    }
}
