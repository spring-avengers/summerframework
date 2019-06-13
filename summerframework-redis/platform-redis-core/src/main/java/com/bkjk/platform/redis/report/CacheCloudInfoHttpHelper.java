package com.bkjk.platform.redis.report;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sohu.tv.cachecloud.client.basic.heartbeat.HeartbeatInfo;
import com.sohu.tv.cachecloud.client.basic.util.HttpUtils;

public class CacheCloudInfoHttpHelper {

    private static Logger LOGGER = LoggerFactory.getLogger(CacheCloudInfoHttpHelper.class);

    public static String CLIENT_VERSION = "1.0";
    public static String CACHECLOUD_REPORT_URL;
    public static String REDIS_CLUSTER_SUFFIX = "/cache/client/redis/cluster/%s.json?clientVersion=";
    public static String REDIS_SENTINEL_SUFFIX = "/cache/client/redis/sentinel/%s.json?clientVersion=";
    public static String REDIS_STANDALONE_SUFFIX = "/cache/client/redis/standalone/%s.json?clientVersion=";
    public static String CACHECLOUD_REPORT_URL_PATH = "/cachecloud/client/reportData.json";
    private static final Lock LOCK = new ReentrantLock();

    private static List<String> generateClusterNodes(Long appId, String hostUrl) {
        try {
            LOCK.tryLock(10, TimeUnit.SECONDS);
            String cachecloudUrl = hostUrl + REDIS_CLUSTER_SUFFIX + CLIENT_VERSION;
            CACHECLOUD_REPORT_URL = hostUrl + CACHECLOUD_REPORT_URL_PATH;
            String url = String.format(cachecloudUrl, String.valueOf(appId));
            String response = HttpUtils.doGet(url);
            ObjectMapper objectMapper = new ObjectMapper();
            HeartbeatInfo heartbeatInfo = objectMapper.readValue(response, HeartbeatInfo.class);
            String nodeInfo = heartbeatInfo.getShardInfo();
            nodeInfo = nodeInfo.replace(" ", ",");
            List<String> nodeList = Arrays.asList(nodeInfo.split(","));
            ClientDataCollectReportExecutor.getInstance();
            return nodeList;
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            LOCK.unlock();
        }
    }

    public static RedisSentinelConfiguration generateSentinelConfiguration(Long appId, String hostUrl) {
        try {
            LOCK.tryLock(10, TimeUnit.SECONDS);
            String cachecloudUrl = hostUrl + REDIS_SENTINEL_SUFFIX + CLIENT_VERSION;
            CACHECLOUD_REPORT_URL = hostUrl + CACHECLOUD_REPORT_URL_PATH;
            String url = String.format(cachecloudUrl, String.valueOf(appId));
            String response = HttpUtils.doGet(String.format(url, appId));
            if (response == null || StringUtils.isEmpty(response)) {
                LOGGER.warn("get response from remote server error, appId: {}, continue...", appId);
                throw new RuntimeException();
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode heartbeatInfo = mapper.readTree(response);
            String masterName = heartbeatInfo.get("masterName").asText();
            String sentinels = heartbeatInfo.get("sentinels").asText();
            Set<RedisNode> sentinelSet = new HashSet<RedisNode>();
            for (String sentinelStr : sentinels.split(" ")) {
                String[] sentinelArr = sentinelStr.split(":");
                if (sentinelArr.length == 2) {
                    sentinelSet.add(new RedisNode(sentinelArr[0], Integer.valueOf(sentinelArr[1])));
                }
            }
            RedisSentinelConfiguration config = new RedisSentinelConfiguration();
            config.master(masterName);
            config.setSentinels(sentinelSet);
            ClientDataCollectReportExecutor.getInstance();
            return config;
        } catch (Throwable e) {
            LOGGER.error("error in build, appId: {}", appId, e);
            throw new RuntimeException(e);
        } finally {
            LOCK.unlock();
        }
    }

    public static List<String> getClusterNodeFromCacheCloud(Long appId, String hostUrl) {
        int i = 0;
        while (true) {
            if (i >= 5) {
                throw new RuntimeException("Can not get info form: " + hostUrl + ". for appId: " + appId + " .");
            }
            try {
                TimeUnit.MILLISECONDS.sleep(500 + new Random().nextInt(1000));
                return generateClusterNodes(appId, hostUrl);
            } catch (Exception e) {
                LOGGER.error("error in build, appId: {}", appId, e);
            }
            i++;
        }
    }

    public static RedisSentinelConfiguration getSentinelConfigurationFromCacheCloud(Long appId, String hostUrl) {
        RedisSentinelConfiguration configuration = null;
        int i = 0;
        while (true) {
            if (i >= 5) {
                throw new RuntimeException("Can not get info form: " + hostUrl + ". for appId: " + appId + " .");
            }
            try {
                configuration = generateSentinelConfiguration(appId, hostUrl);
                TimeUnit.MILLISECONDS.sleep(500 + new Random().nextInt(1000));
                return configuration;
            } catch (Exception e) {
                LOGGER.error("error in build, appId: {}", appId, e);
            }
            i++;
        }
    }
}
