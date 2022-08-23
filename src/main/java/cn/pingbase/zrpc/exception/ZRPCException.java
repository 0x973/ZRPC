package cn.pingbase.zrpc.exception;

/**
 * @author: Zak
 * @date 2022/08/18 11:55
 * @description: TODO
 */
public class ZRPCException extends Exception {
    public ZRPCException(String message) {
        super(message);
    }

    public ZRPCException(Throwable e) {
        super(e);
    }

    public ZRPCException(String message, Throwable e) {
        super(message, e);
    }
}
