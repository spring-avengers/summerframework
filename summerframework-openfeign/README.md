# 说明


## 功能

* spring MVC 不支持继承接口中方法参数上的注解（支持继承类、方法上的注解）,summerframework做了支持
* 支持灰度发布(必须配合platform-starter-eureka使用，参考[灰度](https://confluence.bkjk-inc.com/pages/viewpage.action?pageId=19309838))

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

2. 引入 openfeign 模块
```xml
		<dependency>
			<groupId>com.bkjk.platform.summerframework</groupId>
			<artifactId>platform-starter-openfeign</artifactId>
		</dependency>
```
如果要使用灰度功能，则引入
```xml
		<dependency>
			<groupId>com.bkjk.platform.summerframework</groupId>
			<artifactId>platform-starter-openfeign-gray</artifactId>
		</dependency>

```
引入platform-starter-openfeign-gray后，除了feign之外，resttemplate、httpclient也可支持灰度，详见下面例子

## 演示代码

编写API接口，关于ApiVersion请参考 [platform-starter-webapi使用手册](https://confluence.bkjk-inc.com/pages/viewpage.action?pageId=23050857)
```java
@FeignClient("summerframework")
//@ApiVersion("v1")
public interface ProviderService {
    @PostMapping(value = "/hello")
    String hello(@RequestBody Pojo pojo);
    @GetMapping("/hi")
//    @ApiVersion("v2")
    String hi(Pojo pojo);
}
```

编写实现类，只需要再接口上加注解就可以，实现类上不用再写Mapping注解
```java
@RestController
//@ApiController
public class ProviderServiceImpl implements ProviderService {

    @Override
    public String hello(Pojo pojo) {
        try {
            return new ObjectMapper().writeValueAsString(pojo);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Override
    public String hi(Pojo pojo) {
        try {
            return new ObjectMapper().writeValueAsString(pojo);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}

```

客户端配置好后直接注入ProviderService即可使用
```java

@EnableFeignClients(basePackages = "com.bkjk.feign.sample.provider.service")
@SpringBootApplication
@EnableDiscoveryClient
@Configuration
public class ConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }

}
```


参考：[platform-openfeign-samples](https://code.bkjk-inc.com/projects/SOA/repos/summerframework2/browse/summerframework-samples/platform-openfeign-samples)