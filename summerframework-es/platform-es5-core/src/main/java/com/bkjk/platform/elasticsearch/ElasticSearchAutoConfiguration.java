package com.bkjk.platform.elasticsearch;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.annotation.Resource;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bkjk.platform.elasticsearch.support.ElasticsearchTemplate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ConditionalOnClass({TransportClient.class, InetSocketTransportAddress.class})
@EnableConfigurationProperties({ElasticSearchProperties.class, Transport.class})
public class ElasticSearchAutoConfiguration {
    @Resource
    private ElasticSearchProperties elasticSearchProperties;
    @Resource
    private Transport transport;

    @Bean
    public TransportClient esClient() {
        TransportClient esClient = null;
        try {
            Settings settings = Settings.builder().put("cluster.name", elasticSearchProperties.getClusterName())
                .put("client.transport.sniff", Boolean.valueOf(elasticSearchProperties.getSniff()))
                .put("client.transport.ignore_cluster_name", Boolean.valueOf(transport.getIgnoreClusterName()))
                .put("client.transport.ping_timeout", transport.getPingTimeout())
                .put("client.transport.nodes_sampler_interval", transport.getNodesSamplerInterval()).build();
            log.info("\nhost is:{},port is:{},httpPort is:{},cluster name is:{}\n", elasticSearchProperties.getHost(),
                elasticSearchProperties.getPort(), elasticSearchProperties.getHttpPort(),
                elasticSearchProperties.getClusterName());
            esClient = new PreBuiltTransportClient(settings).addTransportAddress(
                new InetSocketTransportAddress(InetAddress.getByName(elasticSearchProperties.getHost()),
                    Integer.valueOf(elasticSearchProperties.getPort())));
        } catch (UnknownHostException e) {
            log.error("Can not connect to elasticsearch: {}", ExceptionUtils.getStackTrace(e));
        }
        return esClient;
    }

    @Bean
    public ElasticsearchTemplate esTemplate() {
        return new ElasticsearchTemplate();
    }
}
