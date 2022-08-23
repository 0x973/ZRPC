package cn.pingbase.zrpc.client;

import com.alibaba.fastjson2.JSON;
import cn.pingbase.zrpc.annotation.ZRPCRemoteClient;
import cn.pingbase.zrpc.annotation.ZRPCSerializeBinder;
import cn.pingbase.zrpc.annotation.ZRPCThrowableBinder;
import cn.pingbase.zrpc.exception.ZRPCBusinessException;
import cn.pingbase.zrpc.result.ZRPCRequest;
import cn.pingbase.zrpc.result.ZRPCResponse;
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
        if (remoteClientAnnotation == null) {
            throw new ZRPCException("Annotation `ZRPCRemoteClient` configuration error.");
        }

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
                return JSON.parseArray(result.getResultValue(), clazz);
            }
            return JSON.parseObject(result.getResultValue(), clazz);
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
        for (int i = 0; i < args.length; i++) {
            Object argValue = args[i];

            ZRPCRequest.Argument argument = new ZRPCRequest.Argument();
            final String findTypeName;
            if (AbstractList.class.equals(argValue.getClass().getSuperclass())) {
                // 集合类型
                ParameterizedType parameterizedType = (ParameterizedType) method.getParameters()[i].getParameterizedType();
                findTypeName = parameterizedType.getActualTypeArguments()[0].getTypeName();
                argument.setListClassName(method.getParameters()[i].getType().getName());
                argument.setIsList(true);
            } else {
                // 对象类型
                findTypeName = argValue.getClass().getTypeName();
                argument.setIsList(false);
            }

            // 找到匹配的注解则使用注解的映射, 没找到用默认的class类型
            Optional<ZRPCSerializeBinder> serializer = Arrays.stream(annotations)
                    .filter(a -> a.currentClass().getTypeName().equals(findTypeName))
                    .findFirst();
            String className = serializer.isPresent() ? serializer.get().remoteClassName() : argValue.getClass().getName();
            argument.setTypeClassName(className);
            Object obj = String.class.getTypeName().equals(className) ? argValue : JSON.toJSONString(argValue);
            argument.setObject(obj);

            argumentList.add(argument);
        }
        return argumentList;
    }

    private void checkResultSuccess(ZRPCResponse result, ZRPCThrowableBinder annotation) throws Throwable {
        if (result == null) {
            throw new ZRPCException("Failed to call zrpc remote service.");
        }

        if (result.isSuccess()) {
            return;
        }

        if (!result.isBusinessException()) {
            throw new ZRPCException(result.getMessage());
        }

        if (annotation != null) {
            Class<? extends Throwable> exceptionClass = annotation.exceptionClass();
            throw exceptionClass.getConstructor(String.class).newInstance(result.getMessage());
        }

        throw new ZRPCBusinessException(result.getMessage());
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