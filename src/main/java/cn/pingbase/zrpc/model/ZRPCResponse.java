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
    private ZRPCArgType argType = ZRPCArgType.NONE;
    private String collectionTypeName;
    private String resultType;
    private String resultJsonValue;
    private String message;

    public static ZRPCResponse makeSuccessResult(String resultType, String resultJsonValue) {
        ZRPCResponse response = new ZRPCResponse();
        response.setSuccess(true);
        response.setResultType(resultType);
        response.setResultJsonValue(resultJsonValue);
        return response;
    }

    public static ZRPCResponse makeSuccessListResult(String collectionTypeName, String elementType, String resultJsonValue) {
        ZRPCResponse response = new ZRPCResponse();
        response.setSuccess(true);
        response.setArgType(ZRPCArgType.LIST);
        response.setCollectionTypeName(collectionTypeName);
        response.setResultType(elementType);
        response.setResultJsonValue(resultJsonValue);
        return response;
    }

    public static ZRPCResponse makeSuccessSetResult(String collectionTypeName, String elementType, String resultJsonValue) {
        ZRPCResponse response = new ZRPCResponse();
        response.setSuccess(true);
        response.setArgType(ZRPCArgType.SET);
        response.setCollectionTypeName(collectionTypeName);
        response.setResultType(elementType);
        response.setResultJsonValue(resultJsonValue);
        return response;
    }

    public static ZRPCResponse makeFailResult(String reason) {
        ZRPCResponse response = new ZRPCResponse();
        response.setMessage(reason);
        return response;
    }

    public static ZRPCResponse makeBusinessFailResult(String reason) {
        ZRPCResponse response = new ZRPCResponse();
        response.setMessage(reason);
        response.setBusinessException(true);
        return response;
    }

    public boolean isList() {
        return ZRPCArgType.LIST.equals(this.getArgType());
    }

    public boolean isSet() {
        return ZRPCArgType.SET.equals(this.getArgType());
    }
}
