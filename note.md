# Vert.x Event Loop 模型笔记

> 结合 `TcpBufferHandlerWrapper.java` 的分析与讨论整理。

---

## 1. Event Loop 模型基础

Vert.x 底层使用 **Netty** 的 `EventLoopGroup`，其整体模型如下：

```
  网卡 NIC
     │  (原始字节流，随时到达，分多个 TCP segment)
     ▼
  Netty Channel Pipeline
     │  (ByteBuf → Vert.x Buffer 转换)
     ▼
  EventLoop 线程 (单线程，非阻塞)
     │  发射 "数据到达" 事件
     ▼
  NetSocket.handler(buffer -> ...)   ← 注册的 handler
```

**核心特性**：
- EventLoop **不主动轮询**，而是被动等待 Netty 的"数据可读"事件，每次数据到达触发一次 handler
- 没有传统意义上的"循环读取"，而是靠**事件驱动回调**驱动
- 同一条连接上的所有事件由**同一个** EventLoop 线程串行处理，**无需加锁**

---

## 2. 双线程池架构

Vert.x 底层有**两个线程池**，职责严格分离：

```
Vert.x 实例
├── Event Loop 线程池（Netty EventLoopGroup）
│     线程数 = 2 × CPU核心数（默认）
│     线程名：vert.x-eventloop-thread-0, 1, 2 ...
│
└── Worker 线程池（WorkerPool）
      线程数 = 20（默认）
      线程名：vert.x-worker-thread-0, 1, 2 ...
```

### 职责划分——黄金法则

| Event Loop 线程 | Worker 线程 |
|----------------|-------------|
| ✅ 处理网络 I/O 事件 | ✅ 执行阻塞操作 |
| ✅ 调用 NetSocket Handler | ✅ 文件读写 |
| ✅ 运行 RecordParser 逻辑 | ✅ 数据库同步查询 |
| ✅ 定时器回调 | ✅ CPU 密集型计算 |
| ✅ 编解码、序列化（轻量） | ✅ Thread.sleep / 锁等待 |
| ❌ 绝对不能阻塞！ | ✅ 可以任意阻塞 |

> **黄金法则**：Event Loop 线程上**绝对不能有阻塞操作**，否则会导致该线程上的所有连接"冻结"。

---

## 3. "单线程串行"与"线程池"并不矛盾

### 常见误解的澄清

> 说 EventLoop "单线程串行处理所有事件" 是在**单条连接的视角**下成立的，
> 对**整个服务器**而言，多个 EventLoop 线程是并行运行的。

### 两个层面的结论

| 观察粒度 | 结论 |
|----------|------|
| **单条连接**视角 | 该连接的所有事件永远由**同一个** EL 线程串行处理，无需加锁 |
| **整个服务器**视角 | 多个 EL 线程**并行**处理不同连接的事件，充分利用多核 |

### 图示

```
Vert.x EventLoop 线程池（例如 8核 → 16个线程）

  EL-0 线程                EL-1 线程                EL-2 线程
  ─────────────            ─────────────            ─────────────
  连接A 的事件1            连接C 的事件1            连接E 的事件1
  连接A 的事件2    并行     连接D 的事件1    并行     连接E 的事件2
  连接B 的事件1   ──────▶  连接C 的事件2   ──────▶  连接F 的事件1
  连接A 的事件3            连接D 的事件2            连接F 的事件2
  连接B 的事件2            连接C 的事件3            ...
  ...                      ...

  ↑ 每个线程内部串行       ↑ 每个线程内部串行       ↑ 每个线程内部串行
  ↑ 线程之间互相并行
```

---

## 4. Netty Multi-Reactor 模式（Vert.x 底层）

```
                  ┌─── acceptorEventLoopGroup（1个线程）
                  │    专门负责 accept 新连接
Vert.x/Netty ─────┤
                  └─── eventLoopGroup（2×核心数 个线程）
                       每个线程是一个独立的 Reactor
                       负责已建立连接的 I/O 读写

                       新连接 accept 后，以 Round-Robin
                       方式分配给某一个 EL 线程，此后
                       该连接的所有 I/O 事件都由这个
                       线程独占处理
```

### 两个线程池的交互方式

