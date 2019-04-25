package com.bkjk.platform.webapi.test;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bkjk.platform.webapi.exception.ApiException;
import com.bkjk.platform.webapi.version.ApiVersion;

import io.swagger.annotations.ApiOperation;

@ApiVersion("")
@RequestMapping("exception")
public class ExceptionController {
    @GetMapping("item/{id}")
    @ApiOperation("测试异常")
    public MyItem getItem(@PathVariable("id") String id) {
        Integer i = null;
        try {
            i = Integer.parseInt(id);
        } catch (NumberFormatException ex) {
        }
        if (i == null)
            throw new ApiException("1001", "商品ID只能是数字");
        if (i < 1)
            throw new RuntimeException("不合法的商品ID");

        return new MyItem("item" + id, 10);
    }

    @PostMapping("item")
    @ApiOperation("测试POJO参数校验异常")
    public MyItem setItem(MyItem item) {
        return item;
    }
}
