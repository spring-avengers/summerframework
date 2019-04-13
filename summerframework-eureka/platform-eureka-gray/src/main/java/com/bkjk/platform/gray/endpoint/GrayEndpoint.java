package com.bkjk.platform.gray.endpoint;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.bkjk.platform.gray.GrayRulesStore;

@ConfigurationProperties(prefix = "endpoints.gray")
@RestControllerEndpoint(id = "gray")
public class GrayEndpoint {

    @Autowired
    private GrayRulesStore grayRulesStore;

    public GrayEndpoint() {

    }

    @GetMapping("rules")
    @ResponseBody
    public Object getRules() {
        return grayRulesStore.getStore();
    }

    @GetMapping("rules/hash")
    @ResponseBody
    public Object getRulesHash() {
        Map<String, String> ret = new HashMap<>();
        ret.put(GrayRulesStore.KEY_HASH, grayRulesStore.getRulesHash());
        return ret;
    }

    @PutMapping(path = "rules", consumes = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @ResponseBody
    public Object updateRule(@RequestBody Map body) {
        grayRulesStore.setRules(body);
        return null;
    }

}
