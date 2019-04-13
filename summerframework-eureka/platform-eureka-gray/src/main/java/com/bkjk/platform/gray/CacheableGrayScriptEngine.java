package com.bkjk.platform.gray;

import java.util.List;
import java.util.Map;

import com.netflix.loadbalancer.Server;

public class CacheableGrayScriptEngine implements GrayScriptEngine {

    private GrayScriptEngine delegate;

    public CacheableGrayScriptEngine(GrayScriptEngine delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<Map<String, String>> execute(List<Server> servers, Map<String, Object> context) {
        return delegate.execute(servers, context);
    }
}
