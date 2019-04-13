package com.bkjk.platform.gray;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public abstract class AbstractGrayHeaderCustomizer<R> implements GrayHeaderCustomizer<R> {
    public static final Logger logger = LoggerFactory.getLogger(AbstractGrayHeaderCustomizer.class);

    EurekaRegistration registration;

    private GrayRulesStore grayRulesStore;

    public AbstractGrayHeaderCustomizer(EurekaRegistration registration, GrayRulesStore grayRulesStore) {
        this.registration = registration;
        this.grayRulesStore = grayRulesStore;
    }

    protected abstract void addHeaderToRequest(R request, String key, String value);

    @Override
    public void apply(R request) {
        try {
            ServletRequestAttributes requestAttributes =
                (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
            List<String> headerNames =
                grayRulesStore.findHeader(registration.getApplicationInfoManager().getInfo().getAppName());
            if (headerNames != null) {
                headerNames.forEach(h -> {
                    String headerKey = h.toLowerCase();
                    String headerValue = requestAttributes.getRequest().getHeader(h);
                    if (!containsKey(request, headerKey)) {
                        addHeaderToRequest(request, headerKey, headerValue);
                    }
                });
            }
        } catch (Throwable ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    protected abstract boolean containsKey(R request, String key);

}
