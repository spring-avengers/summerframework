package com.bkjk.platform.webapi.test;

import com.bkjk.platform.webapi.result.ApiResult;
import com.bkjk.platform.webapi.util.ApiResultUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/3/7 16:14
 **/
@SpringBootTest
public class ApiResultTest {
    @Test
    public void testApiResult() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ApiResult<Object> result = ApiResult.builder()
                .success(true)
                .code("100")
                .error("err")
//                .message("msg")
                .path("path")
                .data("dt")
                .build();
        System.out.println(mapper.writeValueAsString(result));
        System.out.println(ApiResultUtil.formatAsJSONWithoutData(result));
        System.out.println(mapper.readValue(ApiResultUtil.formatAsJSONWithoutData(result),ApiResult.class));
        Assert.assertEquals(result.getCode(),mapper.readValue(ApiResultUtil.formatAsJSONWithoutData(result),ApiResult.class).getCode());
        Assert.assertEquals(null,mapper.readValue(ApiResultUtil.formatAsJSONWithoutData(result),ApiResult.class).getData());
    }
}
