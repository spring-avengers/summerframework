package com.bkjk.platform.gray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;

import com.netflix.loadbalancer.Server;

public class GroovyGrayScriptEngine implements GrayScriptEngine {

    public static final Logger logger = LoggerFactory.getLogger(GroovyGrayScriptEngine.class);
    @Autowired
    EurekaRegistration registration;

    @Autowired
    private GrayRulesStore grayRulesStore;

    @Override
    public List<Map<String, String>> execute(List<Server> servers, Map<String, Object> context) {
        List<Map<String, String>> list = new ArrayList<>();
        Map<String, String> node = new HashMap<>();
        String toServerName = servers.get(0).getMetaInfo().getAppName();
        String fromServerName = registration.getApplicationInfoManager().getEurekaInstanceConfig().getAppname();
        List<String> groovyScript = grayRulesStore.findGroovyScript(fromServerName, toServerName);
        groovyScript.forEach(s -> {
            try {
                Map<String, String> nodeInfo = (Map)GroovyScriptEngineUtil.executeChoose(s, context);
                list.add(nodeInfo);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });
        return list;
    }
}
