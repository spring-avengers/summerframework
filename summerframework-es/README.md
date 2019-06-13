# 说明

因为Spring-data目前还不支持Elasticsearch 5.x.x版本，所以需要通过另外的starter项目来支持es5

ElasticsearchTemplate是自定义的模板类，可通过注入来使用es5的javaAPI

-----
亦加入支持es6，由于es5和6部分api不兼容，无法一起编译，故拆分es5和es6为两个工程，源码百分之九十九是一致得。
ElasticsearchTemplate中的公共法方签名完全一致，使用起来没有差异

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

2. 引入 mybatis 模块

使用ES5

```xml
		<dependency>
			<groupId>com.bkjk.platform.summerframework</groupId>
			<artifactId>platform-starter-es5</artifactId>
		</dependency>
```

使用ES6

```xml
		<dependency>
			<groupId>com.bkjk.platform.summerframework</groupId>
			<artifactId>platform-starter-es6</artifactId>
		</dependency>
```

## 演示代码

1. 在application.properties文件中加入

```
    es.host=58.87.119.113
    es.port=9300
    es.httpPort=9222
    es.clusterName=elasticsearch-test
    es.sniff=false
    #设置 true ，忽略连接节点集群名验证
    es.transport.ignoreClusterName=true
    #ping一个节点的响应时间 默认5秒
    es.transport.pingTimeout=5s
    #sample/ping 节点的时间间隔，默认是5s
    es.transport.nodesSamplerInterval=5s
```

其中最后三行可填可不填

2. 具体使用方式可见test目录下代码和配置文件

参考 [platform-es5-core/src/test/](https://code.bkjk-inc.com/projects/SOA/repos/summerframework2/browse/summerframework-project/summerframework-es/platform-es5-core/src/test/java/com/bkjk/platform/elasticsearch/ESSearchTest.java)
