package cn.pingbase.zrpc.annotation;

import java.lang.annotation.*;

/**
 * @author: Zak
 * @date 2022/08/21 13:41
 * @description: TODO
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ZRPCThrowableBinder {
    Class<? extends Throwable> exceptionClass();
}
