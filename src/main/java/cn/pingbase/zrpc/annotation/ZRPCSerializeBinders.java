package cn.pingbase.zrpc.annotation;

import java.lang.annotation.*;

/**
 * @author: Zak
 * @date 2022/08/21 12:16
 * @description: TODO
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ZRPCSerializeBinders {
    ZRPCSerializeBinder[] value();
}
