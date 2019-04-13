package com.bkjk.platform.webapi.test;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bkjk.platform.webapi.ApiController;
import com.bkjk.platform.webapi.result.NoApiResult;
import com.bkjk.platform.webapi.version.ApiVersion;

@ApiController
@ApiVersion("")
@RequestMapping("result")
public class ResultController {

    @GetMapping("test1")
    public boolean test1() {
        return false;
    }

    @GetMapping("test2")
    public float test2() {
        return .2f;
    }

    @GetMapping("test3")
    public MyItem test3() {
        return new MyItem("aa", 10);
    }

    @GetMapping("test4")
    public void test4() {
    }

    @GetMapping("test5")
    @NoApiResult
    public MyItem test5() {
        return new MyItem("aa", 10);
    }

}
