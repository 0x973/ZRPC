package cn.pingbase.zrpc.config;

import lombok.Data;

/**
 * @author: Zak
 * @date 2022/08/23 10:51
 * @description: TODO
 */
@Data
public class ZRPCSocketConfig {
    private Integer connectTimeoutInMs = 500;
    private Integer readTimeoutInMs = 500;
    private Integer writeTimeoutInMs = 500;

    private Integer maxIdleConnections = 50;
    private Integer keepAliveDurationInMin = 5;
}
