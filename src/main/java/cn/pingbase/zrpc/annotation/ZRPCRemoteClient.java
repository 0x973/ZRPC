package cn.pingbase.zrpc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author: Zak
 * @date 2022/08/18 11:40
 * @description: TODO
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ZRPCRemoteClient {

    /**
     * remote server instance name
     * @return
     */
    String serverName();

    /**
     * remote server identifier
     * @return
     */
    String serviceIdentifier();
}
