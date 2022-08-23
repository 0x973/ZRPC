package cn.pingbase.zrpc.config;

import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author: Zak
 * @date 2022/08/18 14:59
 * @description: TODO
 */
@Getter
@Configuration
@ConfigurationProperties("zrpc")
public class ZRPCConfig {
    List<RemoteConfig> remotes = new ArrayList<>();

    ZRPCSocketConfig socket = new ZRPCSocketConfig();

    private Map<String, RemoteConfig> remotesMapCache = null;

    public void setRemotes(List<RemoteConfig> remotes) {
        this.remotes = remotes;
        this.remotesMapCache = this.remotes.stream()
                .collect(Collectors.toConcurrentMap(RemoteConfig::getServerName, config -> config));
    }

    public RemoteConfig getRemoteConfig(String serverName) {
        Assert.notNull(serverName, "serverName is required");
        return this.remotesMapCache.get(serverName);
    }

    @Data
    public static class RemoteConfig {
        private String serverName = "";
        private String schema = "http";
        private String endpoint = "127.0.0.1";
        private Integer port = 80;
    }
}
