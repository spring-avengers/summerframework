package com.bkjk.platform.webapi.test;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bkjk.platform.webapi.ApiController;
import com.bkjk.platform.webapi.version.ApiVersion;

import io.swagger.annotations.ApiOperation;

@ApiController
@RequestMapping("misc")
@ApiVersion("")
public class MiscController {
    @PostMapping("item")
    @ApiOperation("用来测试自动的RequestBody和Validation")
    public MyItem setItem(MyItem myItem) {
        return myItem;
    }
}
