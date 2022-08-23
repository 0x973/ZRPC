package cn.pingbase.zrpc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author: Zak
 * @date 2022/08/18 22:10
 * @description: TODO
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ZRPCRemoteService {
    /**
     * remote service identifier
     *
     * @return
     */
    String serviceIdentifier();

    /**
     * service impl class(if you have multiple implementations class).
     *
     * @return
     */
    Class<?> serviceImplClass() default Class.class;
}