```
Event Loop 线程池
       │
       │ 如果需要阻塞操作，通过以下方式切换：
       │
┌──────▼──────────────────────┐
│  vertx.executeBlocking(...)  │  显式提交到 Worker 池
│  @WorkerVerticle 注解        │  整个 Verticle 跑在 Worker 上
└──────┬──────────────────────┘
       │
Worker 线程池
       │
       │ 阻塞操作完成后，结果回调
       │ 被投递回原来的 EventLoop 线程执行
       │
┌──────▼──────────────────────┐
│  context.runOnContext(...)   │  回到 EL 线程处理结果
└─────────────────────────────┘
```

---

## 5. RecordParser 内部机制：如何分批处理 TCP 报文

### 问题背景

TCP 是流式协议，一个逻辑报文可能被拆分成多个 TCP 分片到达（**拆包**），
也可能多个报文粘在一起到达（**粘包**）。`RecordParser` 负责解决这个问题。

### RecordParserImpl 核心源码解析

```java
// 每次网络数据到来，EventLoop 调用此方法
public void handle(Buffer buffer) {
    if (buffer.length() != 0) {
        if (buff == EMPTY_BUFFER) {
            buff = buffer.getBuffer(0, buffer.length()); // 首次直接用
        } else {
            buff.appendBuffer(buffer); // 追加到内部累积缓冲区
        }
    }
    handleParsing(); // 核心：尝试解析
}

private void handleParsing() {
    if (parsing) { return; } // 防止重入
    parsing = true;
    try {
        do {  // ← RecordParser 内部的"同步循环"
            if (demand > 0L) {
                int next;
                if (delimited) {
                    next = parseDelimited();
                } else {
                    next = parseFixed(); // fixed 模式
                }
                if (next == -1) {
                    break; // 数据还不够，等下一次网络数据到来
                }
                // 够了！截取精确字节片段，触发 output handler
                Buffer event = buff.getBuffer(start, next);
                // ... 调用 output handler ...
            }
        } while (...); // 若剩余数据还够一个 record，继续循环
    } finally {
        parsing = false;
    }
}

private int parseFixed() {
    int len = buff.length();
    if (len - start >= recordSize) { // 累积字节 >= 目标长度?
        int end = start + recordSize;
        pos = end;
        return end; // 够了，返回截止位置
    }
    return -1; // 不够，返回 -1 告知等待
}
```

**关键点**：`RecordParser` 内部维护一个 `buff`（累积缓冲区）和 `do-while` 循环，
在**单次 handle 调用内**同步把 buff 里所有满足长度的记录全部吐出去。

---

## 6. TcpBufferHandlerWrapper 完整流程追踪

### 自定义协议报文结构（17字节头部）

| 字段 | 大小 |
|------|------|
| magic | 1 byte |
| version | 1 byte |
| serializer | 1 byte |
| type | 1 byte |
| status | 1 byte |
| requestId | 8 bytes |
| bodyLength | 4 bytes（offset=13） |
| **合计** | **17 bytes** |

### 场景：一个完整请求被拆成多个 TCP 分片

```
时间线：
  t1: 到达 10 字节（报文头才 17 字节，不够）
  t2: 到达 10 字节（累积 20 字节 > 17，头部完整！）
  t3: 到达 body 的前半部分（50字节）
  t4: 到达 body 的后半部分（50字节，body=100字节完整）
```

**逐步追踪**：

