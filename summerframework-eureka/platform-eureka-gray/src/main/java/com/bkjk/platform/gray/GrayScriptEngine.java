package com.bkjk.platform.gray;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.netflix.loadbalancer.Server;

public interface GrayScriptEngine {

    default List<Map<String, String>> execute(List<Server> servers, Map<String, Object> context) {
        return new ArrayList<>();
    }
}
