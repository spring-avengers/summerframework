package com.bkjk.platform.redis;

import java.lang.reflect.Proxy;
import java.util.List;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SentinelServersConfig;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.bkjk.platform.redis.data.DataRedisDistributedLock;
import com.bkjk.platform.redis.redisson.RedissonClientProxyHandler;
import com.bkjk.platform.redis.redisson.RedissonDistributedLock;

@Configuration
@EnableConfigurationProperties(RedisProperties.class)
public class RedisAutoConfiguration {

    @Value("${spring.redis.cacheCloudUrl:}")
    private String cacheCloudUrl;
    @Autowired
    private RedisProperties redisProperties;

    @Autowired
    private Environment env;

    @ConditionalOnClass(name = {"org.springframework.data.redis.core.RedisTemplate"})
    protected class DataRedisConfig {

        @Bean
        @ConditionalOnClass(name = {"org.springframework.data.redis.core.StringRedisTemplate"})
        @ConditionalOnMissingBean(DistributedLock.class)
        public DistributedLock distributedLock(StringRedisTemplate redisTemplate) {
            return new DataRedisDistributedLock(redisTemplate, env);
        }
    }

    @ConditionalOnClass(name = {"org.redisson.api.RedissonClient"})
    protected class RedissonConfig {
        @Bean
        @ConditionalOnMissingBean(RedissonClient.class)
        public RedissonClient redissonClient() {
            Config config = new Config();
            // sentinel
            if (redisProperties.getSentinel() != null) {
                SentinelServersConfig sentinelServersConfig = config.useSentinelServers();
                sentinelServersConfig.setMasterName(redisProperties.getSentinel().getMaster());
                List<String> nodes = redisProperties.getSentinel().getNodes();
                String[] sentinelAddress = new String[nodes.size()];
                nodes.toArray(sentinelAddress);
                sentinelServersConfig.addSentinelAddress(sentinelAddress);
                sentinelServersConfig.setDatabase(redisProperties.getDatabase());
                if (redisProperties.getPassword() != null) {
                    sentinelServersConfig.setPassword(redisProperties.getPassword());
                }
            }
            // cluster
            else if (redisProperties.getCluster() != null) {
                ClusterServersConfig clusterServersConfig = config.useClusterServers();
                List<String> nodes = redisProperties.getSentinel().getNodes();
                String[] clusterAddress = new String[nodes.size()];
                for (int i = 0; i < nodes.size(); i++) {
                    String node = nodes.get(i);
                    if (!node.startsWith("redis://")) {
                        clusterAddress[i] = "redis://" + nodes.get(i);
                    }
                }
                clusterServersConfig.addNodeAddress(clusterAddress);
                if (redisProperties.getPassword() != null) {
                    clusterServersConfig.setPassword(redisProperties.getPassword());
                }
            } else { // single server
                SingleServerConfig singleServerConfig = config.useSingleServer();
                // format as redis://127.0.0.1:7181 or rediss://127.0.0.1:7181 for SSL
                String schema = redisProperties.isSsl() ? "rediss://" : "redis://";
                singleServerConfig.setAddress(schema + redisProperties.getHost() + ":" + redisProperties.getPort());
                singleServerConfig.setDatabase(redisProperties.getDatabase());
                if (redisProperties.getPassword() != null) {
                    singleServerConfig.setPassword(redisProperties.getPassword());
                }
            }
            RedissonClient sourceRedissonClient = Redisson.create(config);
            RedissonClientProxyHandler handler = new RedissonClientProxyHandler(sourceRedissonClient);
            Object proxy = Proxy.newProxyInstance(handler.getClass().getClassLoader(),
                new Class[] {RedissonClient.class}, handler);
            return (RedissonClient)proxy;
        }

        @Bean
        @ConditionalOnClass(name = {"org.redisson.api.RedissonClient"})
        @ConditionalOnMissingBean(DistributedLock.class)
        public DistributedLock distributedLock(RedissonClient redissonClient) {
            return new RedissonDistributedLock(redissonClient, env);
        }
    }

}
