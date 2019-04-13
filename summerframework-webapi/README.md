# 说明
platform-starter-webapi完成了以下小功能，旨在方便大家基于SpringMVC开发微服务（为了不破坏Spring MVC的功能，所有这些功能的基础是使用了@ApiController注解，不使用这个注解所有行为和Spring MVC一致）：

1. 方法级的“拦截器”，我们叫做@ApiFilter。对于Spring MVC我们可以通过定义过滤器或拦截器来做一些通用的事情，但是不够直观，框架提供了注解方式使得我们可以在方法或Controller类级声明过滤器。
2. 强制指定版本号。通过在方法或Controller类级强制设置版本号@ApiVersion来规范API的版本。
3. 入参如果是POJO的话，默认就是Json序列化传参，自带验证，无需声明@Valid以及@RequestBody。某些历史应用并不是用JSON格式传递参数的，那么需要用@ApiController(requestBody = false)来告诉框架不要自动做RequestBody的映射
4. 方法返回参数直接是POJO，无需自己包装返回类型，框架会统一包装进行规范（也支持简单类型的返回），可以通过@NoApiResult来取消包装。
5. 框架统一全局处理各种异常，通过ApiException来规范异常。
6. 自动集成SwaggerUI，无需配置（生产环境也就是spring profile是PROD的时候自动禁用）
7. 可自定义HttpMessageConverter,并设置为优先级最高


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
         <artifactId>platform-starter-webapi</artifactId>
      </dependency>
```

# 演示代码

```java
@ApiController
@ApiVersion("v1")
public class ExampleController {
    private static List<User> userList = new ArrayList<>();

    @GetMapping("health")
    @ApiVersion("")
    @NoApiResult
    public String health() {
        return "OK";
    }

    @PostMapping("user")
    @ApiFilter(LoginCheck.class)
    @ApiOperation("注册")
    public User register(User user) {
        if (user.getName().equalsIgnoreCase("admin"))
            throw new ApiException("1234", "非法的用户名");
        userList.add(user);
        return user;
    }

    @GetMapping("users")
    @ApiFilter(RemoveSensitiveInfoFilter.class)
    @ApiVersion({"v2", "v3"})
    @ApiOperation("新版本查询用户")
    public List<User> getUsersNew() {
        return userList;
    }

    @GetMapping("users")
    @ApiOperation("老版本查询用户")
    public List<User> getUsers() {
        return userList;
    }
}
```

可以看到代码极其简洁，实现的过滤器因为定义在方法或类上也很直观：

1. Controller上需要定义@ApiController所有功能才会生效并且必须通过@ApiVersion定义版本号，如果不想改变现有行为，可以为空字符串
2. 使用ApiException来抛异常
3. Swagger的各种注解直接有效，比如@ApiOperation
4. 请使用@ApiFilter并且继承AbstractApiFilter来自定义过滤器（过滤器需要标记@Component）

POJO代码如下：
```java
@Data
public class User {
    @NotBlank
    @Size(min = 2, max = 10)
    private String name;
    @NotNull
    @Min(1)
    @Max(100)
    private Integer age;
    @Size(min = 18, max = 18)
    private String idCard;
}
```

RemoveSensitiveInfoFilter过滤器代码如下：

```java
@Component
public class RemoveSensitiveInfoFilter extends AbstractApiFilter {
    @Override
    protected String getDescription() {
        return "屏蔽敏感信息";
    }

    @Override
    protected Object beforeReturn(HttpServletRequest request, HttpServletResponse response, Method method, Object object) {
        if (object instanceof List) {
            if (((List) object).size() > 0 && (((List) object).get(0) instanceof User)) {
                ((List<User>) object).forEach(user -> user.setIdCard(""));
            }
        }
        return object;
    }
}

```

LoginCheck过滤器代码如下：

```java
@Component
public class LoginCheck extends AbstractApiFilter {

    @Override
    protected String getDescription() {
        return "校验Token";
    }

    @Override
    public boolean preAction(HttpServletRequest request, HttpServletResponse response, Method method) {
        if (request.getHeader("token") == null || !request.getHeader("token").equals("1"))
            throw new RuntimeException("请登录！");
        return true;
    }
}
```

AbstractApiFilter定义了三个切入点，实现自定义的过滤器只需要扩展这个抽象类，给一个描述，然后（部分）实现需要的方法即可：

preAction：方法执行前，这个时候可以做一些权限、缓存控制

beforeReturn：生成了返回数据后，方法返回前，这个时候有机会修改返回的数据

postAction：方法执行完成后，做一些后处理，比如统计执行时间



# 其它功能

## API包装结构的扩展点
如果业务应用的Result与上面的ApiResult不一样，那么可以实现ApiResultTransformer接口（注册为Spring bean）来自定义包装result的逻辑，该接口需实现两个方法

1. 包装正常返回值
2. 包装异常返回值

以上两者的结构必须一致，且返回值必须实现ApiResultWrapper接口。

其实我们的框架也是通过这种方式实现的，框架默认提供的实现如下

```java
public class DefaultApiResultTransformer implements ApiResultTransformer {

