package com.bkjk.platform.webapi.test;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bkjk.platform.webapi.ApiController;
import com.bkjk.platform.webapi.filter.ApiFilter;
import com.bkjk.platform.webapi.filter.ApiFilterExclude;
import com.bkjk.platform.webapi.version.ApiVersion;

import io.swagger.annotations.ApiOperation;

@ApiController
@ApiVersion("")
@RequestMapping("filter")
@ApiFilter(LoginCheck.class)
@ApiFilter(TestApiFilter3.class)
public class FilterController {

    @ApiFilter(value = TestApiFilter1.class, order = 100)
    @ApiFilter(TestApiFilter2.class)
    @GetMapping("item1")
    @ApiOperation("默认按照顺序排序，order越大越靠后")
    public MyItem getItem1() {
        return new MyItem("aa", 10);
    }

    @ApiFilter(TestApiFilter1.class)
    @ApiFilter(TestApiFilter2.class)
    @GetMapping("item2")
    @ApiOperation("类上的过滤器优先执行")
    public MyItem getItem2() {
        return new MyItem("aa", 10);
    }

    @ApiFilter(TestApiFilter1.class)
    @ApiFilter(TestApiFilter2.class)
    @ApiFilterExclude(TestApiFilter3.class)
    @ApiFilterExclude(TestApiFilter2.class)
    @ApiFilterExclude(TestApiFilter1.class)
    @GetMapping("item3")
    @ApiOperation("多次Exclude")
    public MyItem getItem3() {
        return new MyItem("aa", 10);
    }

    @ApiFilterExclude(LoginCheck.class)
    @ApiFilterExclude(LoginCheck.class)
    @GetMapping("health")
    public String health() {
        return "OK";
    }

    @GetMapping("hello")
    public String hello() {
        return "OK";
    }
}
