package cn.pingbase.zrpc.client;

import cn.pingbase.zrpc.annotation.ZRPCRemoteClient;
import cn.pingbase.zrpc.annotation.ZRPCSerializeBinder;
import cn.pingbase.zrpc.annotation.ZRPCThrowableBinder;
import cn.pingbase.zrpc.exception.ZRPCException;
import cn.pingbase.zrpc.model.ZRPCRequest;
import cn.pingbase.zrpc.model.ZRPCResponse;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author: Zak
 * @date 2022/08/18 11:42
 * @description: TODO
 */
public class RemoteClientProxy<T> implements InvocationHandler {

    private static final RemoteCaller remoteCaller = new RemoteCaller();

    private final T target;

    public RemoteClientProxy(T target) {
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

        ZRPCSerializeBinder[] serializerBinders = method.getDeclaredAnnotationsByType(ZRPCSerializeBinder.class);
        ZRPCThrowableBinder throwableBinderAnnotation = method.getDeclaredAnnotation(ZRPCThrowableBinder.class);

        ZRPCRequest request = RemoteClientRequestUtil.makeRequest(serverName, serviceIdentifier, method, args, serializerBinders);
        ZRPCResponse response = remoteCaller.call(request);
        RemoteClientResponseUtil.checkResponse(response, throwableBinderAnnotation);

        try {
            return RemoteClientResponseUtil.parse(response, serializerBinders);
        } catch (ClassNotFoundException e) {
            throw new ZRPCException("Class not found, please check your package classes.", e);
        } catch (Exception e) {
            throw new ZRPCException(e);
        }
    }
}
