package cn.pingbase.zrpc.client;

import cn.pingbase.zrpc.model.ZRPCArgType;
import cn.pingbase.zrpc.serialization.ZRPCSerialization;
import cn.pingbase.zrpc.annotation.ZRPCRemoteClient;
import cn.pingbase.zrpc.annotation.ZRPCSerializeBinder;
import cn.pingbase.zrpc.annotation.ZRPCThrowableBinder;
import cn.pingbase.zrpc.exception.ZRPCBusinessException;
import cn.pingbase.zrpc.model.ZRPCRequest;
import cn.pingbase.zrpc.model.ZRPCResponse;
import cn.pingbase.zrpc.exception.ZRPCException;
import cn.pingbase.zrpc.util.CollectionUtil;
import org.springframework.util.StringUtils;

import java.lang.reflect.*;
import java.util.*;

/**
 * @author: Zak
 * @date 2022/08/18 11:42
 * @description: TODO
 */
public class RemoteServiceProxy<T> implements InvocationHandler {

    private static final RemoteCaller remoteCaller = new RemoteCaller();

    private final T target;

    public RemoteServiceProxy(T target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        ZRPCRemoteClient remoteClientAnnotation = method.getDeclaringClass().getAnnotation(ZRPCRemoteClient.class);
        String serverName = remoteClientAnnotation.serverName();
        String serviceIdentifier = remoteClientAnnotation.serviceIdentifier();
        if (!StringUtils.hasLength(serverName) || !StringUtils.hasLength(serviceIdentifier)) {
            throw new ZRPCException("serverName or serviceIdentifier in `ZRPCRemoteClient` can not be empty.");
        }

        ZRPCSerializeBinder[] serializerAnnotations = method.getDeclaredAnnotationsByType(ZRPCSerializeBinder.class);
        ZRPCRequest request = this.makeRequest(serverName, serviceIdentifier, method, args, serializerAnnotations);
        ZRPCResponse result = remoteCaller.call(request);

        ZRPCThrowableBinder throwableBinderAnnotation = method.getDeclaredAnnotation(ZRPCThrowableBinder.class);
        this.checkResultSuccess(result, throwableBinderAnnotation);

        try {
            Class<?> clazz = this.getDeSerializerClass(result.getResultType(), serializerAnnotations);
            if (result.isList()) {
                Class<?> listType = Class.forName(result.getCollectionTypeName());
                return ZRPCSerialization.parseList(result.getResultJsonValue(), listType, clazz);

            } else if (result.isSet()) {
                Class<?> setType = Class.forName(result.getCollectionTypeName());
                return ZRPCSerialization.parseSet(result.getResultJsonValue(), setType, clazz);
            }

            return ZRPCSerialization.parseObject(result.getResultJsonValue(), clazz);
        } catch (ClassNotFoundException e) {
            throw new ZRPCException("Class not found, please check your package classes.", e);
        } catch (Exception e) {
            throw new ZRPCException(e);
        }
    }

    private ZRPCRequest makeRequest(String serverName, String serviceIdentifier, Method method, Object[] args, ZRPCSerializeBinder[] serializerBinders) {
        ZRPCRequest request = new ZRPCRequest();
        request.setServerName(serverName);
        request.setIdentifier(serviceIdentifier);
        request.setMethodName(method.getName());
        request.setArgs(this.makeArgumentList(method, args, serializerBinders));
        return request;
    }

    private List<ZRPCRequest.Argument> makeArgumentList(Method method, Object[] args, ZRPCSerializeBinder[] serializerBinders) {
        List<ZRPCRequest.Argument> argumentList = new ArrayList<>();
        if (args == null) {
            return argumentList;
        }

        for (int i = 0; i < args.length; i++) {
            argumentList.add(this.makeArgument(method, i, args[i], serializerBinders));
        }
        return argumentList;
    }

    private ZRPCRequest.Argument makeArgument(Method method, int methodArgIndex, Object argValue, ZRPCSerializeBinder[] serializerBinders) {
        ZRPCRequest.Argument argument = new ZRPCRequest.Argument();
        String formalClassName = method.getParameters()[methodArgIndex].getType().getName();
        argument.setFormalTypeClassName(this.getMappingArgClassName(formalClassName, serializerBinders));

        if (argValue == null) {
            argument.setArgType(ZRPCArgType.NULL);
            argument.setObjectJson(null);
            return argument;
        }

        if (String.class.equals(argValue.getClass())) {
            argument.setArgType(ZRPCArgType.STRING);
            argument.setTypeClassName(String.class.getName());
            argument.setObjectJson((String) argValue);
            return argument;
        }

        final String needMappingTypeName;
        if (CollectionUtil.isList(argValue.getClass())) {
            // List collection
            ParameterizedType parameterizedType = (ParameterizedType) method.getParameters()[methodArgIndex].getParameterizedType();
            needMappingTypeName = parameterizedType.getActualTypeArguments()[0].getTypeName();
            argument.setArgType(ZRPCArgType.LIST);
            argument.setCollectionClassName(argValue.getClass().getName());

        } else if (CollectionUtil.isSet(argValue.getClass())) {
            // Set collection
            ParameterizedType parameterizedType = (ParameterizedType) method.getParameters()[methodArgIndex].getParameterizedType();
            needMappingTypeName = parameterizedType.getActualTypeArguments()[0].getTypeName();
            argument.setArgType(ZRPCArgType.SET);
            argument.setCollectionClassName(argValue.getClass().getName());

        } else {
            // Object or Array ...
            needMappingTypeName = argValue.getClass().getName();
            argument.setArgType(ZRPCArgType.OBJECT);
        }

        argument.setTypeClassName(this.getMappingArgClassName(needMappingTypeName, serializerBinders));
        argument.setObjectJson(ZRPCSerialization.toJSONString(argValue));
        return argument;
    }

    private void checkResultSuccess(ZRPCResponse result, ZRPCThrowableBinder throwableBinder) throws Throwable {
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

    private String getMappingArgClassName(String needMappingTypeName, ZRPCSerializeBinder[] annotations) {
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
            return needMappingTypeName;
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

    private Class<?> getDeSerializerClass(String resultType, ZRPCSerializeBinder[] annotations) throws ClassNotFoundException {
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
}
