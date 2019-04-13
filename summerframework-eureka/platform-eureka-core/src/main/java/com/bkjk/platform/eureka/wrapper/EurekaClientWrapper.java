
package com.bkjk.platform.eureka.wrapper;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.ConfigurableEnvironment;

import com.bkjk.platform.common.Constants;
import com.bkjk.platform.eureka.EurekaRuleCache;
import com.bkjk.platform.eureka.event.EurekaClientEventListener;
import com.bkjk.platform.eureka.util.JsonUtil;
import com.google.common.collect.Lists;
import com.netflix.appinfo.InstanceInfo;

public class EurekaClientWrapper extends CloudEurekaClient {

    private final List<Map<String, String>> SERVICE_REFERENCE_CACHE = Lists.newArrayList();

    private final EurekaRuleCache eurekaRuleCache;

    private final ConfigurableEnvironment environment;

    private volatile String reference;

    public EurekaClientWrapper(CloudEurekaClient cloudEurekaClient, ConfigurableEnvironment environment,
        ApplicationEventPublisher publisher) {
        super(cloudEurekaClient.getApplicationInfoManager(), cloudEurekaClient.getEurekaClientConfig(), publisher);
        this.environment = environment;
        this.eurekaRuleCache = EurekaRuleCache.getInstance();
        this.initReferenceCache();
        this.registerEventListener(new EurekaClientEventListener(publisher));
    }

    private List<InstanceInfo> filterInstance(List<InstanceInfo> instanceList) {
        List<InstanceInfo> filteredInsantce = Lists.newArrayList();
        List<InstanceInfo> toDeleteInstance = Lists.newArrayList();
        for (InstanceInfo insaceInfo : instanceList) {
            final String serviceId = insaceInfo.getVIPAddress();
            final String instanceIpAddr = insaceInfo.getIPAddr();
            final Map<String, String> instanceMeta = insaceInfo.getMetadata();
            String reference = environment.getProperty(Constants.CONSUMER_INSTANCE_REFERENCE);
            if (this.reference != null && reference != null) {
                if (!this.reference.equals(reference)) {
                    this.initReferenceCache();
                    this.reference = reference;
                }
            }
            Pair<String, String> groupVersion = this.getGroupVersion(serviceId);
            if (groupVersion != null) {
                String group = instanceMeta.get(Constants.EUREKA_METADATA_GROUP);
                String version = instanceMeta.get(Constants.EUREKA_METADATA_VERSION);
                if (group != null && version != null) {
                    boolean groupEqual = StringUtils.equals(groupVersion.getLeft(), group);
                    boolean versionEqual = StringUtils.equals(groupVersion.getRight(), version);
                    if (groupEqual && versionEqual) {
                        filteredInsantce.add(insaceInfo);
                    }
                } else {
                    filteredInsantce.add(insaceInfo);
                }
            } else {
                filteredInsantce.add(insaceInfo);
            }
            String ipAddress = eurekaRuleCache.get(serviceId);
            if (StringUtils.isNotEmpty(ipAddress)) {
                List<String> ipAddressList = Arrays.asList(StringUtils.split(ipAddress, ","));
                if (ipAddressList.contains(instanceIpAddr)) {
                    toDeleteInstance.add(insaceInfo);
                }
            }
        }
        if (toDeleteInstance.size() != 0) {
            @SuppressWarnings("unchecked")
            Collection<InstanceInfo> remainList = CollectionUtils.removeAll(filteredInsantce, toDeleteInstance);
            return Lists.newArrayList(remainList);
        } else {
            return filteredInsantce;
        }
    }

    private Pair<String, String> getGroupVersion(String serviceId) {
        for (Map<String, String> referenceDefintion : SERVICE_REFERENCE_CACHE) {
            String service = referenceDefintion.get("serviceId");
            if (service.toUpperCase().equals(serviceId.toUpperCase())) {
                String group = referenceDefintion.get("group");
                String version = referenceDefintion.get("version");
                return new ImmutablePair<String, String>(group, version);
            }
        }
        return null;
    }

    @Override
    public List<InstanceInfo> getInstancesByVipAddress(String vipAddress, boolean secure) {
        List<InstanceInfo> instanceInfos = super.getInstancesByVipAddress(vipAddress, secure);
        return this.filterInstance(instanceInfos);
    }

    @Override
    public List<InstanceInfo> getInstancesByVipAddress(String vipAddress, boolean secure, String region) {
        List<InstanceInfo> instanceInfos = super.getInstancesByVipAddress(vipAddress, secure, region);
        return this.filterInstance(instanceInfos);
    }

    @Override
    public List<InstanceInfo> getInstancesByVipAddressAndAppName(String vipAddress, String appName, boolean secure) {
        List<InstanceInfo> instanceInfos = super.getInstancesByVipAddressAndAppName(vipAddress, appName, secure);
        return this.filterInstance(instanceInfos);
    }

    private void initReferenceCache() {
        String reference = environment.getProperty(Constants.CONSUMER_INSTANCE_REFERENCE);
        if (reference != null) {
            List<Map<String, String>> referencList = Lists.newArrayList();
            if (JsonUtil.isGoodJson(reference)) {
                referencList = JsonUtil.toList(new StringReader(reference));
            } else {
                InputStream in = EurekaClientWrapper.class.getClassLoader().getResourceAsStream(reference);
                if (in != null)
                    referencList = JsonUtil.toList(new InputStreamReader(in));
            }
            SERVICE_REFERENCE_CACHE.clear();
            SERVICE_REFERENCE_CACHE.addAll(referencList);
            this.reference = reference;
        }
    }

}
