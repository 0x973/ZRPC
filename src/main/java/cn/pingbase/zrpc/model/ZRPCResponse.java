package cn.pingbase.zrpc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Zak
 * @date 2022/08/18 11:39
 * @description: TODO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZRPCResponse {
    private boolean success;
    private boolean isBusinessException;
    private boolean isList;
    private String resultType;
    private String resultValue;
    private String message;

    public static ZRPCResponse makeSuccessResult(String resultType, String resultValue) {
        return new ZRPCResponse(true, false, false, resultType, resultValue, "success");
    }

    public static ZRPCResponse makeSuccessListResult(String resultType, String resultValue) {
        return new ZRPCResponse(true, false, true, resultType, resultValue, "success");
    }

    public static ZRPCResponse makeFailResult(String reason) {
        return new ZRPCResponse(false, false, false, null, null, reason);
    }

    public static ZRPCResponse makeFailResult(String reason, boolean isBusinessException) {
        return new ZRPCResponse(false, isBusinessException, false, null, null, reason);
    }
}
