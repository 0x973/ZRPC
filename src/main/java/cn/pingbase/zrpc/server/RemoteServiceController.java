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

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
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
    public ZRPCResponse zrpcMain(@RequestHeader(HttpHeaders.USER_AGENT) String userAgent,
                                 @RequestBody ZRPCRequest request) {
        if (!StringUtils.hasLength(userAgent) || !ZRPConstants.REMOTE_CALLER_USERAGENT.equals(userAgent)) {
            return null;
        }

        String identifier = request.getIdentifier();
        String methodName = request.getMethodName();
        Object beanObject = RemoteServiceBeanStore.getServiceBean(identifier);
        if (beanObject == null) {
            return ZRPCResponse.makeFailResult("Target bean could not found, identifier: " + identifier);
        }

        return this.invoke(beanObject, methodName, request.getArgs());
    }

    private ZRPCResponse invoke(Object beanObject, String methodName, List<ZRPCRequest.Argument> args) {
        try {
            Class<?>[] argClassArray = this.convertArgClass(args);
            Method method = beanObject.getClass().getMethod(methodName, argClassArray);

            Class<?> returnType = method.getReturnType();
            String returnTypeName = returnType.getName();

            boolean isList = false;
            if (returnType.equals(List.class) || returnType.equals(ArrayList.class) || returnType.equals(Array.class)) {
                returnTypeName = ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0].getTypeName();
                isList = true;
            }

            Object[] argValueArray = this.getArgValueArray(args);
            try {
                Object result = method.invoke(beanObject, argValueArray);
                if (isList) {
                    return ZRPCResponse.makeSuccessListResult(returnTypeName, ZRPCSerialization.toJSONString(result));
                }
                return ZRPCResponse.makeSuccessResult(returnTypeName, ZRPCSerialization.toJSONString(result));
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                log.error("ZRPC method call exception", e);
                return ZRPCResponse.makeFailResult("Server call method exception.");
            } catch (Exception businessEx) {
                return ZRPCResponse.makeFailResult(businessEx.getMessage(), true);
            }

        } catch (NoSuchMethodException e) {
            return ZRPCResponse.makeFailResult("Service method not found, please check your interface class.");
        } catch (Exception e) {
            log.warn("ZRPC Remote call exception.", e);
            return ZRPCResponse.makeFailResult("Server parsing error, message: " + e.getMessage());
        }
    }

    private Object[] getArgValueArray(List<ZRPCRequest.Argument> args) throws ClassNotFoundException {
        Object[] argValueArray = new Object[args.size()];

        for (int i = 0; i < args.size(); i++) {
            ZRPCRequest.Argument argument = args.get(i);
            Object object = argument.getObject();
            String typeClassName = argument.getTypeClassName();
            if (String.class.getTypeName().equals(typeClassName)) {
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
