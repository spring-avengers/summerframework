package com.bkjk.platform.gray;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import com.bkjk.platform.eureka.util.JsonUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import groovy.util.logging.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.bkjk.platform.ribbon.ServerFilter;
import com.netflix.loadbalancer.Server;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;
import org.springframework.web.util.ContentCachingRequestWrapper;

@Slf4j
public class GrayServerFilter implements ServerFilter {

    public static final Logger logger = LoggerFactory.getLogger(GrayServerFilter.class);

    @Autowired
    @Qualifier("grayScriptEngine")
    private GrayScriptEngine grayScriptEngine;

    private Map convertRequest(HttpServletRequest request) {
        Map<String, Object> context = new HashMap<>();

        Enumeration<String> hs = request.getHeaderNames();
        Map<String, Object> header = new HashMap<>();
        while (hs.hasMoreElements()) {
            String key = hs.nextElement();
            header.put(key, request.getHeader(key));
        }
        context.put("header", header);

        Enumeration<String> pns = request.getParameterNames();
        Map<String, Object> parameter = new HashMap<>();
        while (pns.hasMoreElements()) {
            String key = pns.nextElement();
            parameter.put(key, request.getParameter(key));
        }
        context.put("parameter", parameter);

        try {
            if (!StringUtils.isEmpty(request.getContentType())
                && request.getContentType().toLowerCase().contains("application/json")
                && request instanceof ContentCachingRequestWrapper) {
                ContentCachingRequestWrapper req = (ContentCachingRequestWrapper)request;
                String json = new String(req.getContentAsByteArray(), Charset.forName("UTF-8"));
                if (JsonUtil.isGoodJson(json)) {
                    context.put("body", json);
                }
            }
        } catch (Throwable ignore) {
            logger.error(ignore.getMessage(), ignore);
        }
        if (!context.containsKey("body")) {
            context.put("body", JsonUtil.toJson(Maps.newHashMap()));
        }
        return context;
    }

    @Override
    public List<Server> match(List<Server> servers) {
        if (servers == null || servers.size() == 0) {
            return servers;
        }
        ServletRequestAttributes requestAttributes =
            (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            logger.debug("No servletRequestAttributes in current thread. Match all servers");
            return Lists.newArrayList(servers);
        }
        HttpServletRequest request = requestAttributes.getRequest();

        List<Map<String, String>> matchList = grayScriptEngine.execute(servers, convertRequest(request));
        List<Server> allServers = new ArrayList<>();
        allServers.addAll(servers);
        for (Map<String, String> m : matchList) {
            allServers = match(allServers, m);
        }
        if (allServers.size() == 0) {
            logger.info("No server found");
        }
        return allServers;
    }

    private List<Server> match(List<Server> servers, Map<String, String> matchMap) {
        if (servers == null || servers.size() == 0) {
            logger.info("No server to match");
            return servers;
        }
        Map<String, String> nodeInfo = new HashMap();
        nodeInfo.putAll(matchMap);
        String match = nodeInfo.remove("match");
        if (null == nodeInfo || nodeInfo.isEmpty()) {
            return servers;
        }
        List<Server> grayServerList = servers.stream().filter(server -> {
            if (server instanceof DiscoveryEnabledServer) {
                Map<String, String> meteData = ((DiscoveryEnabledServer)server).getInstanceInfo().getMetadata();
                for (String key : nodeInfo.keySet()) {

                    if (!StringUtils.isEmpty(nodeInfo.get(key))) {
                        if (!nodeInfo.get(key).equals(meteData.get(key))) {
                            logger.debug("Excluded server [{}]",
                                ((DiscoveryEnabledServer)server).getInstanceInfo().getHealthCheckUrl());
                            return false;
                        }
                    }
                }
                logger.debug("Matched server [{}]",
                    ((DiscoveryEnabledServer)server).getInstanceInfo().getHealthCheckUrl());
                return true;
            }
            logger.warn("Server {} is not instance of DiscoveryEnabledServer. Exclude it.", server.getHost());
            return false;
        }).collect(Collectors.toList());
        List<Server> nonGrayServerList =
            servers.stream().filter(server -> !grayServerList.contains(server)).collect(Collectors.toList());
        if ("true".equals(match)) {
            if (grayServerList.size() == 0) {
                logger.error("Gray rule match = {}. But no server found. NonGrayServerList = {}. GrayServerList = {}",
                    matchMap, nonGrayServerList, grayServerList);
            }
            return grayServerList;
        } else {
            if (nonGrayServerList.size() == 0) {
                logger.error("Gray rule match = {}. But no server found. NonGrayServerList = {}. GrayServerList = {}",
                    matchMap, nonGrayServerList, grayServerList);
            }
            return nonGrayServerList;
        }
    }

}