```
[t1] EventLoop 触发 handle(10字节)
  → RecordParser.handle() → buff = [10字节]
  → handleParsing(): parseFixed() → 10 < 17 → 返回 -1 → break
  → 什么都不输出，等待

[t2] EventLoop 触发 handle(10字节)
  → RecordParser.handle() → buff.append → buff = [20字节]
  → handleParsing(): parseFixed() → 20 >= 17 → 截取 [0,17)
  → ✅ 触发 output handler，传入 17字节 header buffer

  output handler 第一次被调用（size == -1）：
    size = buffer.getInt(13)   // 从头部解析 body 长度，假设 = 100
    parser.fixedSizeMode(100)  // ★ 切换！下次要等 100 字节
    result.appendBuffer(buffer) // 把头部存起来

  do-while 继续检查：buff 剩余 3 字节 < 100 → break

[t3] EventLoop 触发 handle(50字节)
  → buff.append → buff 剩余 = 53 字节
  → handleParsing(): parseFixed() → 53 < 100 → -1 → break
  → 等待

[t4] EventLoop 触发 handle(50字节)
  → buff.append → buff 剩余 = 103 字节
  → handleParsing(): parseFixed() → 103 >= 100 → 截取 100字节
  → ✅ 触发 output handler，传入 100字节 body buffer

  output handler 第二次被调用（size != -1）：
    result.appendBuffer(buffer)      // header + body 拼完整
    resultHandler.handle(result)     // ★ 真正回调上层！
    size = -1                        // 重置状态
    parser.fixedSizeMode(17)         // 恢复等待下一个报文头
```

### 完整架构图

```
                     TCP 字节流（可能碎片化）
                            │
              ┌─────────────▼──────────────┐
              │    Netty Channel + Vert.x   │
              │    EventLoop (单线程)        │  每次数据到达 → 一次事件
              └─────────────┬──────────────┘
                            │ 调用
              ┌─────────────▼──────────────┐
              │  TcpBufferHandlerWrapper    │
              │    .handle(buffer)          │
              └─────────────┬──────────────┘
                            │ 委托给
              ┌─────────────▼──────────────┐
              │    RecordParser             │
              │  ┌─────────────────────┐   │
              │  │ buff（内部累积缓冲） │   │
              │  │ do-while 解析循环   │   │
              │  │ parseFixed() 判断   │   │
              │  └──────────┬──────────┘   │
              └─────────────┼──────────────┘
                   ┌────────▼────────┐
                   │  output handler │  （匿名内部类，有状态）
                   │                 │
                   │  size == -1 ?   │
                   │  → 解析 header  │
                   │  → fixedSize(n) │  切换为等 body
                   │                 │
                   │  size != -1 ?   │
                   │  → 拼接 body    │
                   │  → 回调上层 ✅  │
                   └─────────────────┘
```

---

## 7. RPC 调用的完整线程协作模型

```
main / test 线程
  │  ServiceProxy.invoke()
  │  CompletableFuture.get()  ←─────────────────────────── 被唤醒
  │  （阻塞等待）                                           │
  │                                                         │ complete()
  │                                         Event Loop 线程（EL-0）
  │                                           │ 网络数据到达事件
  │                                           │ TcpBufferHandlerWrapper
  │                                           │ RecordParser 累积
  │                                           │ output handler
  │                                           │ 解析响应体
  │                                           └──────────────────────▶

Worker 线程（可选，此 RPC 框架中未显式使用）
  如果有数据库查询、文件 IO 等阻塞操作才会用到
```

### 三类线程角色汇总

| 线程 | 数量 | 在此 RPC 框架中的角色 |
|------|------|----------------------|
| `main / test` 线程 | 1 | 发起调用、阻塞等待结果 |
| Event Loop 线程 | 2 × 核心数 | 处理所有 TCP 收发、协议解析、序列化 |
| Worker 线程 | 20（默认） | 框架目前未显式使用 |

---

## 8. 在此 RPC 框架中的应用（8核机器示例）

```
Client 端发出 100 个并发 RPC 请求（100条连接）
                │
        Round-Robin 分配给 16 个 EL 线程
       ┌────────┼────────┐
       ▼        ▼        ▼
     EL-0     EL-1     EL-2  ...（共 16 个 EL 线程）
   (约7条)   (约7条)  (约6条)

   EL-0 内部：串行处理这 7 条连接上的事件
   EL-1 内部：串行处理那 7 条连接上的事件
   各 EL 线程之间：完全并行，互不干扰
```

---

## 9. 三个关键认知总结

| 问题 | 答案 |
|------|------|
| **EventLoop 怎么"循环"读数据？** | 不主动循环，被动等待 Netty 的"数据可读"事件，每次到达触发一次 handler |
| **RecordParser 的"循环"在哪？** | 在 `handleParsing()` 内的 `do-while`，在**单次 handle 调用内**同步把 buff 里所有够长度的记录都吐出去 |
| **分两次处理的本质是什么？** | `output handler` 里的 `size` 字段是**跨事件的状态机**，第一次回调切换 `fixedSizeMode(bodyLen)`，第二次回调才真正完成整条报文，两次回调可能由**不同时刻的 EventLoop 事件**触发 |

