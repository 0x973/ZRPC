package cn.pingbase.zrpc.client;

import org.springframework.beans.factory.FactoryBean;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * @author: Zak
 * @date 2022/08/18 11:41
 * @description: TODO
 */
public class RemoteServiceFactory<T> implements FactoryBean<T> {
    private final Class<T> interfaceType;

    public RemoteServiceFactory(Class<T> interfaceType) {
        this.interfaceType = interfaceType;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getObject() {
        InvocationHandler handler = new RemoteServiceProxy<>(interfaceType);
        return (T) Proxy.newProxyInstance(interfaceType.getClassLoader(), new Class[]{interfaceType}, handler);
    }

    @Override
    public Class<T> getObjectType() {
        return interfaceType;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}