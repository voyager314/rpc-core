package com.yzy.server.tcp;

import com.yzy.constant.ProtocolConstant;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.parsetools.RecordParser;

public class TcpBufferHandlerWrapper implements Handler<Buffer> {
    private final RecordParser resultParser;
    public TcpBufferHandlerWrapper(Handler<Buffer> resultHandler) {
        resultParser=initParser();
    }

    @Override
    public void handle(Buffer buffer) {
        resultParser.handle(buffer);
    }

    private RecordParser initParser(){
        RecordParser parser = RecordParser.newFixed(ProtocolConstant.MESSAGE_HEADER_LENGTH);
        parser.setOutput(new Handler<Buffer>() {
            int size=-1;
            Buffer result = Buffer.buffer();
            @Override
            public void handle(Buffer buffer) {
                if(size==-1){
                    //根据请求头获取消息体长度，重置读取的固定大小
                    size=buffer.getInt(13);
                    parser.fixedSizeMode(size);
                    //获取请求头
                    result.appendBuffer(buffer);
                }else {
                    result.appendBuffer(buffer);
                    System.out.println(result);
                    //重置
                    size=-1;
                    parser.fixedSizeMode(ProtocolConstant.MESSAGE_HEADER_LENGTH);
                    result=Buffer.buffer();
                }
            }
        });
        return parser;
    }
}
