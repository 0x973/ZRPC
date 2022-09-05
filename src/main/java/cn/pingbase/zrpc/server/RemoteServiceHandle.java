package cn.pingbase.zrpc.server;

import cn.pingbase.zrpc.model.ZRPCRequest;
import cn.pingbase.zrpc.model.ZRPCResponse;
import cn.pingbase.zrpc.serialization.ZRPCSerialization;
import cn.pingbase.zrpc.util.ZRPCCollectionUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * @author: Zak
 * @date 2022/09/05 01:49
 * @description: TODO
 */
public class RemoteServiceHandle {

    private final Object beanObject;
    private final String methodName;
    private final List<ZRPCRequest.Argument> args;

    public RemoteServiceHandle(Object beanObject, String methodName, List<ZRPCRequest.Argument> args) {
        this.beanObject = beanObject;
        this.methodName = methodName;
        this.args = args;
    }

    public ZRPCResponse invoke() throws ClassNotFoundException, NoSuchMethodException {
        Class<?>[] argTypes = getArgClasses(args);
        Object[] argValues = getArgValues(args);
        Method method = beanObject.getClass().getMethod(methodName, argTypes);

        try {
            Class<?> returnType = method.getReturnType();
            Type genericReturnType = method.getGenericReturnType();
            Object result = method.invoke(beanObject, argValues);
            return this.makeResponse(returnType, genericReturnType, result);

        } catch (InvocationTargetException e) {
            return ZRPCResponse.makeBusinessFailResult(e.getTargetException().getMessage());

        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private ZRPCResponse makeResponse(Class<?> returnType, Type genericReturnType, Object result) {
        String returnTypeName = returnType.getName();
        if (result == null) {
            return ZRPCResponse.makeSuccessResult(returnTypeName, null);
        }

        String jsonString = ZRPCSerialization.toJSONString(result);
        if (ZRPCCollectionUtil.isList(returnType)) {
            String elementType = ((ParameterizedType) genericReturnType).getActualTypeArguments()[0].getTypeName();
            return ZRPCResponse.makeSuccessListResult(returnTypeName, elementType, jsonString);

        } else if (ZRPCCollectionUtil.isSet(returnType)) {
            String elementType = ((ParameterizedType) genericReturnType).getActualTypeArguments()[0].getTypeName();
            return ZRPCResponse.makeSuccessSetResult(returnTypeName, elementType, jsonString);

        } else if (ZRPCCollectionUtil.isMap(returnType)) {
            Type[] actualTypeArguments = ((ParameterizedType) genericReturnType).getActualTypeArguments();
            String keyType = actualTypeArguments[0].getTypeName();
            String valueType = actualTypeArguments[1].getTypeName();
            return ZRPCResponse.makeSuccessMapResult(returnTypeName, keyType, valueType, jsonString);
        }

        return ZRPCResponse.makeSuccessResult(returnTypeName, jsonString);
    }

    private Class<?>[] getArgClasses(List<ZRPCRequest.Argument> args) throws ClassNotFoundException {
        if (args == null) {
            return new Class<?>[0];
        }

        Class<?>[] argClasses = new Class<?>[args.size()];
        for (int i = 0; i < args.size(); i++) {
            argClasses[i] = Class.forName(args.get(i).getFormalTypeClassName());
        }
        return argClasses;
    }

    private Object[] getArgValues(List<ZRPCRequest.Argument> args) throws ClassNotFoundException {
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
            case MAP: {
                Class<?> mapClass = Class.forName(argument.getCollectionClassName());
                Class<?> keyClass = Class.forName(argument.getKeyClassName());
                Class<?> valueClass = Class.forName(argument.getValueClassName());
                return ZRPCSerialization.parseMap(argument.getObjectJson(), mapClass, keyClass, valueClass);
            }
            case OBJECT:
            default: {
                return ZRPCSerialization.parseObject(argument.getObjectJson(), Class.forName(argument.getTypeClassName()));
            }
        }
    }
}
