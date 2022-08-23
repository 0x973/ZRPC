package cn.pingbase.zrpc.annotation;

import java.lang.annotation.*;

/**
 * @author: Zak
 * @date 2022/08/21 11:11
 * @description: TODO
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(value = ZRPCSerializeBinders.class)
public @interface ZRPCSerializeBinder {
    /**
     * remote class name
     *
     * @return
     */
    String remoteClassName();

    /**
     * current class
     *
     * @return
     */
    Class<?> currentClass();
}
