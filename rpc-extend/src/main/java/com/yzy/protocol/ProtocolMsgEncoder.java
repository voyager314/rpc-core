package com.yzy.protocol;

import com.yzy.util.Serializer;
import com.yzy.util.SerializerFactory;
import io.vertx.core.buffer.Buffer;

import java.io.IOException;

public class ProtocolMsgEncoder {
    /**
     * 编码器 protocolmsg->byte[]->buffer
     * @param protocolMessage
     * @return
     */
    public static Buffer encode(ProtocolMessage<?> protocolMessage) throws IOException {
        Buffer buffer = Buffer.buffer();
        if(protocolMessage==null||protocolMessage.getHeader()==null){
            return buffer;
        }
        ProtocolMessage.Header header = protocolMessage.getHeader();
        buffer.appendByte(header.getMagic())
                .appendByte(header.getVersion())
                .appendByte(header.getSerializer())
                .appendByte(header.getType())
                .appendByte(header.getStatus())
                .appendLong(header.getRequestId());
        ProtocolMsgSerializerEnum anEnum = ProtocolMsgSerializerEnum.getEnumByCode(header.getSerializer());
        if(anEnum==null){
            throw new RuntimeException("序列化协议不存在");
        }
        Serializer serializer = SerializerFactory.getInstance(anEnum.getName());
        byte[] bytes = serializer.serialize(protocolMessage.getBody());
        buffer.appendInt(bytes.length);
        buffer.appendBytes(bytes);
        return buffer;
    }
}
