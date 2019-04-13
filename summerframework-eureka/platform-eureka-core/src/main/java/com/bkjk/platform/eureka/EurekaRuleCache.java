
package com.bkjk.platform.eureka;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class EurekaRuleCache {

    private static class LazyHolder {
        private static final EurekaRuleCache INSTANCE = new EurekaRuleCache();
    }

    private static final LoadingCache<String, String> EUREKA_RULE_CACHE = CacheBuilder.newBuilder()
        .expireAfterWrite(1, TimeUnit.DAYS).maximumSize(100).build(new CacheLoader<String, String>() {

            @Override
            public String load(String key) throws Exception {
                return StringUtils.EMPTY;
            }
        });

    public static final EurekaRuleCache getInstance() {
        return LazyHolder.INSTANCE;
    }

    private EurekaRuleCache() {
    }

    public boolean clear(String serviceId) {
        EUREKA_RULE_CACHE.invalidate(serviceId);
        return Boolean.TRUE;
    }

    public String get(String serviceId) {
        try {
            return EUREKA_RULE_CACHE.get(serviceId);
        } catch (ExecutionException e) {
            return StringUtils.EMPTY;
        }
    }

    public boolean put(String serviceId, String rule) {
        EUREKA_RULE_CACHE.put(serviceId, rule);
        return Boolean.TRUE;
    }

}