    @Override
    public ApiResultWrapper changeBody(Object body, MethodParameter returnType, MediaType selectedContentType, Class selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        return ApiResult.builder().time(System.currentTimeMillis()).success(true)
                .code(String.valueOf(HttpStatus.OK.value())).data(body).error("").message(HttpStatus.OK.getReasonPhrase())
                .path(request.getURI().getPath()).build();
    }

    @Override
    public  ApiResultWrapper<Object> exceptionToResult(HttpServletRequest request, Exception ex) {
        if(ex instanceof MethodArgumentNotValidException){
            return handleMethodArgumentNotValidException(request, (MethodArgumentNotValidException) ex);
        } if(ex instanceof ApiException){
            return handleApiException(request, (ApiException) ex);
        }
        else {
            return handleException(request,ex);
        }
    }
}



@Data
@Builder
public class ApiResult<T> implements ApiResultWrapper<T> {
    boolean success;
    String code;
    String error;
    String message;
    String path;
    long time;
    T data;

    @Override
    @JsonIgnore
    public String getSchemaVersion() {
        return VERSION_SUMMER2;
    }

}
```

下面再举一个自定义格式的例子

如果你的返回值是

```json
{
  "myCode":"0000",
  "myMessage":"",
  "result":{"name":"Bob"}
}
```

那么可以用用下面的方式扩展框架的接口。（***ApiResultWrapper接口一定要实现，尤其是isSuccess，因为框架会根据这个进行处理***）

```java
@Data
@Builder
public class MyApiResult implements ApiResultWrapper<Object> {
    public static final String SUCCESS="0000";
    public static final String FAIL="1111";

    private String myCode;
    private String myMessage;
    private Object result;


    @Override
    @JsonIgnore
    public boolean isSuccess() {
        return SUCCESS.equalsIgnoreCase(getMyCode());
    }

    @Override
    @JsonIgnore
    public String getCode() {
        return getMyCode();
    }

    @Override
    @JsonIgnore
    public String getError() {
        return getMyCode();
    }

    @Override
    @JsonIgnore
    public String getMessage() {
        return getMyMessage();
    }

    @Override
    @JsonIgnore
    public String getPath() {
        return "";
    }

    @Override
    @JsonIgnore
    public Object getData() {
        return getResult();
    }
}


@Component
public class MyApiResultTransformer implements ApiResultTransformer<MyApiResult> {
    @Override
    public MyApiResult changeBody(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        return MyApiResult.builder().result(body).myCode(SUCCESS).build();
    }

    @Override
    public MyApiResult exceptionToResult(HttpServletRequest request, Exception ex) {
        return  MyApiResult.builder().myCode(FAIL).myMessage(ex.getLocalizedMessage()).build();
    }

    @Override
    public Class<? extends ApiResultWrapper> getType() {
        return MyApiResult.class;
    }
}
```

## 自定义HttpMessageConverter

```java
/**
* 继承AbstractHttpMessageConverter接口来实现AbstractHttpMessageConverter
*/
public class MyMessageConverter extends AbstractHttpMessageConverter<DemoObj> {
 
    public MyMessageConverter() {
        //新建一个我们自定义的媒体类型application/x-wisely
        super(new MediaType("application", "x-wisely",Charset.forName("UTF-8")));
    }
    
    /**
     * 重写readInternal方法，处理请求的数据。代表我们处理由"-"隔开的数据，并转成DemoObj对象
     */
 
    @Override
    protected DemoObj readInternal(Class<? extends DemoObj> clazz,
            HttpInputMessage inputMessage) throws IOException,
            HttpMessageNotReadableException {
        String temp = StreamUtils.copyToString(inputMessage.getBody(),
 
        Charset.forName("UTF-8"));
        String[] tempArr = temp.split("-");
        return new DemoObj(new Long(tempArr[0]), tempArr[1]);
    }
    
    /**
     * 表明HttpMessageConvert只处理DemoObj这个类
     */
    @Override
    protected boolean supports(Class<?> clazz) {
        return DemoObj.class.isAssignableFrom(clazz);
    }
    
    /**
     * 重写writeInternal，处理如何输出数据到response。此例中，在原样输出签名加上“hello:”
     */
    @Override
    protected void writeInternal(DemoObj obj, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        String out = "hello:" + obj.getId() + "-"
                + obj.getName();
        outputMessage.getBody().write(out.getBytes());
    }
 
}
```