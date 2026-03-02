package com.yzy.server.tcp;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetServer;

public class VertxTcpServer {
    public void start(int port) {
        Vertx vertx = Vertx.vertx();
        NetServer netServer = vertx.createNetServer();

        netServer.connectHandler(new TcpServerHandler());

        netServer.listen(port,result->{
            if(result.succeeded()){
                System.out.println("Server started on port "+port);
            }else {
                System.out.println("Server failed to start on port "+port+" "+result.cause());
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
    //public static void main(String[] args) {new VertxTcpServer().start(8081);}
}
