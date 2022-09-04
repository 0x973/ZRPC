package cn.pingbase.zrpc.client;

import cn.pingbase.zrpc.annotation.ZRPCSerializeBinder;
import cn.pingbase.zrpc.annotation.ZRPCThrowableBinder;
import cn.pingbase.zrpc.exception.ZRPCBusinessException;
import cn.pingbase.zrpc.exception.ZRPCException;
import cn.pingbase.zrpc.model.ZRPCResponse;
import cn.pingbase.zrpc.serialization.ZRPCSerialization;
import cn.pingbase.zrpc.util.ZRPCSerializeBinderUtil;

import java.lang.reflect.Constructor;

/**
 * @author: Zak
 * @date 2022/09/05 01:27
 * @description: TODO
 */
public class RemoteClientResponseUtil {

    private RemoteClientResponseUtil() {}

    public static void checkResponse(ZRPCResponse result, ZRPCThrowableBinder throwableBinder) throws Throwable {
        if (result == null) {
            throw new ZRPCException("Failed to call zrpc remote server.");
        }

        if (result.isSuccess()) {
            return;
        }

        if (!result.isBusinessException()) {
            throw new ZRPCException(result.getMessage());
        }

        if (throwableBinder == null) {
            throw new ZRPCBusinessException(result.getMessage());
        }

        Class<? extends Throwable> exceptionClass = throwableBinder.exceptionClass();
        try {
            Constructor<? extends Throwable> classConstructor = exceptionClass.getConstructor(String.class);
            throw classConstructor.newInstance(result.getMessage());
        } catch (NoSuchMethodException e) {
            throw new ZRPCException("Can not found constructor(String message) method for " + exceptionClass.getSimpleName(), e);
        }
    }

    public static Object parse(ZRPCResponse response, ZRPCSerializeBinder[] serializerAnnotations) throws ClassNotFoundException {
        if (response.isList()) {
            Class<?> clazz = ZRPCSerializeBinderUtil.convertResultType(response.getResultTypeName(), serializerAnnotations);
            Class<?> listType = ZRPCSerializeBinderUtil.convertResultType(response.getCollectionTypeName(), serializerAnnotations);
            return ZRPCSerialization.parseList(response.getResultJsonValue(), listType, clazz);

        } else if (response.isSet()) {
            Class<?> clazz = ZRPCSerializeBinderUtil.convertResultType(response.getResultTypeName(), serializerAnnotations);
            Class<?> setType = ZRPCSerializeBinderUtil.convertResultType(response.getCollectionTypeName(), serializerAnnotations);
            return ZRPCSerialization.parseSet(response.getResultJsonValue(), setType, clazz);

        } else if (response.isMap()) {
            Class<?> mapType = ZRPCSerializeBinderUtil.convertResultType(response.getCollectionTypeName(), serializerAnnotations);
            Class<?> keyType = ZRPCSerializeBinderUtil.convertResultType(response.getKeyTypeName(), serializerAnnotations);
            Class<?> valueType = ZRPCSerializeBinderUtil.convertResultType(response.getValueTypeName(), serializerAnnotations);
            return ZRPCSerialization.parseMap(response.getResultJsonValue(), mapType, keyType, valueType);

        } else {
            Class<?> clazz = ZRPCSerializeBinderUtil.convertResultType(response.getResultTypeName(), serializerAnnotations);
            return ZRPCSerialization.parseObject(response.getResultJsonValue(), clazz);
        }
    }
}
