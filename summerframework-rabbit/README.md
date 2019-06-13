# 说明

## 功能

* 重试消息
* 延迟消息
* 对任何一个消息会打印出trace日志，如果有DataSource则自动保存到表message_trace中


# 使用方式

## Maven引入

1. 引入summerframework依赖。有两种方式

第一种是将summerframework-starter-parent作为工程的parent
```xml
	<parent>
		<groupId>com.bkjk.platform.summerframework</groupId>
		<artifactId>summerframework-starter-parent</artifactId>
		<version>LATEST_VERSION</version>
	</parent>
```

第二中是利用dependencyManagement引入依赖

```xml
<dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.bkjk.platform.summerframework</groupId>
                <artifactId>summerframework-dependencies</artifactId>
                <version>LATEST_VERSION</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
         </dependencies>
</dependencyManagement>
```

2. 引入 rabbit 模块
```xml
		<dependency>
			<groupId>com.bkjk.platform.summerframework</groupId>
			<artifactId>platform-starter-rabbit</artifactId>
		</dependency>
```

## 演示代码

* 配置

消息接收端(开启重试队列，本地重试三次后重新提交到死信队列中)

```yaml
spring:
  application:
    name: rabbit-test-app
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtualHost: /
    listener:
      simple:
        retry:
          enabled: true
          max-attempts: 3
    dynamic: true
```

* 消息发送端(发送延迟消息，延迟1秒，注意@Delay)
利用死信队列实现延迟消息。配置Exchange: DeadLetterExchange,队列architect.queue.delay，并用architect.route.delay绑定两者
```
@SpringBootApplication
@EnableScheduling
public class SampleAmqpSimpleApplication {

    @Bean
    public Sender mySender() {
        return new Sender();
    }

    @RabbitListener(queues = "architect.queue")
    public void process(@Payload String foo) {
        System.out.println(new Date() + ": " + foo);
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(SampleAmqpSimpleApplication.class, args);
    }

}

public class Sender {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelay = 100L)
    @Delay(interval = 1000L, queue = "architect.queue")
    public void send() {
        this.rabbitTemplate.convertAndSend("architect.exchange", "architect.route", "hello");
    }

}
```


## 演示结果

* Trace log

以JSON格式存储在文件/opt/app/logs/rabbit.json中 
```text
2019-02-18 12:46:43,062 INFO (Slf4jTraceLogger.java:14)- {"timestamp":"20190218124642","clientIp":"10.0.75.1","applicationName":"rabbit-test-app","messageId":"hEWb2vWj","type":"publish","node":"rabbit#guest","connection":"localhost:5672","vhost":"/","user":"guest","channel":"4","exchange":"architect.exchange","queue":"none","routing_keys":"architect.route","properties":"{\"headers\":{\"MessageLogTraceId\":\"hEWb2vWj\"},\"contentType\":\"text/plain\",\"contentEncoding\":\"UTF-8\",\"contentLength\":5,\"deliveryMode\":\"PERSISTENT\",\"priority\":0,\"deliveryTag\":0,\"finalRetryForMessageWithNoId\":false}","payload":"hello","success":"0"}
2019-02-18 12:46:43,179 INFO (Slf4jTraceLogger.java:14)- {"timestamp":"20190218124643","clientIp":"10.0.75.1","applicationName":"rabbit-test-app","messageId":"NJtNeuF2","type":"publish","node":"rabbit#guest","connection":"localhost:5672","vhost":"/","user":"guest","channel":"5","exchange":"architect.exchange","queue":"none","routing_keys":"architect.route","properties":"{\"headers\":{\"MessageLogTraceId\":\"NJtNeuF2\"},\"contentType\":\"text/plain\",\"contentEncoding\":\"UTF-8\",\"contentLength\":5,\"deliveryMode\":\"PERSISTENT\",\"priority\":0,\"deliveryTag\":0,\"finalRetryForMessageWithNoId\":false}","payload":"hello","success":"0"}
```

如果有DataSource，则会自动创建一张message_trace表，并把log存在表里。保留七天，过期自动清理。


# 详细功能示例代码和测试用例

## trace log 配置
trace log 支持的配置项有 file mysql none。默认不用配，框架会自动检测，如果有数据库优先存数据库，没有就存文件。 file表示仅存储到文件，mysql表示存储到mysql表（只支持mysql），none表示不存储
```yaml
rabbit.trace.log-type: none # file mysql none
```