# 说明

* Eureka starter是一个针对eureka服务注册发现的增强插件

## 功能

* 增加服务注册及发现的维度选择，而不仅仅依赖于服务ID来进行注册与发现；（这个功能是为了适应贝壳金控集团子公司及BU部门比较多的情况，有可能出现应用名重复的情况）
* 可以屏蔽某一些IP，不让流量进入这些IP，达到类似金丝雀部署的效果；
* 对ribbon的默认负载均衡机制进行增强，动态根据服务节点负载情况来路由。为每个节点打分（包括cpu和内存；cpu权重为0.7，内存权重为0.3），分值越高，被选中的概率越大。
* 消费者可以指定服务提供者的的group和version
* 灰度发布

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

2. 引入 eureka 模块
```xml
		<dependency>
			<groupId>com.bkjk.platform.summerframework</groupId>
			<artifactId>platform-starter-eureka</artifactId>
		</dependency>
```

## 演示代码

* 服务提供端

bootstrap.yml配置：

 ```
   spring:
      application:
         name: user-service
         group: bkjk     //group建议以部门作为一个分组
         version: 1.0.0  //version建议1.0.0,可以随意定制
 ```
添加Group和Version进行服务注册，这两个字段将会注册在Eureka的Meta-Data，其中Group建议为部门名，Version可以1.0.0-DEV等等

请使用下列group：

* 房产：housing
* 平台（Infra）： platform
* 信用： credit
* 租赁：cf
* 支付：payment
* 理财：wealth

* 服务消费端，如果希望消费指定的group和version，则启用如下配置。否则不需要做下面的配置

application.yml配置：

```
eureka:
  client:
    serviceUrl:
      defaultZone: http://eureka.dev.bkjk.cn/eureka/
    reference: META-INF/refercen.json
```

在META-INF目录下新建refercen.json，对于服务user-service只消费group：bkjk且version：1.0.0的服务，内容如下：
 
 ```
  [
	{
		"serviceId": "user-service",
		"group": "bkjk",
		"version": "1.0.0"
	}
]
 
 ```


# 详细功能示例代码和测试用例

## 默认配置

框架添加了如下默认配置，之所以添加这些，是因为曾经在我们的线上出现过未配置它们导致的问题

```yaml
eureka.instance.prefer-ip-address:true
ribbon.MaxAutoRetries: 0
ribbon.MaxAutoRetriesNextServer: 0
```

## 屏蔽一些IP

屏蔽某一些IP

1. 开启actuator并开启管理端口
2. 访问http://ip:managerPort/eurekamgmt/dynamicsroute?serviceId=**&routeIp=**，其中routeIp是需要屏蔽的IP列表，以","分割
3. 访问http://ip:managerPort/eurekamgmt/clearRoute？serviceId=**, 清除步骤2的规则


## 负载均衡机制

原理是：CPU和内存等可用资源越多，得分越高，节点被选中的概率也就越大

大多数情况，我们只考虑CPU就可以，因为得益于强大的GC，同一个JAVA应用的多个节点的内存使用率基本差不多，除非发生了内存泄漏。故在判断节点资源时，最好给CPU和内存不同的权重。

下面举个栗子来描述如何选择节点，这个例子中不考虑内存

cpuScore =(max( coreSize - load), 0)/coreSize

memScore  = (max(maxHeap - usedHeap), 0)/maxheap

以cpu为例（内存同理）

Node1：8核心，load等于2时得分(8 -2 )/8 = 0.75

Node2：8核心，load等于6时得分(8 -6) /8 = 0.25

Node3：8核心，load等于4时得分(8 -4) /8 = 0.5 

Node4：8核心，load等于10时得分(8 -10) /8 = 0

totalScore= 0.75+0.25+0.5+0 = 1.5

按CPU的分值将节点分散在[0-1]之间

R1 = 0.75/1.5=0.5

R2 = 0.25/1.5 = 0.1667 。0.1667+ 0.5 = 0.6667 

R3 = 0.5/1.5= 0.3333 。0.3333+0.6667 = 1.0

R4 = 1+0 = 1

结果是：

[0,0.5) 选Node1

[0.5,0.6667) 选Node2

[0.6667,1) 选Node3

[1,1)选Node4

 取一个0-1之间的随机值，这个值分布到哪个区间，就选择对应那个区间的Node。

rollScore = Random(0,1)

## 灰度发布支持

2.0之后的版本支持灰度发布，并会自动在metadata中写入
```json
{"gray.enable": "true"}
```