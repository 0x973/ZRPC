package cn.pingbase.zrpc.server;

import cn.pingbase.zrpc.consts.ZRPConstants;
import cn.pingbase.zrpc.model.ZRPCRequest;
import cn.pingbase.zrpc.model.ZRPCResponse;
import cn.pingbase.zrpc.serialization.ZRPCSerialization;
import cn.pingbase.zrpc.util.CollectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

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
            Class<?>[] argTypes = this.convertArgClass(args);
            Object[] argValues = this.getArgValueArray(args);
            Method method = beanObject.getClass().getMethod(methodName, argTypes);

            try {
                Object result = method.invoke(beanObject, argValues);
                return this.makeResponse(method.getReturnType(), method.getGenericReturnType(), result);
            } catch (InvocationTargetException e) {
                return ZRPCResponse.makeBusinessFailResult(e.getTargetException().getMessage());
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
        if (result == null) {
            return ZRPCResponse.makeSuccessResult(returnTypeName, null);

        } else if (CollectionUtil.isList(returnType)) {
            String elementType = ((ParameterizedType) genericReturnType).getActualTypeArguments()[0].getTypeName();
            return ZRPCResponse.makeSuccessListResult(returnTypeName, elementType, ZRPCSerialization.toJSONString(result));

        } else if (CollectionUtil.isSet(returnType)) {
            String elementType = ((ParameterizedType) genericReturnType).getActualTypeArguments()[0].getTypeName();
            return ZRPCResponse.makeSuccessSetResult(returnTypeName, elementType, ZRPCSerialization.toJSONString(result));

        } else {
            return ZRPCResponse.makeSuccessResult(returnTypeName, ZRPCSerialization.toJSONString(result));
        }
    }

    private Object[] getArgValueArray(List<ZRPCRequest.Argument> args) throws ClassNotFoundException {
        Object[] argValueArray = new Object[args.size()];

        for (int i = 0; i < args.size(); i++) {
            argValueArray[i] = this.parseArgValue(args.get(i));
        }

        return argValueArray;
    }

    private Object parseArgValue(ZRPCRequest.Argument argument) throws ClassNotFoundException {
        switch (argument.getArgType()) {
            case NULL: {
                return null;
            }
            case STRING: {
                return argument.getObjectJson();
            }
            case LIST: {
                Class<?> listClass = Class.forName(argument.getCollectionClassName());
                Class<?> elementClass = Class.forName(argument.getTypeClassName());
                return ZRPCSerialization.parseList(argument.getObjectJson(), listClass, elementClass);
            }
            case SET: {
                Class<?> setClass = Class.forName(argument.getCollectionClassName());
                Class<?> elementClass = Class.forName(argument.getTypeClassName());
                return ZRPCSerialization.parseSet(argument.getObjectJson(), setClass, elementClass);
            }
            case OBJECT:
            default: {
                return ZRPCSerialization.parseObject(argument.getObjectJson(), Class.forName(argument.getTypeClassName()));
            }
        }
    }

    private Class<?>[] convertArgClass(List<ZRPCRequest.Argument> args) throws ClassNotFoundException {
        if (args == null) {
            return new Class<?>[0];
        }

        Class<?>[] argClasses = new Class<?>[args.size()];
        for (int i = 0; i < args.size(); i++) {
            argClasses[i] = Class.forName(args.get(i).getFormalTypeClassName());
        }
        return argClasses;
    }
}
