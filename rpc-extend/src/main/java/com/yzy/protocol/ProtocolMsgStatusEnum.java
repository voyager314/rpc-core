package com.yzy.protocol;

import lombok.Getter;

/**
 * 协议状态枚举
 */
@Getter
public enum ProtocolMsgStatusEnum {
    OK("ok",20),
    BAD_REQUEST("bad request",40),
    BAD_RESPONSE("bad response",50),;

    private final String text;
    private final int value;
    private ProtocolMsgStatusEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据值获取枚举
     * @param value
     * @return
     */
    public static ProtocolMsgStatusEnum getEnumByValue(int value){
        for (ProtocolMsgStatusEnum anEnum : ProtocolMsgStatusEnum.values()) {
            if(anEnum.value == value){
                return anEnum;
            }
        }
        return null;
    }
}
