# SummerFramework

SummerFramework是在Spring Cloud的基础上一些扩展



# Features

* 用携程[Apollo](https://github.com/ctripcorp/apollo)替换原生的配置中心，把Apollo的app.id,env等环境参数与Spring Boot参数统一

* 对服务注册、发现进行扩展，多维度服务发现，面向接口声明式调用，自动负载，A/B测试

* 对微服务下Metrcis打点，将micrometer和skywalking结合在一起，把日志-调用链-metrcis进行有效结合

* 对Web Api进行统一规范，规范化返回结果，异常，api版本





# Compile

1: 首先构建summerframework-build
 
https://github.com/ke-finance/summerframework-build

```
 mvn clean install
 
```

2: 构建summerframework

```
./install-all.sh

```


# Quick Start




 

# License

This software is free to use under the Apache License [Apache license](https://github.com/alibaba/DataX/blob/master/license.txt).



