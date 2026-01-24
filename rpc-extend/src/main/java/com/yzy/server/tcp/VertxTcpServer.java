package com.yzy.server.tcp;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;
import io.vertx.core.parsetools.RecordParser;

public class VertxTcpServer {
    public void start(int port) {
        Vertx vertx = Vertx.vertx();
        NetServer netServer = vertx.createNetServer();
        netServer.connectHandler(socket -> {
            //这里设测试数据的请求头大小为8字节
            RecordParser parser = RecordParser.newFixed(8);
            parser.setOutput(new Handler<Buffer>() {
                int size=-1;
                Buffer result = Buffer.buffer();
                @Override
                public void handle(Buffer buffer) {
                    if(size==-1){
                        //根据请求头获取消息体长度，重置读取的固定大小
                        size=buffer.getInt(4);
                        parser.fixedSizeMode(size);
                        //获取请求头
                        result.appendBuffer(buffer);
                    }else {
                        result.appendBuffer(buffer);
                        System.out.println(result);
                        //重置
                        size=-1;
                        parser.fixedSizeMode(8);
                        result=Buffer.buffer();
                    }
                }
            });
            socket.handler(parser);
        });
        netServer.listen(port,result->{
            if(result.succeeded()){
                System.out.println("Server started on port "+port);
            }else {
                System.out.println("Server failed to start on port "+port);
            }
        });

//        vertx.createNetServer()
//                .connectHandler(new TcpServerHandler())
//                .listen(port, result -> {
//            if (result.succeeded()) {
//                System.out.println("Server started on port " + port);
//            }else {
//                System.out.println("Server failed to start on port " + port);
//            }
//        });
    }
    public static void main(String[] args) {new VertxTcpServer().start(8081);}
}
