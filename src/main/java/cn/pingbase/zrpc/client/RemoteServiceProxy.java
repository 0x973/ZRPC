package cn.pingbase.zrpc.client;

import cn.pingbase.zrpc.serialization.ZRPCSerialization;
import cn.pingbase.zrpc.annotation.ZRPCRemoteClient;
import cn.pingbase.zrpc.annotation.ZRPCSerializeBinder;
import cn.pingbase.zrpc.annotation.ZRPCThrowableBinder;
import cn.pingbase.zrpc.exception.ZRPCBusinessException;
import cn.pingbase.zrpc.model.ZRPCRequest;
import cn.pingbase.zrpc.model.ZRPCResponse;
import cn.pingbase.zrpc.exception.ZRPCException;
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
            Class<?> clazz = this.getSerializerClass(serializerAnnotations, result.getResultType());
            if (result.isList()) {
                return ZRPCSerialization.parseArray(result.getResultValue(), clazz);
            }
            return ZRPCSerialization.parseObject(result.getResultValue(), clazz);
        } catch (ClassNotFoundException e) {
            throw new ZRPCException("Class not found, please check your package classes.", e);
        } catch (Exception e) {
            throw new ZRPCException(e);
        }
    }

    private ZRPCRequest makeRequest(String serverName, String serviceIdentifier, Method method, Object[] args,
                                    ZRPCSerializeBinder[] annotations) {
        ZRPCRequest request = new ZRPCRequest();
        request.setServerName(serverName);
        request.setIdentifier(serviceIdentifier);
        request.setMethodName(method.getName());
        request.setArgs(this.makeArgumentList(method, args, annotations));
        return request;
    }

    private List<ZRPCRequest.Argument> makeArgumentList(Method method, Object[] args, ZRPCSerializeBinder[] annotations) {
        List<ZRPCRequest.Argument> argumentList = new ArrayList<>();
        if (args == null) {
            return argumentList;
        }

        for (int i = 0; i < args.length; i++) {
            Object argValue = args[i];
            ZRPCRequest.Argument argument = new ZRPCRequest.Argument();

            final String findTypeName;
            if (argValue == null) {
                findTypeName = method.getParameters()[i].getType().getTypeName();
            } else if (AbstractList.class.equals(argValue.getClass().getSuperclass())) {
                // Collection
                ParameterizedType parameterizedType = (ParameterizedType) method.getParameters()[i].getParameterizedType();
                findTypeName = parameterizedType.getActualTypeArguments()[0].getTypeName();
                argument.setListClassName(method.getParameters()[i].getType().getName());
            } else {
                // Object
                findTypeName = argValue.getClass().getTypeName();
            }

            String argClassName = this.getArgClassName(argValue, findTypeName, annotations);
            argument.setTypeClassName(argClassName);

            Object val = this.convertArgValue(argValue, argClassName);
            argument.setObject(val);

            argumentList.add(argument);
        }
        return argumentList;
    }

    private String getArgClassName(Object argValue, String findTypeName, ZRPCSerializeBinder[] annotations) {
        if (argValue == null) {
            return findTypeName;
        }

        // If a matching annotation is found, use it remoteClassName
        // Otherwise, use argValue class name by default.
        Optional<ZRPCSerializeBinder> serializer = Arrays.stream(annotations)
                .filter(a -> a.currentClass().getTypeName().equals(findTypeName))
                .findFirst();
        return serializer.isPresent() ? serializer.get().remoteClassName() : argValue.getClass().getName();
    }

    private Object convertArgValue(Object argValue, String className) {
        if (argValue == null) {
            return null;
        } else if (String.class.getTypeName().equals(className)) {
            return argValue;
        }
        return ZRPCSerialization.toJSONString(argValue);
    }

    private void checkResultSuccess(ZRPCResponse result, ZRPCThrowableBinder annotation) throws Throwable {
        if (result == null) {
            throw new ZRPCException("Failed to call zrpc remote server.");
        }

        if (result.isSuccess()) {
            return;
        }

        if (!result.isBusinessException()) {
            throw new ZRPCException(result.getMessage());
        }

        if (annotation == null) {
            throw new ZRPCBusinessException(result.getMessage());
        }

        Class<? extends Throwable> exceptionClass = annotation.exceptionClass();
        try {
            Constructor<? extends Throwable> classConstructor = exceptionClass.getConstructor(String.class);
            throw classConstructor.newInstance(result.getMessage());
        } catch (NoSuchMethodException e) {
            throw new ZRPCException("Can not found constructor(String message) method for " + exceptionClass.getSimpleName(), e);
        }
    }

    private Class<?> getSerializerClass(ZRPCSerializeBinder[] serializerAnnotations, String resultType) throws ClassNotFoundException {
        if (serializerAnnotations != null && serializerAnnotations.length > 0) {
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