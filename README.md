# ZRPC(远程过程调用)框架文档
    Ver: 1.0 | Updated: 2022.08.23 | @Zak
### 介绍
ZRPC: 是一个基于Spring的轻量RPC(Remote Procedure Call)框架, 可以提供多服务之间的远程函数调用, 对业务代码无侵入简单配置即可使用
![框架实现原理](images/RPC.jpeg)
### 基础使用

1. 在Maven Java项目pom.xml中(客户端&服务端都需要添加依赖)
    ```xml
    <dependency>
      <groupId>cn.pingbase.zrpc</groupId>
      <artifactId>core</artifactId>
      <version>1.0-SNAPSHOT</version>
    </dependency>
    ```
2. 配置(仅需要在客户端配置, 服务端不需要加配置)
    ```yaml
    # 1)简单配置
    zrpc:
        remotes:
        - serverName: ServerA   # 必填! 这个名字下面客户端注解需要用
          schema: http          # http/https
          endpoint: 127.0.0.1   # 必填! 目标服务端地址(在k8s环境中可以配置service name)
          port: 8080            # 必填! 必须和目标服务端的server.port值一致
    
    # 2)多个远程服务端
    zrpc:
        remotes:
        - serverName: ServerA
          schema: http
          endpoint: 127.0.0.1
          port: 8080
        - serverName: ServerB
          schema: http
          endpoint: 127.0.0.1
          port: 9090
    ```
3. 在客户端和服务端的启动类上注解(`@ZRPCPackageScan`注解非必须, 如果想要精确扫描类请配置)
   ```java
   @SpringBootApplication                              // 必须
   @ComponentScan(basePackages = {"cn.pingbase.zrpc"}) // 必须
   @ZRPCPackageScan(basePackages = {"com.xxx.yyy"})    // 可选, 不填则扫描所有
   public class TestServer {
     public static void main(String[] args) {
       SpringApplication.run(TestServer.class, args);
     }
   }
   ```
4. 在客户端的接口类上注解:
    ```java
    // 注解中的serverName值必须和配置中的serverName值一致
    // 注解中的serviceIdentifier值必须和服务端@ZRPCRemoteService注解的serviceIdentifier值一致
    @ZRPCRemoteClient(serverName = "ServerA", serviceIdentifier = "TestService")
    ```
5. 在服务端的接口类/实现类上注解:
   ```java
   // 注解中的serviceIdentifier值必须和客户端@ZRPCRemoteClient注解的serviceIdentifier值一致
   // serviceImplClass: 可选, 用于包含多个实现类时指定当前接口具体的实现类
   @ZRPCRemoteService(serviceIdentifier = "TestService", serviceImplClass = xxxx.class)
   ```
6. 调用接口
    ```java
    @Autowired
    TestService testService; // (IDEA警告可忽略: "Could not autowire. No beans of 'TestService' type found.")
    
    testService.test();
    ```
---

### 高级使用

#### 1. 自定义序列化类型

当远程函数的返回值/参数值为自定义类型, RPC过程中的序列化会是一个问题
ZRPC为了解决这个问题进行了类型自动映射, 使用者仅需要在客户端的interface中具体函数上注解`@ZRPCSerializeBinder`, 例如:

```java
@ZRPCSerializeBinder(remoteClassName = "com.xxx.remote.model.TestModel", currentClass = TestModel.class)
@ZRPCSerializeBinder(remoteClassName = "com.xxx.remote.model.Body", currentClass = Body.class)
List<TestModel> getTestModel(Body body);
```
也可以使用另外一个注解`@ZRPCSerializeBinders`, 例如:
```java
@ZRPCSerializeBinders({
    @ZRPCSerializeBinder(remoteClassName = "com.xxx.remote.model.TestModel", currentClass = TestModel.class),
    @ZRPCSerializeBinder(remoteClassName = "com.xxx.remote.model.Body", currentClass = Body.class)
})
TestModel getTestModel(Body body);
```

#### 2. 自定义业务异常

当远程函数的执行出现业务的异常(逻辑代码主动抛出的异常), 默认RPC客户端收到的异常类型为`ZRPCBusinessException`
某些场景RPC客户端需要根据特定的异常类型进行逻辑处理, 使用者仅需要在客户端的interface中具体函数上注解`@ZRPCThrowableBinder`即可自定义业务异常类型(异常中的`message`字段已赋值为服务端抛异常时的值)
```java
@ZRPCThrowableBinder(exceptionClass = CustomException.class)
void check();
```
*注意: 自定义的`CustomException`必须继承Exception/RuntimeException/Throwable并实现单个String类型入参的构造函数!*

#### 3. RPC框架socket参数
ZRPC框架支持调整socket参数, 当前默认参数及含义:
```yaml
# 连接超时时间(单位: 毫秒)
connectTimeoutInMs: 500

# 读超时时间(单位: 毫秒)
readTimeoutInMs: 500

# 写超时时间(单位: 毫秒)
writeTimeoutInMs: 500

# 最大闲置连接数
maxIdleConnections: 50

# 闲置连接数最长保活时间(单位: 分钟)
keepAliveDurationInMin: 5
```
完整配置示例:
```yaml
zrpc:
    remotes:
    - serverName: ServerA
      schema: http
      endpoint: 127.0.0.1
      port: 8080
    - serverName: ServerB
      schema: http
      endpoint: 127.0.0.1
      port: 9090
    socket:
          connectTimeoutInMs: 500
          readTimeoutInMs: 500
          writeTimeoutInMs: 500
          maxIdleConnections: 50
          keepAliveDurationInMin: 5
```
