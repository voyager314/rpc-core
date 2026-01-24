package com.yzy.protocol;

import com.yzy.constant.ProtocolConstant;
import com.yzy.model.RpcRequest;
import com.yzy.model.RpcResponse;
import com.yzy.util.Serializer;
import com.yzy.util.SerializerFactory;
import io.vertx.core.buffer.Buffer;

import java.io.IOException;

public class ProtocolMsgDecoder {
    /**
     * 解码器 buffer->byte[]->protocolmsg
     * @param buffer
     * @return
     * @throws IOException
     */
    public static ProtocolMessage<?> decode(Buffer buffer) throws IOException {
        ProtocolMessage.Header header = new ProtocolMessage.Header();
        byte magic = buffer.getByte(0);
        if(magic!= ProtocolConstant.PROTOCOL_MAGIC)throw new RuntimeException("消息魔数非法");
        //顺序获取请求字段
        header.setMagic(magic);
        header.setVersion(buffer.getByte(1));
        header.setSerializer(buffer.getByte(2));
        header.setType(buffer.getByte(3));
        header.setStatus(buffer.getByte(4));
        header.setRequestId(buffer.getLong(5));
        header.setBodyLength(buffer.getInt(13));
        //请求头占17bit
        byte[] bytes = buffer.getBytes(17, 17 + header.getBodyLength());
        ProtocolMsgSerializerEnum anEnum = ProtocolMsgSerializerEnum.getEnumByCode(header.getSerializer());
        if(anEnum==null)throw new RuntimeException("序列化协议不存在");
        Serializer serializer = SerializerFactory.getInstance(anEnum.getName());
        ProtocolMsgTypeEnum typeEnum = ProtocolMsgTypeEnum.getEnumByValue(header.getType());
        if(typeEnum==null)throw new RuntimeException("消息体类型不存在");
        switch (typeEnum) {
            case REQUEST -> {
                RpcRequest rpcRequest = serializer.deserialize(bytes, RpcRequest.class);
                return new ProtocolMessage<>(header, rpcRequest);
            }
            case RESPONSE -> {
                RpcResponse response = serializer.deserialize(bytes, RpcResponse.class);
                return new ProtocolMessage<>(header, response);
            }
            default -> throw new RuntimeException("暂不支持该消息类型");
        }
    }
}
