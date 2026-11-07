# rpc-core

基于 Java 17、Vert.x 和 Maven 的 RPC 框架示例。`rpc-extend` 提供 RPC 核心能力，`my-rpc-springboot-starter` 提供 Spring Boot 注解接入。

## 环境要求

- JDK 17
- Maven 3.6+
- etcd（默认地址 `http://localhost:2379`）；运行 provider/consumer 时需要可访问

RPC 默认监听 `localhost:8081`。可在各应用的 `src/main/resources/application.properties` 中覆盖配置。

## 构建

从仓库根目录依次安装核心模块和 starter。starter 依赖本地 Maven 仓库中的 `rpc-extend:1.0-SNAPSHOT`，因此需要先安装核心模块：

```bash
mvn -f rpc-extend/pom.xml clean install
mvn -f my-rpc-springboot-starter/pom.xml clean install
```

starter 坐标为 `com.yzy:my-rpc-springboot-starter:0.0.1-SNAPSHOT`。Spring Boot 示例还依赖 `common:1.0-SNAPSHOT`，构建或运行示例前也需安装：

```bash
mvn -f common/pom.xml clean install
```

`rpc-extend` 的测试中包含需要本机 etcd 的注册中心测试；运行测试前请启动 etcd。仅需编译和安装时可使用 `-DskipTests`。

## rpc-extend 原生用法

非 Spring Boot 应用可直接使用 `rpc-extend`。provider 将服务接口和实现类交给 `ProviderBootStrap` 注册并启动 TCP 服务：

```java
import com.yzy.ServiceImpl.UserService;
import com.yzy.bootstrap.ProviderBootStrap;
import com.yzy.model.ServiceRegisterInfo;
import com.yzy.service.IUserService;

List<ServiceRegisterInfo<?>> services = new ArrayList<>();
services.add(new ServiceRegisterInfo<>(IUserService.class.getName(), UserService.class));
ProviderBootStrap.init(services);
```

consumer 初始化框架后，通过 `ServiceProxyFactory` 创建服务代理并调用共享接口：

```java
import com.yzy.bootstrap.ConsumerBootStrap;
import com.yzy.model.User;
import com.yzy.proxy.ServiceProxyFactory;
import com.yzy.service.IUserService;

ConsumerBootStrap.init();
IUserService userService = ServiceProxyFactory.getProxy(IUserService.class);
User user = userService.getUserByName("mike");
```

provider 与 consumer 应分别运行在独立进程中，并使用可互相访问的 `rpc.serverHost`、`rpc.serverPort` 和相同的注册中心配置。仓库中的完整非 Spring Boot 示例位于 `provider` 和 `consumer`。

## Spring Boot Starter 用法

provider 和 consumer 需要共享服务接口及其数据类型；本仓库将它们放在 `common` 模块。应用依赖 starter 和接口模块：

```xml
<dependency>
    <groupId>com.yzy</groupId>
    <artifactId>my-rpc-springboot-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>com.yzy</groupId>
    <artifactId>common</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

在 Spring Boot 启动类上启用 RPC。provider 启动 TCP 服务；consumer 通常设置 `needServer = false`：

```java
@SpringBootApplication
@EnableRpc(needServer = true) // consumer 使用 false
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

provider 实现共享接口，并用 `@EnableRpcService` 发布服务。未指定 `interfaceClass` 时，starter 使用实现类的第一个接口：

```java
@Service
@EnableRpcService(interfaceClass = IUserService.class)
public class UserService implements IUserService {
    // 实现 IUserService
}
```

consumer 在 Spring Bean 字段上标记 `@EnableRefrence`，starter 会注入该接口的 RPC 代理，之后可像普通接口一样调用：

```java
@Service
public class UserClient {
    @EnableRefrence(interfaceClass = IUserService.class)
    private IUserService userService;

    public User getUser(String name) {
        return userService.getUserByName(name);
    }
}
```

框架从 classpath 的 `application.properties`（也支持 `application.yml`、`application.yaml`）读取 `rpc` 前缀配置。默认注册中心是 etcd，地址为 `http://localhost:2379`；示例配置：

```properties
rpc.serverHost=localhost
rpc.serverPort=8081
rpc.version=1.0
rpc.registryConfig.registry=etcd
rpc.registryConfig.address=http://localhost:2379
```

配置项及默认值见 `rpc-extend/src/main/java/com/yzy/config/RpcConfig.java` 和 `RegistryConfig.java`。starter 接入代码可参考 `exam-provider-springboot` 与 `exam-consumer-springboot`；消费者测试会发起 RPC 调用，运行前需要先启动 etcd 和 provider。

## 目录

```text
common/                    provider 与 consumer 共用的服务接口和数据模型
consumer/                  非 Spring Boot consumer 示例
exam-consumer-springboot/  starter consumer 接入示例
exam-provider-springboot/  starter provider 接入示例
my-rpc-springboot-starter/ Spring Boot 注解与自动初始化
provider/                  非 Spring Boot provider 示例
rpc-extend/                RPC 协议、网络通信、代理、注册中心等核心实现
```
