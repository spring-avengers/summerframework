package com.bkjk.platform.elasticsearch;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties("es")
public class ElasticSearchProperties {
    private String host;
    private String port;
    private String httpPort;
    private String clusterName;

    @Value("${es.sniff:true}")
    private String sniff;
}
