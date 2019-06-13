package com.bkjk.platform.elasticsearch;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties("es.transport")
public class Transport {

    @Value("${es.transport.ignoreClusterName:true}")
    private String ignoreClusterName;

    @Value("${es.transport.pingTimeout:5s}")
    private String pingTimeout;

    @Value("${es.transport.nodesSamplerInterval:5s}")
    private String nodesSamplerInterval;
}
