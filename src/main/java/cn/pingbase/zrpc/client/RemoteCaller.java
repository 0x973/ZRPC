package cn.pingbase.zrpc.client;

import cn.pingbase.zrpc.serialization.ZRPCSerialization;
import cn.pingbase.zrpc.config.ZRPCConfig;
import cn.pingbase.zrpc.config.ZRPCSocketConfig;
import cn.pingbase.zrpc.consts.ZRPConstants;
import cn.pingbase.zrpc.exception.ZRPCException;
import cn.pingbase.zrpc.model.ZRPCRequest;
import cn.pingbase.zrpc.model.ZRPCResponse;
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

    private volatile ZRPCSocketConfig lastSocketConfig;
    private volatile OkHttpClient httpClient;

    public ZRPCResponse call(ZRPCRequest request) throws ZRPCException {
        ZRPCConfig.RemoteConfig remoteConfig = this.getZRPConfig().getRemoteConfig(request.getServerName());
        if (remoteConfig == null) {
            throw new ZRPCException("Could not find server name in zrpc remote config, name: " + request.getServerName());
        }

        try {
            URL url = UriComponentsBuilder.newInstance().scheme(remoteConfig.getSchema())
                    .host(remoteConfig.getEndpoint())
                    .port(remoteConfig.getPort())
                    .path(ZRPC_CONTROLLER_PATH)
                    .build().toUri().toURL();
            return ZRPCSerialization.parseObject(this.sendPost(url, request), ZRPCResponse.class);
        } catch (Exception e) {
            return ZRPCResponse.makeFailResult("Remote call failed, message: " + e.getMessage());
        }
    }

    private String sendPost(URL url, Object obj) throws IOException {
        RequestBody body = RequestBody.create(ZRPCSerialization.toJSONString(obj), MEDIA_TYPE_JSON);
        Request request = new Request.Builder()
                .header(HttpHeaders.USER_AGENT, ZRPConstants.REMOTE_CALLER_USERAGENT)
                .url(url)
                .post(body)
                .build();

        try (Response response = this.getHttpClient().newCall(request).execute()) {
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

    private OkHttpClient getHttpClient() {
        ZRPCSocketConfig socketConfig = this.getZRPConfig().getSocket();
        if (this.lastSocketConfig == null || !this.lastSocketConfig.equals(socketConfig)) {
            this.prepareHttpClient(socketConfig);
            this.lastSocketConfig = socketConfig;
        }

        return this.httpClient;
    }

    private void prepareHttpClient(ZRPCSocketConfig socketConfig) {
        // In extreme cases, there may be cases where it is initialized twice.
        synchronized (this) {
            ConnectionPool connectionPool = new ConnectionPool(socketConfig.getMaxIdleConnections(), socketConfig.getKeepAliveDurationInMin(), TimeUnit.MINUTES);
            this.httpClient = new OkHttpClient().newBuilder()
                    .connectTimeout(socketConfig.getConnectTimeoutInMs(), TimeUnit.MILLISECONDS)
                    .readTimeout(socketConfig.getReadTimeoutInMs(), TimeUnit.MILLISECONDS)
                    .writeTimeout(socketConfig.getWriteTimeoutInMs(), TimeUnit.MILLISECONDS)
                    .connectionPool(connectionPool)
                    .build();
        }
    }

    private ZRPCConfig getZRPConfig() {
        ZRPCConfig zrpcConfig = applicationContext.getBean(ZRPCConfig.class);
        Assert.notNull(zrpcConfig, "The ZRPC config can not be null.");
        return zrpcConfig;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        RemoteCaller.applicationContext = applicationContext;
    }
}
