package cn.pingbase.zrpc.server;

import cn.pingbase.zrpc.consts.ZRPConstants;
import cn.pingbase.zrpc.model.ZRPCRequest;
import cn.pingbase.zrpc.model.ZRPCResponse;
import cn.pingbase.zrpc.serialization.ZRPCSerialization;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.*;
import java.util.*;

/**
 * @author: Zak
 * @date 2022/08/18 11:46
 * @description: TODO
 */
@Slf4j
@RestController
public class RemoteServiceController {

    private static final String MEDIA_TYPE_JSON = "application/json;charset=utf-8";

    @PostMapping(value = "/zrpc", produces = MEDIA_TYPE_JSON, consumes = MEDIA_TYPE_JSON)
    public ZRPCResponse zrpcMain(@RequestHeader(HttpHeaders.USER_AGENT) String userAgent, @RequestBody ZRPCRequest request) {
        if (!StringUtils.hasLength(userAgent) || !ZRPConstants.REMOTE_CALLER_USERAGENT.equals(userAgent)) {
            return ZRPCResponse.makeFailResult("Your request was denied.");
        }

        String identifier = request.getIdentifier();
        String methodName = request.getMethodName();
        Object beanObject = RemoteServiceBeanStore.get(identifier);
        if (beanObject == null) {
            String reason = String.format("Remote: [target bean could not found, service identifier: %s].", identifier);
            return ZRPCResponse.makeFailResult(reason);
        }

        return this.invoke(beanObject, methodName, request.getArgs());
    }

    private ZRPCResponse invoke(Object beanObject, String methodName, List<ZRPCRequest.Argument> args) {
        try {
            Class<?>[] argClassArray = this.convertArgClass(args);
            Object[] argValueArray = this.getArgValueArray(args);
            Method method = beanObject.getClass().getMethod(methodName, argClassArray);
            try {
                Object result = method.invoke(beanObject, argValueArray);
                return this.makeResponse(method.getReturnType(), method.getGenericReturnType(), result);
            } catch (InvocationTargetException e) {
                return ZRPCResponse.makeFailResult(e.getTargetException().getMessage(), true);
            }
        } catch (NoSuchMethodException e) {
            return ZRPCResponse.makeFailResult("Service method not found, please check your interface class.");
        } catch (Exception e) {
            log.warn("ZRPC Remote call exception.", e);
            return ZRPCResponse.makeFailResult("Server parsing error, message: " + e.getMessage());
        }
    }

    private ZRPCResponse makeResponse(Class<?> returnType, Type genericReturnType, Object result) {
        String returnTypeName = returnType.getName();
        boolean isList = false;

        if (isArrayOrList(returnType)) {
            returnTypeName = ((ParameterizedType) genericReturnType).getActualTypeArguments()[0].getTypeName();
            isList = true;
        }

        if (result == null) {
            return ZRPCResponse.makeSuccessResult(returnTypeName, null);
        } else if (isList) {
            return ZRPCResponse.makeSuccessListResult(returnTypeName, ZRPCSerialization.toJSONString(result));
        } else {
            return ZRPCResponse.makeSuccessResult(returnTypeName, ZRPCSerialization.toJSONString(result));
        }
    }

    private boolean isArrayOrList(Class<?> clazz) {
        return clazz.equals(List.class) || clazz.equals(ArrayList.class) || clazz.equals(Array.class);
    }

    private Object[] getArgValueArray(List<ZRPCRequest.Argument> args) throws ClassNotFoundException {
        Object[] argValueArray = new Object[args.size()];

        for (int i = 0; i < args.size(); i++) {
            ZRPCRequest.Argument argument = args.get(i);
            Object object = argument.getObject();
            String typeClassName = argument.getTypeClassName();
            if (object == null) {
                argValueArray[i] = null;
            } else if (String.class.getTypeName().equals(typeClassName)) {
                argValueArray[i] = object;
            } else if (argument.getIsList()) {
                argValueArray[i] = ZRPCSerialization.parseArray((String) object, Class.forName(typeClassName));
            } else {
                argValueArray[i] = ZRPCSerialization.parseObject((String) object, Class.forName(typeClassName));
            }
        }

        return argValueArray;
    }

    private Class<?>[] convertArgClass(List<ZRPCRequest.Argument> args) throws ClassNotFoundException {
        if (args == null) {
            return new Class<?>[0];
        }

        Class<?>[] argClasses = new Class<?>[args.size()];
        for (int i = 0; i < args.size(); i++) {
            ZRPCRequest.Argument argument = args.get(i);
            if (argument.getIsList()) {
                argClasses[i] = Class.forName(argument.getListClassName());
            } else {
                argClasses[i] = Class.forName(argument.getTypeClassName());
            }
        }
        return argClasses;
    }
}
