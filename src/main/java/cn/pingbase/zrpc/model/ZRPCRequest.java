package cn.pingbase.zrpc.model;

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
    private List<Argument> args = null;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Argument {
        private ZRPCArgType argType = ZRPCArgType.NONE;

        // 形参类型，用于兼容寻找指定method，除无参函数外此字段必须有值
        private String formalTypeClassName;

        // 集合的实参类型，用于序列化，仅仅在为集合类型的时候不为null
        private String collectionClassName;

        // 实参类型，但多场景下意义不同，用于序列化
        // 1. 在为list/set/array类型时，这个值为element的类型
        // 2. 在基本类型/对象类型时，这个值直接为对应类型(Mapping后)
        private String typeClassName;

        // 仅类型为Map时，两个字段有值
        private String keyClassName;
        private String valueClassName;

        private String dataJson;
    }
}
