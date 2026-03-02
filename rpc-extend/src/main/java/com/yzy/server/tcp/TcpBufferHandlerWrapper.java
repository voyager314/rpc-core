package com.yzy.server.tcp;

import com.yzy.constant.ProtocolConstant;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.parsetools.RecordParser;

public class TcpBufferHandlerWrapper implements Handler<Buffer> {
    private final RecordParser resultParser;
    public TcpBufferHandlerWrapper(Handler<Buffer> resultHandler) {
        //必须传入resultHandler进行回调！
        resultParser=initParser(resultHandler);
    }

    @Override
    public void handle(Buffer buffer) {
        resultParser.handle(buffer);
    }

    private RecordParser initParser(Handler<Buffer> resultHandler){
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
                    // 触发回调，将完整消息传递给调用方
                    //缺失这一步将导致responseFuture.get在主线程无限阻塞等待
                    //也是测试时消费者无法获取远程调用结果，测试进程阻塞的唯一原因！
                    resultHandler.handle(result);
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
