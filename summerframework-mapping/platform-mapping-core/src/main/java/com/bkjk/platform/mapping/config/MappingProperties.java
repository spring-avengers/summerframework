package com.bkjk.platform.mapping.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties("mapping")
public class MappingProperties {

    private boolean useBuiltinConverters = true;

    private boolean useAutoMapping = true;

    private boolean mapNulls = true;

    private boolean dumpStateOnException = true;

    private boolean favorExtension = true;

    private boolean captureFieldContext = true;
}
