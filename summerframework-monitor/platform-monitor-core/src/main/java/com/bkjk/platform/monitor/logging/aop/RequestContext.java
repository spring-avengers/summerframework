package com.bkjk.platform.monitor.logging.aop;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class RequestContext {
    private final Map<String, String> context = new LinkedHashMap<>(4);

    public RequestContext() {

    }

    public RequestContext(Map<String, String> context) {
        context.forEach((key, value) -> this.context.put(key, value != null ? value : "null"));
    }

    public RequestContext add(String key, String value) {
        context.put(key, value != null ? value : "null");
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        RequestContext that = (RequestContext)o;

        return context.equals(that.context);
    }

    @Override
    public int hashCode() {
        return context.hashCode();
    }

    @Override
    public String toString() {
        return context.entrySet().stream().map(e -> e.getKey() + ": [" + e.getValue() + "]")
            .collect(Collectors.joining(", "));
    }
}
