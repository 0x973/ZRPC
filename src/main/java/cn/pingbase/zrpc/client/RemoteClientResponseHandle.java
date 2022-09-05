package cn.pingbase.zrpc.client;

import cn.pingbase.zrpc.annotation.ZRPCSerializeBinder;
import cn.pingbase.zrpc.annotation.ZRPCThrowableBinder;
import cn.pingbase.zrpc.exception.ZRPCBusinessException;
import cn.pingbase.zrpc.exception.ZRPCException;
import cn.pingbase.zrpc.model.ZRPCResponse;
import cn.pingbase.zrpc.serialization.ZRPCSerialization;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

/**
 * @author: Zak
 * @date 2022/09/05 01:27
 * @description: TODO
 */
public class RemoteClientResponseHandle {

    private final ZRPCSerializeBinder[] serializerAnnotations;
    private final ZRPCThrowableBinder throwableBinderAnnotation;
    private final ZRPCResponse response;

    public RemoteClientResponseHandle(Method method, ZRPCResponse response) {
        this.serializerAnnotations = method.getDeclaredAnnotationsByType(ZRPCSerializeBinder.class);
        this.throwableBinderAnnotation = method.getDeclaredAnnotation(ZRPCThrowableBinder.class);
        this.response = response;
    }

    public RemoteClientResponseHandle checkResponse() throws Throwable {
        if (response == null) {
            throw new ZRPCException("Failed to call zrpc remote server.");
        }

        if (response.isSuccess()) {
            return this;
        }

        if (!response.isBusinessException()) {
            throw new ZRPCException(response.getMessage());
        }

        if (throwableBinderAnnotation == null) {
            throw new ZRPCBusinessException(response.getMessage());
        }

        Class<? extends Throwable> exceptionClass = throwableBinderAnnotation.exceptionClass();
        try {
            Constructor<? extends Throwable> classConstructor = exceptionClass.getConstructor(String.class);
            throw classConstructor.newInstance(response.getMessage());
        } catch (NoSuchMethodException e) {
            throw new ZRPCException("Can not found constructor(String message) method for " + exceptionClass.getSimpleName(), e);
        }
    }

    public Object parse() throws ClassNotFoundException {
        if (response.isList()) {
            Class<?> clazz = this.convertResultType(response.getResultTypeName());
            Class<?> listType = this.convertResultType(response.getCollectionTypeName());
            return ZRPCSerialization.parseList(response.getResultJsonValue(), listType, clazz);

        } else if (response.isSet()) {
            Class<?> clazz = this.convertResultType(response.getResultTypeName());
            Class<?> setType = this.convertResultType(response.getCollectionTypeName());
            return ZRPCSerialization.parseSet(response.getResultJsonValue(), setType, clazz);

        } else if (response.isMap()) {
            Class<?> mapType = this.convertResultType(response.getCollectionTypeName());
            Class<?> keyType = this.convertResultType(response.getKeyTypeName());
            Class<?> valueType = this.convertResultType(response.getValueTypeName());
            return ZRPCSerialization.parseMap(response.getResultJsonValue(), mapType, keyType, valueType);

        } else {
            Class<?> clazz = this.convertResultType(response.getResultTypeName());
            return ZRPCSerialization.parseObject(response.getResultJsonValue(), clazz);
        }
    }

    private Class<?> convertResultType(String resultType) throws ClassNotFoundException {
        if (serializerAnnotations == null) {
            return Class.forName(resultType);
        }

        if (resultType.startsWith("[")) {
            // Array
            for (ZRPCSerializeBinder annotation : serializerAnnotations) {
                String remoteClassName = annotation.remoteClassName();
                if (resultType.contains(remoteClassName)) {
                    return Class.forName(resultType.replaceAll(remoteClassName, annotation.currentClass().getName()));
                }
            }
        } else {
            Optional<ZRPCSerializeBinder> serializer = Arrays.stream(serializerAnnotations)
                    .filter(a -> a.remoteClassName().equals(resultType))
                    .findFirst();
            if (serializer.isPresent()) {
                return serializer.get().currentClass();
            }
        }

        return Class.forName(resultType);
    }
}
