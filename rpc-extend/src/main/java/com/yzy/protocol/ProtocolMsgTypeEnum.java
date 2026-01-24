package com.yzy.protocol;

import lombok.Getter;

@Getter
public enum ProtocolMsgTypeEnum {
    REQUEST(0),
    RESPONSE(1),
    HEART_BEAT(2),
    OTHERS(3);
    private final int value;
    private ProtocolMsgTypeEnum(int value) {
        this.value = value;
    }

    /**
     * 根据值获取枚举
     * @param value
     * @return
     */
    public static ProtocolMsgTypeEnum getEnumByValue(int value){
        for (ProtocolMsgTypeEnum anEnum : ProtocolMsgTypeEnum.values()) {
            if(anEnum.getValue() == value){
                return anEnum;
            }
        }
        return null ;
    }
}
