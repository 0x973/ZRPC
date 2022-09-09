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
    private String collectionClassName;
    private String resultClassName;
    private String keyClassName;
    private String valueClassName;

    private String dataJson;
    private String message;

    public static ZRPCResponse makeSuccessResult(String resultType, String resultJsonValue) {
        ZRPCResponse response = new ZRPCResponse();
        response.setSuccess(true);
        response.setResultClassName(resultType);
        response.setDataJson(resultJsonValue);
        return response;
    }

    public static ZRPCResponse makeSuccessListResult(String collectionTypeName, String elementType, String resultJsonValue) {
        ZRPCResponse response = new ZRPCResponse();
        response.setSuccess(true);
        response.setArgType(ZRPCArgType.LIST);
        response.setCollectionClassName(collectionTypeName);
        response.setResultClassName(elementType);
        response.setDataJson(resultJsonValue);
        return response;
    }

    public static ZRPCResponse makeSuccessSetResult(String collectionTypeName, String elementType, String resultJsonValue) {
        ZRPCResponse response = new ZRPCResponse();
        response.setSuccess(true);
        response.setArgType(ZRPCArgType.SET);
        response.setCollectionClassName(collectionTypeName);
        response.setResultClassName(elementType);
        response.setDataJson(resultJsonValue);
        return response;
    }

    public static ZRPCResponse makeSuccessMapResult(String collectionTypeName, String keyType, String valueType,
                                                    String resultJsonValue) {
        ZRPCResponse response = new ZRPCResponse();
        response.setSuccess(true);
        response.setArgType(ZRPCArgType.MAP);
        response.setCollectionClassName(collectionTypeName);
        response.setKeyClassName(keyType);
        response.setValueClassName(valueType);
        response.setDataJson(resultJsonValue);
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

    public boolean isMap() {
        return ZRPCArgType.MAP.equals(this.getArgType());
    }
}
