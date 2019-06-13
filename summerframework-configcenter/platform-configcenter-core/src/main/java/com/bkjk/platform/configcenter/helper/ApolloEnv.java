package com.bkjk.platform.configcenter.helper;

import java.io.IOException;
import java.util.Properties;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import com.ctrip.framework.apollo.core.utils.StringUtils;

public enum ApolloEnv {

    DEV("dev_meta"),

    TEST("test_meta"),

    STAGE("stage_meta"),

    PROD("prod_meta");

    private static final Properties ENV_PROPERTIES = new Properties();

    static {
        try {
            String fileName = "apollo.properties";
            Resource[] resources =
                new PathMatchingResourcePatternResolver().getResources("classpath*:META-INF/" + fileName);
            for (Resource resource : resources) {
                PropertiesLoaderUtils.fillProperties(ENV_PROPERTIES, resource);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ApolloEnv fromTypeName(String active) {
        if (StringUtils.isEmpty(active)) {
            return ApolloEnv.DEV;
        } else {
            for (ApolloEnv type : ApolloEnv.values()) {
                if (type.name().equals(active.toUpperCase())) {
                    return type;
                }
            }
            return null;
        }
    }

    private final String envMetaKey;

    ApolloEnv(String envMetaKey) {
        this.envMetaKey = envMetaKey;
    }

    public String getEnvMetaKey() {
        return envMetaKey;
    }

    public String getEnvMetaValue() {
        return ENV_PROPERTIES.getProperty(envMetaKey);
    }

}
