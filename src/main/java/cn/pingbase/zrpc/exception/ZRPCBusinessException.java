package cn.pingbase.zrpc.exception;

/**
 * @author: Zak
 * @date 2022/08/21 13:46
 * @description: TODO
 */
public class ZRPCBusinessException extends Exception {
    public ZRPCBusinessException(String message) {
        super(message);
    }
}
