# 说明

* configcenter是针对Apollo的增强组件

|  | 原生客户端 | 二次封装客户端  |
|------ | ------ | ------ |
|apollo.bootstrap.enabled  | 需要配置| 不需要配置 |
|app.id  | 需要配置 | 不需要配置，和spring.application.name统一|
|env  | 需要配置 | 不需要配置，和spring.profiles.active统一 |
|spring.profiles.active  | 不需要配置 | 不需要配置 |


* 将环境的的区分与Spring Boot的标准环境区分变量统一起来
* 减少了环境变量传入的个数，从五个减少到了两个
* 统一AppId
* 不会对原生的Apollo Client使用有任何的影响

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

2. 引入 configcenter 模块
```xml
		<dependency>
			<groupId>com.bkjk.platform.summerframework</groupId>
			<artifactId>platform-starter-configcenter</artifactId>
		</dependency>
```