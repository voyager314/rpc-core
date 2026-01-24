package com.yzy.server;


import io.vertx.core.Vertx;

public class VertxHttpServer implements HttpServer {
    @Override
    public void start(int port) {
        Vertx vertx = Vertx.vertx();//获取vertx实例
        //createHttpServer()创建http服务器
        vertx.createHttpServer().requestHandler(req -> {
                    //处理请求
                    System.out.println("received request: " + req.method() + " " + req.uri());
                    //发送响应
                    //req.response().putHeader("content-type", "text/plain").end("Hello from Vert.x");
                })
                .requestHandler(new HttpServerHandler())//处理请求，这里使用我们实现好的HttpServerHandler
                .listen(port, result -> {
                    //监听端口
                    if (result.succeeded()) {
                        System.out.println("server started on port " + port);
                    } else {
                        System.err.println("server failed to start on port " + port);
                    }
                });
    }
}
