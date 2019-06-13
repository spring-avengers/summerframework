# 说明

platform-jobcenter是Xxl job的客户端封装。依赖 Eureka 模块

## 功能

* 零配置启动定时Job任务
* 与调度中心的发现通过eureka来实现，让调度中心无中心化扩展

# 使用方式

参考 [platform-jobcenter-sample](https://code.bkjk-inc.com/projects/SOA/repos/summerframework2/browse/summerframework-samples/platform-jobcenter-sample)

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

2. 引入 jobcenter 模块
```xml
		<dependency>
			<groupId>com.bkjk.platform.summerframework</groupId>
			<artifactId>platform-starter-jobcenter</artifactId>
		</dependency>
```

## 演示代码

### 配置
1. 需配置eureka地址（如果项目之前已接入eureka则忽略本条）
```yaml
eureka:
  client:
    serviceUrl:
      defaultZone: http://eureka.dev.bkjk.cn/eureka/
```

k8s下默认通过`http://soa-eureka/eureka/`来连接eureka

2. 开发及调试时，如果有多个ip（比如连接了vpn或者装了虚拟机），需要指定本地ip。k8s环境下无需配置
```sh
-Dplatform.jobcenter.local-ip=10.241.0.199
```

### 编写JobHandler

```java
@JobHandler(value="demoJobHandler")
@Component
public class DemoJobHandler  extends IJobHandler {

    @Override
    public ReturnT<String> execute(String param) throws Exception {
        XxlJobLogger.log("XXL-JOB, Hello World.");

        for (int i = 0; i < 5; i++) {
            XxlJobLogger.log("beat at:" + i);
            TimeUnit.SECONDS.sleep(2);
        }
        return SUCCESS;
    }

}

```

### 配置 jobcenter 服务端

[dev环境点这里](http://jobcenter-ops.dev.bkjk.cn/jobinfo)

添加【执行器管理】，其中AppName必须和spring.application.name保持一致，注册方式选择自动注册，这样框架会自动从eureka中找到jobcenter并注册节点。

更多功能请查看[分布式任务调度平台xxl-job](http://www.xuxueli.com/xxl-job/#/?id=%E3%80%8A%E5%88%86%E5%B8%83%E5%BC%8F%E4%BB%BB%E5%8A%A1%E8%B0%83%E5%BA%A6%E5%B9%B3%E5%8F%B0xxl-job%E3%80%8B)