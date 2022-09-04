package cn.pingbase.zrpc.client;

import cn.pingbase.zrpc.annotation.ZRPCSerializeBinder;
import cn.pingbase.zrpc.model.ZRPCArgType;
import cn.pingbase.zrpc.model.ZRPCRequest;
import cn.pingbase.zrpc.serialization.ZRPCSerialization;
import cn.pingbase.zrpc.util.ZRPCCollectionUtil;
import cn.pingbase.zrpc.util.ZRPCSerializeBinderUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * @author: Zak
 * @date 2022/09/05 01:35
 * @description: TODO
 */
public class RemoteClientRequestUtil {
    private RemoteClientRequestUtil() {
    }

    public static ZRPCRequest makeRequest(String serverName, String serviceIdentifier, Method method, Object[] args, ZRPCSerializeBinder[] serializerBinders) {
        ZRPCRequest request = new ZRPCRequest();
        request.setServerName(serverName);
        request.setIdentifier(serviceIdentifier);
        request.setMethodName(method.getName());

        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                request.getArgs().add(makeArgument(method, i, args[i], serializerBinders));
            }
        }

        return request;
    }

    private static ZRPCRequest.Argument makeArgument(Method method, int methodArgIndex, Object argValue,
                                                     ZRPCSerializeBinder[] serializerBinders) {
        ZRPCRequest.Argument argument = new ZRPCRequest.Argument();

        Parameter methodParameter = method.getParameters()[methodArgIndex];
        String formalClassName = ZRPCSerializeBinderUtil.convertSerializerType(methodParameter.getType().getName(),
                serializerBinders);
        argument.setFormalTypeClassName(formalClassName);

        if (argValue == null) {
            argument.setArgType(ZRPCArgType.NULL);
            argument.setTypeClassName(formalClassName);
            argument.setObjectJson(null);
            return argument;
        }

        if (String.class.equals(argValue.getClass())) {
            argument.setArgType(ZRPCArgType.STRING);
            argument.setTypeClassName(String.class.getName());
            argument.setObjectJson((String) argValue);
            return argument;
        }

        String argValueTypeName = ZRPCSerializeBinderUtil.convertSerializerType(argValue.getClass().getName(), serializerBinders);
        Type[] actualTypeArguments = getActualTypeArguments(methodParameter);

        if (ZRPCCollectionUtil.isList(argValue.getClass())) {
            listHandle(argument, actualTypeArguments, argValueTypeName, serializerBinders);

        } else if (ZRPCCollectionUtil.isSet(argValue.getClass())) {
            setHandle(argument, actualTypeArguments, argValueTypeName, serializerBinders);

        } else if (ZRPCCollectionUtil.isMap(argValue.getClass())) {
            mapHandle(argument, actualTypeArguments, argValueTypeName, serializerBinders);

        } else {
            // Object or Array ...
            argument.setTypeClassName(argValueTypeName);
            argument.setArgType(ZRPCArgType.OBJECT);
        }
        argument.setObjectJson(ZRPCSerialization.toJSONString(argValue));
        return argument;
    }

    private static void listHandle(ZRPCRequest.Argument argument, Type[] actualTypeArguments, String argValueTypeName,
                                   ZRPCSerializeBinder[] serializerBinders) {
        String elementTypeName = actualTypeArguments[0].getTypeName();
        argument.setTypeClassName(ZRPCSerializeBinderUtil.convertSerializerType(elementTypeName, serializerBinders));
        argument.setArgType(ZRPCArgType.LIST);
        argument.setCollectionClassName(argValueTypeName);
    }

    private static void setHandle(ZRPCRequest.Argument argument, Type[] actualTypeArguments, String argValueTypeName,
                                  ZRPCSerializeBinder[] serializerBinders) {
        String elementTypeName = actualTypeArguments[0].getTypeName();
        argument.setTypeClassName(ZRPCSerializeBinderUtil.convertSerializerType(elementTypeName, serializerBinders));
        argument.setArgType(ZRPCArgType.SET);
        argument.setCollectionClassName(argValueTypeName);
    }

    private static void mapHandle(ZRPCRequest.Argument argument, Type[] actualTypeArguments, String argValueTypeName,
                                  ZRPCSerializeBinder[] serializerBinders) {
        String keyTypeName = actualTypeArguments[0].getTypeName();
        String valueTypeName = actualTypeArguments[1].getTypeName();

        argument.setKeyClassName(ZRPCSerializeBinderUtil.convertSerializerType(keyTypeName, serializerBinders));
        argument.setValueClassName(ZRPCSerializeBinderUtil.convertSerializerType(valueTypeName, serializerBinders));

        argument.setArgType(ZRPCArgType.MAP);
        argument.setCollectionClassName(argValueTypeName);
    }

    private static Type[] getActualTypeArguments(Parameter methodParameter) {
        if (methodParameter == null) {
            return null;
        }

        Type type = methodParameter.getParameterizedType();
        if (!(type instanceof ParameterizedType)) {
            return null;
        }

        ParameterizedType parameterizedType = (ParameterizedType) type;
        return parameterizedType.getActualTypeArguments();
    }
}
