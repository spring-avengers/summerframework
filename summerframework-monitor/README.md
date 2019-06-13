# 说明

关于监控的架构详情可以看这里[全链路监控](https://confluence.bkjk-inc.com/pages/viewpage.action?pageId=20712091)
#### 对所有的依赖组件进行了Metrcis打点
* CPU、内存、磁盘空间
* JVM Memory、GC、thread、classes等
* tomcat、HttpRequest
* RestTemplate、Feign
* Hystrix
* 数据库连接池、SQL执行时间、慢SQL检测、事务开启和关闭
* Spring的ThreadPoolTaskExecutor和ThreadPoolTaskScheduler
* Redis
* Kafaka、RabbitMQ
* Log events
#### Grafana报表展示
* [Grafanab开发环境](https://grafana.dev.bkjk-inc.com/d/y44VRBLmk/overview?orgId=1)
* [Grafanab生产环境](https://grafana.ocean.bkjk-inc.com/d/y44VRBLmk/overview?orgId=1)
#### 支持自定义打点
#### 支持自定义Skywalking的Span
#### 格式化日志的输出格式(所有的日志将会带上Skywalking的TraceId)
#### 该组件依赖于[configcenter](https://confluence.bkjk-inc.com/pages/viewpage.action?pageId=25542101)

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

2. 引入 monitor 模块
```xml
      <dependency>
         <groupId>com.bkjk.platform.summerframework</groupId>
         <artifactId>platform-starter-monitor</artifactId>
      </dependency>
```
或者,如果应用时web类应用，则可直接引入platform-starter-web模块，platform-starter-web中已经包含monitor模块
```xml
      <dependency>
         <groupId>com.bkjk.platform.summerframework</groupId>
         <artifactId>platform-starter-web</artifactId>
      </dependency>
```

## 演示代码

如无自定义埋点，无需做任何代码改动

## 演示结果

接入成功后，日志里会自动打印http request、traceId和sql等信息，另外可在[Grafana](https://grafana.dev.bkjk-inc.com/d/y44VRBLmk/overview?orgId=1)中可以看到应用的指标信息，在[SkyWalking](https://skywalking.dev.bkjk-inc.com/)中可以看到链路监控信息。

```xml
[2019-02-14 11:10:03.718] [http-nio-8761-exec-3] [INFO ] [c.b.p.m.l.aop.GenericControllerAspect] [TID: N/A] - UserServiceController.repay() called with arguments: borrowerId: [3], amount: [1] called via url: [http://172.24.137.33:8761/repay]
[2019-02-14 11:10:03.761] [http-nio-8761-exec-3] [INFO ] [monitor.sql] [TID: N/A] - autocommit(true)|readonly(false)|REPEATABLE_READ|1ms|statement|connection50127|SELECT id,name,available_balance,frozen_balance,created_at,updated_at FROM user WHERE id=3
[2019-02-14 11:10:03.806] [http-nio-8761-exec-3] [INFO ] [monitor.sql] [TID: N/A] - autocommit(true)|readonly(false)|REPEATABLE_READ|36ms|statement|connection50128|UPDATE user  SET available_balance=38.00  WHERE id=3
[2019-02-14 11:10:03.845] [http-nio-8761-exec-3] [INFO ] [c.b.p.m.l.aop.GenericControllerAspect] [TID: N/A] - UserServiceController.repay() took [127 ms] to complete
[2019-02-14 11:10:03.845] [http-nio-8761-exec-3] [INFO ] [c.b.p.m.l.aop.GenericControllerAspect] [TID: N/A] - UserServiceController.repay() returned: [38.00]
```

# 详细功能示例代码和测试用例

## 手动埋点

通过 Monitors.logEvent(String type, Object param) 埋点后会有两个结果，一是在grafana里可以看到该type的数量，二是在skywalking里可以看到param里的信息

```java
  Monitors.logEvent("starter order", orderModel);
```

## 自定义指标

### @Monitor

这个注解可以加载类或者方法上，效果是统计方法的执行次数、耗时并在Grafana里展示，还有在日志中打印出入参数与耗时。***建议采用此注解***

```java
@Monitor
@Slf4j
@Component
@RabbitListener(queues = "project")
public class ProjectServiceListener {
    @Autowired
    UserService remoteUserService;
    @Autowired
    ProjectService remoteProjectService;
    @Autowired
    InvestService remoteInvestService;

    static ObjectMapper objectMapper = new ObjectMapper();

    @RabbitHandler
    public void handleProjectLendpay(@Payload Project project, @Header(value="XID",required = false) String xid) throws Exception {

        Thread.sleep(1000);
        SpringCloudDtsContext.getContext().setAttachment("XID", xid);
        if (project.getStatus() == ProjectStatusEnum.INVEST_FINISH) {
            remoteInvestService.getOrders(project.getId(), InvestStatusEnum.PAID)
                    .forEach(invest -> {
                        try {
                            remoteUserService.lendpayMoney(invest.getInvestorId(), invest.getBorrowerId(), invest.getAmount());
                            remoteInvestService.lendpay(invest.getId());
                        } catch (Exception ex) {
                            try {
                                log.error("处理放款的时候遇到异常：" + objectMapper.writeValueAsString(invest), ex);
                            } catch (JsonProcessingException e) {

                            }
                        }
                    });
            remoteProjectService.lendpay(project.getId());
        }
    }
}
```

打印日志
```text
[2019-02-22 15:06:43.817] [SimpleAsyncTaskExecutor-1] [INFO ] [c.b.p.m.l.aop.GenericControllerAspect] [TID: N/A] - ProjectServiceListener.handleProjectLendpay() called with arguments: project: [Project(id=1, projectId=WYB001, totalAmount=1000000, remainAmount=999992, name=投资项目001年化10%, reason=借款人用于生活, borrowerId=3, borrowerName=借款人1, status=OPEN, createdAt=Tue Oct 09 17:57:58 CST 2018)], xid: [null] called via url: [null]
[2019-02-22 15:06:44.825] [SimpleAsyncTaskExecutor-1] [INFO ] [c.b.p.m.l.aop.GenericControllerAspect] [TID: N/A] - ProjectServiceListener.handleProjectLendpay() took [1007 ms] to complete
[2019-02-22 15:06:44.831] [SimpleAsyncTaskExecutor-1] [INFO ] [c.b.p.m.l.aop.GenericControllerAspect] [TID: N/A] - ProjectServiceListener.handleProjectLendpay() returned: [null]
```

### timer
最简单的方式时在方法上加@Timed注解，比如下面的代码表示统计 allPeople 这个方法的执行次数以及执行时间。`method.timed`时这个指标的默认key值，建议不要修改，这个指标会展示在Grafana上。

```java
@Timed("method.timed")
public List<String> allPeople() {
    try {
        Thread.sleep(200);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    return people;
}
```

如果要单独统计某段代码而不是一个方法，那么可以用下面这种写法，不用指定percentiles，默认系统会添加p80,p90,p95和p99。

```java
long start=MicrometerUtil.monotonicTime();
Exception ex=null;
try{
    // 业务代码
}catch (Exception e){
    ex=e;
    throw ex;
}finally {
	Monitors.recordNanoSecondAfterStartTime("code.execute.timer",start,"method","trade","type","T01","exception",ex==null?"":ex.getClass().getSimpleName());
}
```

### counter
要单独统计某段代码执行的次数，应当使用counter。

```java

Exception ex=null;
try{
    // 业务代码
}catch (Exception e){
    ex=e;
    throw ex;
}finally {
    Monitors.count(counterName,"method","trade","type","T01","exception",ex==null?"":ex.getClass().getSimpleName());
}
```

### summary

summary 用来统计一组数据的分布情况，和timer的区别时，timer统计的时时间，单位只能是纳秒毫秒等时间单位，而summary统计的时数字，没有单位。可以认为timer是一种特殊的summary。

下面举个例子，我们要对交易做统计，需要知道统计每个支付的金额分布，并且要支持按交易渠道做筛选，那可以按下面这样写

```java
        double[] percentiles=new double[]{0.5,0.8,0.9};
        Monitors.summary("trade",10,percentiles,"channel","wechat");
        Monitors.summary("trade",20,percentiles,"channel","alipay");
```
第一个参数是summary的名字，第二个是支付金额，第三个percentiles表示可以统计50% 80% 90%的支付请求的金额，后面是变长参数，变长参数的数量必须是偶数，上面的例子意思是可以按channel统计，有两个channel分别是alipay和wechat


## Kafka日志

自定义一些日志需要送给Kafka进行流式计算，LoggerFactory.getLogger("BizLogger") 获取到的 Logger 所打印的日志会被发送到Kafka。

```java
 private static final Logger logger = LoggerFactory.getLogger("BizLogger");
 logger.info("****");
 
```


## SQL日志

默认提供了SQL的日志功能，详细记录了sql的执行时间、内容和autocommit等信息，如[这个例子-事务测试](https://confluence.bkjk-inc.com/pages/viewpage.action?pageId=25527342) 

默认打印所有sql，如果不想看到那么多，可以配置仅打印超过x毫秒的sql，添加启动参数 `-DMONITOR.SQL.EXECUTIONTHRESHOLD=10`（这里单位是毫秒）

# 更多功能

## 自动提供的指标有

1. CPU、内存、磁盘空间
2. JVM Memory、GC、thread、classes等
3. tomcat、HttpRequest
4. RestTemplate、Feign
5. Hystrix
6. 数据库连接池、SQL执行时间、慢SQL检测、事务开启和关闭
7. Spring的ThreadPoolTaskExecutor和ThreadPoolTaskScheduler
8. Redis
9. Kafaka、RabbitMQ
10. Log events
11. etc.


## 自动添加的TAG有

1. application.name 代表spring.application.name
2. application.group 代表spring.application.group
3. application.version 代表spring.application.version
4. application.profiles.active 代表spring.profiles.active
5. zone 代表Eureka的zone
6. ip 代表应用所在机器或者容器的IP

## 指标重写

框架默认对所有的timer指标做了重写，增加了p80,p90,p95,p99的指标。

比如`sql_execute_time`这个指标会自动生成另一个名为`sql_execute_time_percentile`的包含更多聚合统计的指标。