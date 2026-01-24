package com.yzy.protocol;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum ProtocolMsgSerializerEnum {
    JDK(0, "jdk"),
    JSON(1, "json"),
    KRYO(2, "kryo"),
    HESSIAN(3, "hessian");
    private final int code;
    private final String name;
    private ProtocolMsgSerializerEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 获取序列化器名列表
     * @return
     */
    public static List<String> getNames(){
        return Arrays.stream(ProtocolMsgSerializerEnum.values()).map(item->item.name).toList();
    }

    /**
     * 根据序列化器代号获取枚举
     * @param code
     * @return
     */
    public static ProtocolMsgSerializerEnum getEnumByCode(int code){
        for (ProtocolMsgSerializerEnum anEnum : ProtocolMsgSerializerEnum.values()) {
            if (anEnum.getCode() == code){
                return anEnum;
            }
        }
        return null;
    }

    /**
     * 根据序列化器名称获取枚举
     * @param name
     * @return
     */
    public static ProtocolMsgSerializerEnum getEnumByName(String name){
        for (ProtocolMsgSerializerEnum anEnum : ProtocolMsgSerializerEnum.values()) {
            if (anEnum.getName().equals(name)){
                return anEnum;
            }
        }
        return null;
    }
}
