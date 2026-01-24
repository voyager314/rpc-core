package com.yzy.server;

public interface HttpServer {
    /**
     * 启动服务器
     * @param port 监听的端口
     */
    void start(int port);
}
