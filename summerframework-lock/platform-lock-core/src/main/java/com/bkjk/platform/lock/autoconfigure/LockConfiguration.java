package com.bkjk.platform.lock.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/5/6 10:01
 **/
@Configuration
@ConfigurationProperties(prefix = LockConfiguration.PREFIX)
@Data
public class LockConfiguration {
    public static final String PREFIX="platform.lock";

    private String prefix="p.lock.";

    private long expireTimeMillis=30_000;
}
