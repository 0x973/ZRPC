package cn.pingbase.zrpc.client;

import cn.pingbase.zrpc.annotation.ZRPCSerializeBinder;
import cn.pingbase.zrpc.model.ZRPCArgType;
import cn.pingbase.zrpc.model.ZRPCRequest;
import cn.pingbase.zrpc.serialization.ZRPCSerialization;
import cn.pingbase.zrpc.util.ZRPCCollectionUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

/**
 * @author: Zak
 * @date 2022/09/05 01:35
 * @description: TODO
 */
public class RemoteClientRequestHandle {

    private final Method method;
    private final Object[] args;
    private final ZRPCSerializeBinder[] serializerBinders;

    public RemoteClientRequestHandle(Method method, Object[] args) {
        this.method = method;
        this.args = args;
        this.serializerBinders = method.getDeclaredAnnotationsByType(ZRPCSerializeBinder.class);
    }

    public ZRPCRequest makeRequest() {
        ZRPCRequest request = new ZRPCRequest();

        if (args != null) {
            if (request.getArgs() == null) {
                request.setArgs(new ArrayList<>());
            }

            Parameter[] methodParameters = method.getParameters();
            for (int i = 0; i < args.length; i++) {
                request.getArgs().add(this.makeRequestArgument(methodParameters[i], args[i]));
            }
        }

        return request;
    }

    private ZRPCRequest.Argument makeRequestArgument(Parameter methodParameter, Object argValue) {
        ZRPCRequest.Argument argument = new ZRPCRequest.Argument();

        String formalClassName = this.convertSerializerType(methodParameter.getType().getName());
        argument.setFormalTypeClassName(formalClassName);

        if (argValue == null) {
            argument.setArgType(ZRPCArgType.NULL);
            argument.setTypeClassName(formalClassName);
            argument.setDataJson(null);
            return argument;
        }

        if (String.class.equals(argValue.getClass())) {
            argument.setArgType(ZRPCArgType.STRING);
            argument.setTypeClassName(String.class.getName());
            argument.setDataJson((String) argValue);
            return argument;
        }

        String argValueTypeName = this.convertSerializerType(argValue.getClass().getName());
        Type[] actualTypeArguments = this.getActualTypeArguments(methodParameter);

        if (ZRPCCollectionUtil.isList(argValue.getClass())) {
            // List
            this.listHandle(argument, actualTypeArguments, argValueTypeName);
        } else if (ZRPCCollectionUtil.isSet(argValue.getClass())) {
            // Set
            this.setHandle(argument, actualTypeArguments, argValueTypeName);
        } else if (ZRPCCollectionUtil.isMap(argValue.getClass())) {
            // Map
            this.mapHandle(argument, actualTypeArguments, argValueTypeName);
        } else {
            // Object or Array ...
            this.objectHandle(argument, actualTypeArguments, argValueTypeName);
        }
        argument.setDataJson(ZRPCSerialization.toJSONString(argValue));
        return argument;
    }

    private void listHandle(ZRPCRequest.Argument argument, Type[] actualTypeArguments, String argValueTypeName) {
        String elementTypeName = actualTypeArguments[0].getTypeName();
        argument.setTypeClassName(this.convertSerializerType(elementTypeName));
        argument.setArgType(ZRPCArgType.LIST);
        argument.setCollectionClassName(argValueTypeName);
    }

    private void setHandle(ZRPCRequest.Argument argument, Type[] actualTypeArguments, String argValueTypeName) {
        String elementTypeName = actualTypeArguments[0].getTypeName();
        argument.setTypeClassName(this.convertSerializerType(elementTypeName));
        argument.setArgType(ZRPCArgType.SET);
        argument.setCollectionClassName(argValueTypeName);
    }

    private void mapHandle(ZRPCRequest.Argument argument, Type[] actualTypeArguments, String argValueTypeName) {
        String keyTypeName = actualTypeArguments[0].getTypeName();
        String valueTypeName = actualTypeArguments[1].getTypeName();

        argument.setKeyClassName(this.convertSerializerType(keyTypeName));
        argument.setValueClassName(this.convertSerializerType(valueTypeName));

        argument.setArgType(ZRPCArgType.MAP);
        argument.setCollectionClassName(argValueTypeName);
    }

    private void objectHandle(ZRPCRequest.Argument argument, Type[] actualTypeArguments, String argValueTypeName) {
        argument.setTypeClassName(argValueTypeName);
        argument.setArgType(ZRPCArgType.OBJECT);
    }

    private Type[] getActualTypeArguments(Parameter methodParameter) {
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

    private String convertSerializerType(String needMappingTypeName) {
        if (serializerBinders == null) {
            return needMappingTypeName;
        }

        if (needMappingTypeName.startsWith("[")) {
            // Array
            for (ZRPCSerializeBinder annotation : serializerBinders) {
                String currentRuntimeClassName = annotation.currentClass().getName();
                if (needMappingTypeName.contains(currentRuntimeClassName)) {
                    return needMappingTypeName.replaceAll(currentRuntimeClassName, annotation.remoteClassName());
                }
            }
        } else {
            // If a matching annotation is found, use it remoteClassName
            // Otherwise, use argValue class name by default.
            Optional<ZRPCSerializeBinder> serializer = Arrays.stream(serializerBinders)
                    .filter(a -> a.currentClass().getName().equals(needMappingTypeName))
                    .findFirst();
            if (serializer.isPresent()) {
                return serializer.get().remoteClassName();
            }
        }

        return needMappingTypeName;
    }
}
