package cn.pingbase.zrpc.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: Zak
 * @date 2022/08/22 11:49
 * @description: TODO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZRPCRequest {
    private String serverName = "";
    private String identifier = "";
    private String methodName = "";
    private List<Argument> args = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Argument {
        private Boolean isList = false;
        private String listClassName = "";
        private String typeClassName = "";
        private Object object = null;
    }
}