---

## 10. VertxTcpClient 异步处理机制

> 源码位置：`rpc-extend/src/main/java/com/yzy/server/tcp/VertxTcpClient.java`

### 问题

**`VertxTcpClient` 中真正实现请求异步处理的是什么，是 `CompletableFuture` 吗？**

### 结论

**不完全是 `CompletableFuture`**，这里实际上是两层机制协作，`CompletableFuture` 反而做的是**逆向操作**——将异步转回同步。

---

### 真正的异步核心：Vert.x 事件循环

```java
// ① 异步发起连接（非阻塞，立即返回，注册回调）
netClient.connect(port, host, result -> {
    // ③ 连接建立后，事件循环触发此回调
    NetSocket socket = result.result();
    socket.write(encode);             // 异步写出请求

    // ④ 注册数据接收处理器（非阻塞，等待数据到达）
    socket.handler(bufferHandlerWrapper); // 数据到达时触发
});
// ② connect() 注册完回调后立即返回，不阻塞
```

底层是 **Netty 的 NIO 事件循环**，所有 I/O 操作（建立连接、写数据、读数据）都是真正的非阻塞异步，由 Vert.x 的 Event Loop 线程驱动。

---

### `CompletableFuture` 实际做的是：异步转同步（阻塞桥接）

```java
CompletableFuture<RpcResponse> responseFuture = new CompletableFuture<>();

// 在 Vert.x 回调内部：异步完成 → 写入结果通知 Future
responseFuture.complete(responseMessage.getBody()); // ← 写入结果

// 调用线程阻塞等待，直到 complete() 被调用才返回
return responseFuture.get(); // ← 阻塞！
```

---

### 各组件职责对比

| 组件 | 实际作用 |
|---|---|
| **Vert.x Event Loop** | 真正的异步 I/O，驱动连接建立、数据收发 |
| **`TcpBufferHandlerWrapper`** | 处理 TCP 粘包 / 拆包，保证读取到完整消息 |
| **`CompletableFuture`** | 桥接工具：把 Vert.x 的回调异步模型，转换为调用方期望的同步返回值 |

---

### 完整数据流

```
调用线程                          Vert.x Event Loop 线程
    │                                      │
    │── netClient.connect() ─────────────> │  注册回调，立即返回
    │── responseFuture.get() ──────────>   │  【调用线程阻塞在这里】
    │                                      │
    │                                      │── 连接成功 → 发送请求
    │                                      │── 数据到达 → bufferHandlerWrapper 处理粘包
    │                                      │── 解码响应报文
    │                                      │── responseFuture.complete(response)
    │                                      │
    │<── 阻塞解除，拿到 response ──────────│
```

---

### 注意事项：`netClient.close()` 的位置

> `netClient.close()` 必须放在响应回调 lambda 内部、`responseFuture.complete()` 之后执行。
> 若将 `close()` 移到回调外部，连接可能在响应到达前就被关闭，导致请求失败。

```java
// ✅ 正确：在收到响应后再关闭
TcpBufferHandlerWrapper bufferHandlerWrapper = new TcpBufferHandlerWrapper(
    buffer -> {
        // ... 解码 ...
        responseFuture.complete(responseMessage.getBody()); // 先完成
        netClient.close();                                  // 再关闭
    }
);

// ❌ 错误：在回调外关闭，连接可能提前断开
socket.handler(bufferHandlerWrapper);
netClient.close(); // 不要放这里
```

---

### 小结

| 问题 | 答案 |
|------|------|
| **真正实现异步的是什么？** | Vert.x Event Loop（底层 Netty NIO），通过回调驱动整个连接和收发过程 |
| **`CompletableFuture` 的作用？** | 同步化桥接工具，将 Vert.x 异步回调转换为阻塞的同步返回值 |
| **`CompletableFuture.get()` 的代价？** | 牺牲了 Vert.x 的全异步优势，调用线程在收到响应前一直阻塞 |
