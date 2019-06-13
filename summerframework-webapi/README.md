platform-starter-webapi完成了以下小功能，旨在方便大家基于SpringMVC开发微服务（为了不破坏Spring MVC的功能，所有这些功能的基础是使用了@ApiController注解，不使用这个注解所有行为和Spring MVC一致）：

- 方法级的“拦截器”，我们叫做@ApiFilter。对于Spring MVC我们可以通过定义过滤器或拦截器来做一些通用的事情，但是不够直观，框架提供了注解方式使得我们可以在方法或Controller类级声明过滤器。
- 强制指定版本号。通过在方法或Controller类级强制设置版本号@ApiVersion来规范API的版本。
- 入参如果是POJO的话，默认就是Json序列化传参，自带验证，无需声明@Valid以及@RequestBody。
- 方法返回参数直接是POJO，无需自己包装返回类型，框架会统一包装进行规范（也支持简单类型的返回），可以通过@NoApiResult来取消包装。
- 框架统一全局处理各种异常，通过ApiException来规范异常。
- 自动集成SwaggerUI，无需配置（生产环境也就是spring profile是PROD的时候自动禁用）