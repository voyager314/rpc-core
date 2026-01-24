package com.yzy.server.tcp;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;

/**
 * tcp客户端
 */
public class VertxTcpClient {
    public void start(){
        Vertx vertx = Vertx.vertx();
        vertx.createNetClient().connect(8081,"localhost",result->{
            if(result.succeeded()){
                System.out.println("connect tcp server success");
                NetSocket socket = result.result();
                for (int i = 0; i < 100; i++) {
                    // 发送数据
                    Buffer buffer = Buffer.buffer();
                    String str = "Hello, server!Hello, server!Hello, server!Hello, server!";
                    buffer.appendInt(0);
                    buffer.appendInt(str.getBytes().length);
                    buffer.appendBytes(str.getBytes());
                    socket.write(buffer);
                }
                //接收响应
                socket.handler(buff->{
                    System.out.println("recieve response from tcp server: "+buff.toString());
                });
            }else {
                System.out.println("connect tcp server failed");
            }
        });
    }
    public static void main(String[] args) {new VertxTcpClient().start();}
}
