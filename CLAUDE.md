# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a custom RPC framework with TCP-based communication, built using Vert.x and supporting etcd service registry. The project uses Maven with Java 17.

## Module Structure

- **common**: Shared service interfaces and models (e.g., `IUserService`, `User`)
- **rpc-easy**: Basic HTTP-based RPC implementation (legacy, not actively used)
- **rpc-extend**: Main RPC implementation with TCP, registry, load balancing, retry, and fault tolerance
- **consumer/consumer-springboot**: Example consumer applications
- **provider/provider-springboot**: Example provider applications
- **my-rpc-springboot-starter**: Spring Boot auto-configuration module for RPC

## Build and Test Commands

```bash
# Build all modules
mvn clean install

# Build specific module
mvn clean install -pl rpc-extend -am

# Run a specific test
mvn test -Dtest=ClassName

# Run the provider example (from provider module)
mvn exec:java -Dexec.mainClass="com.yzy.ProviderExample"

# Run the consumer example (from consumer module)
mvn exec:java -Dexec.mainClass="com.yzy.ConsumerExample"
```

## Architecture

### Custom Binary Protocol

The RPC uses a custom binary protocol with a 17-byte header:
- magic (1 byte): Protocol magic number `0x1`
- version (1 byte): Protocol version `0x1`
- serializer (1 byte): Serializer type (JDK, JSON, Kryo, Hessian)
- type (1 byte): Message type (REQUEST/RESPONSE)
- status (1 byte): Response status
- requestId (8 bytes): Unique request ID (Snowflake)
- bodyLength (4 bytes): Message body length

See `ProtocolMessage.java` and `ProtocolConstant.java`.

### TCP Server/Client

- `VertxTcpServer`: TCP server using Vert.x NetServer
- `VertxTcpClient`: TCP client with async response handling via `CompletableFuture`
- `TcpBufferHandlerWrapper`: Handles sticky/fragmented packets using Vert.x RecordParser - critical for correct message processing
- `TcpServerHandler`: Server-side request handler

**Important**: When modifying TCP client code, ensure `netClient.close()` is called within the response callback after `responseFuture.complete()`, not outside. Closing outside the callback causes connection issues.

### Service Registry (etcd)

Services are registered in etcd under `/rpc/` path with:
- 30-second lease TTL
- 10-second heartbeat renewal interval via `CronUtil`
- Service discovery queries with prefix matching
- Watch-based cache invalidation for service changes

Implemented in `EtcdRegister.java`.

### Load Balancers

Implemented via SPI in `rpc-extend/loadbalancer/`:
- RandomLB: Random selection
- RoundRobinLB: Round-robin selection
- ConsistentHashLB: Consistent hash (method-name based)

### Fault Handling

- **Retry Strategies**: NoRetry, FixedIntervalRetry
- **Tolerance Strategies**: FailFast (throws exception), FailSafe (returns null)

### Serialization

Supported serializers (SPI-based): JDK, JSON (Jackson), Kryo, Hessian

## Configuration

Configuration is loaded from `application.properties` or `application.yml` in classpath with `rpc.` prefix:

```properties
rpc.name=myrpc
rpc.version=1.0
rpc.serverHost=localhost
rpc.serverPort=8081
rpc.mock=false
rpc.serializer=jdk
rpc.loadBalancer=roundRobin
rpc.retryStrategy=no
rpc.tolerantStrategy=failFast
rpc.registryConfig.registry=etcd
rpc.registryConfig.address=http://localhost:2379
rpc.registryConfig.timeout=5000
```

Configuration is loaded via `ConfigUtil.loadConfig()` using Hutool's `Props`/`Setting`.

## Spring Boot Integration

### Provider Setup

1. Add `@EnableRpc(needServer = true)` to main application class
2. Annotate service implementations with `@EnableRpcService(interfaceClass = ...)`

```java
@SpringBootApplication
@EnableRpc(needServer = true)
public class ProviderApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProviderApplication.class, args);
    }
}

@Component
@EnableRpcService(interfaceClass = IUserService.class)
public class UserServiceImpl implements IUserService {
    // implementation
}
```

### Consumer Setup

1. Add `@EnableRpc(needServer = false)` to main application class
2. Inject service references with `@EnableRefrence(interfaceClass = ...)`

```java
@SpringBootApplication
@EnableRpc(needServer = false)
public class ConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }
}

@Service
public class Consumer {
    @EnableRefrence(interfaceClass = IUserService.class)
    private IUserService userService;
}
```

## Bootstrap Patterns

### Standalone Provider

```java
List<ServiceRegisterInfo<?>> services = List.of(
    new ServiceRegisterInfo<>(UserService.class, UserServiceImpl.class)
);
ProviderBootStrap.init(services);
```

### Standalone Consumer

```java
ConsumerBootStrap.init();
IUserService userService = ServiceProxyFactory.getProxy(IUserService.class);
```

## Key Constants and Enums

- `ProtocolConstant`: Protocol magic number, version, header length
- `RpcConstant`: Default service version, config prefix
- `SerializerKey`: Serializer type constants
- `LoadBalancerConstant`: Load balancer type constants
- `RetryStrategyKey`: Retry strategy constants
- `TolerantKey`: Fault tolerance constants
- `RegistryKeys`: Registry type constants
- `ProtocolMsgTypeEnum`: Message types (REQUEST=0, RESPONSE=1)
- `ProtocolMsgSerializerEnum`: Serializer codes
- `ProtocolMsgStatusEnum`: Response status codes

## Tool Preferences

When researching, looking up documentation, or searching for information, prefer using these tools directly without asking for permission:
- **deepwiki**: For querying GitHub repository documentation and understanding open-source projects
- **context7**: For looking up library/framework API documentation and code examples
- **tavily**: For general web search, current information, and anything not covered by the above two

These tools can be used freely in any conversation without prior confirmation.

## Important Notes

- The framework uses double-checked locking for singleton `RpcConfig`
- Shutdown hooks are registered for graceful service deregistration
- Sticky packet handling is implemented via `TcpBufferHandlerWrapper` with stateful RecordParser
- etcd leases are used for automatic node cleanup when providers fail
- Local caching (`RegisterServiceCache`) reduces registry queries
- SPI is used for extensibility (factories in `xxxFactory` classes)
