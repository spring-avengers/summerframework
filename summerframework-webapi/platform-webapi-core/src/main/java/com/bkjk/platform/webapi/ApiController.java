package com.bkjk.platform.webapi;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Controller
@RestController
@ResponseBody
public @interface ApiController {
    @AliasFor(annotation = Controller.class)
    String value() default "";

    /**
     * 如果是一个标准 restful接口 ，即 HttpMethod 符合规范且 Controller 下方法参数是在 body 中以JSON格式传递，那么设置为true。
     * 如果使用queryString或者RequestParam传递 POJO 参数，这里设置为false。
     * 
     * @return
     */
    boolean requestBody() default true;
}
