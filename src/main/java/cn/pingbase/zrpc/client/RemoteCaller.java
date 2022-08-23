package cn.pingbase.zrpc.client;

import com.alibaba.fastjson2.JSON;
import cn.pingbase.zrpc.config.ZRPCConfig;
import cn.pingbase.zrpc.config.ZRPCSocketConfig;
import cn.pingbase.zrpc.consts.ZRPConstants;
import cn.pingbase.zrpc.exception.ZRPCException;
import cn.pingbase.zrpc.result.ZRPCRequest;
import cn.pingbase.zrpc.result.ZRPCResponse;
import okhttp3.*;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * @author: Zak
 * @date 2022/08/18 11:36
 * @description: TODO
 */
@Component
public class RemoteCaller implements ApplicationContextAware {

    private static final MediaType MEDIA_TYPE_JSON = MediaType.get("application/json; charset=utf-8");
    private static final String ZRPC_CONTROLLER_PATH = "/zrpc";
    private static ApplicationContext applicationContext;

    public ZRPCResponse call(ZRPCRequest request) throws ZRPCException {
        ZRPCConfig.RemoteConfig remoteConfig = this.getZRPConfig().getRemoteConfig(request.getServerName());
        if (remoteConfig == null) {
            throw new ZRPCException("Server can not found in remote config.");
        }

        try {
            URL url = UriComponentsBuilder.newInstance().scheme(remoteConfig.getSchema())
                    .host(remoteConfig.getEndpoint())
                    .port(remoteConfig.getPort())
                    .path(ZRPC_CONTROLLER_PATH)
                    .build().toUri().toURL();
            return JSON.parseObject(this.sendPost(url, request), ZRPCResponse.class);
        } catch (Exception e) {
            return ZRPCResponse.makeFailResult("Remote call failed, message: " + e.getMessage());
        }
    }

    private String sendPost(URL url, Object obj) throws IOException {
        OkHttpClient client = this.makeHttpClient();
        RequestBody body = RequestBody.create(JSON.toJSONString(obj), MEDIA_TYPE_JSON);
        Request request = new Request.Builder().url(url).post(body)
                .header(HttpHeaders.CONNECTION, "close")
                .header(HttpHeaders.USER_AGENT, ZRPConstants.REMOTE_CALLER_USERAGENT)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }

            try (ResponseBody responseBody = response.body()) {
                if (responseBody != null) {
                    return responseBody.string();
                }
            }
        }

        return null;
    }

    private OkHttpClient makeHttpClient() {
        ZRPCSocketConfig socketConfig = this.getZRPConfig().getSocket();
        OkHttpClient.Builder builder = new OkHttpClient().newBuilder();
        builder.connectTimeout(socketConfig.getConnectTimeoutInMs(), TimeUnit.MILLISECONDS);
        builder.readTimeout(socketConfig.getReadTimeoutInMs(), TimeUnit.MILLISECONDS);
        builder.writeTimeout(socketConfig.getWriteTimeoutInMs(), TimeUnit.MILLISECONDS);
        builder.connectionPool(new ConnectionPool(socketConfig.getMaxIdleConnections(), socketConfig.getKeepAliveDurationInMin(),
                TimeUnit.MINUTES));
        return builder.build();
    }

    private ZRPCConfig getZRPConfig() {
        ZRPCConfig zrpcConfig = applicationContext.getBean(ZRPCConfig.class);
        Assert.notNull(zrpcConfig, "The ZRPC config must config.");
        return zrpcConfig;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        RemoteCaller.applicationContext = applicationContext;
    }
}
