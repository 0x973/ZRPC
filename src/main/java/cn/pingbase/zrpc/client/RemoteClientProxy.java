package cn.pingbase.zrpc.client;

import cn.pingbase.zrpc.annotation.ZRPCRemoteClient;
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

        ZRPCRequest request = new RemoteClientRequestHandle(method, args).makeRequest();
        ZRPCResponse response = remoteCaller.call(serverName, serviceIdentifier, method.getName(), request);

        try {
            return new RemoteClientResponseHandle(method, response).checkResponse().parse();
        } catch (ClassNotFoundException e) {
            throw new ZRPCException("Class not found, please check your package classes.", e);
        }
    }
}
