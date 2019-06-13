package com.bkjk.platfrom.dts.core;

import com.bkjk.platform.dts.remoting.RemoteConstant;
import com.bkjk.platform.eureka.event.EurekaClientLocalCacheRefreshedEvent;
import com.bkjk.platfrom.dts.core.interceptor.DtsRemoteInterceptor;
import com.bkjk.platfrom.dts.core.resource.mysql.DataSourceAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.context.*;
import org.springframework.core.env.Environment;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;

@SuppressWarnings("unchecked")
public class SpringContextHolder
    implements DisposableBean, ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static final Logger log = LoggerFactory.getLogger(SpringContextHolder.class);

    private static ApplicationContext applicationContext = null;

    private static volatile Boolean eurekaLocalCacheRefreshed = false;

    private static Long rpcInvokeTimeout = null;

    public static void clearHolder() {
        applicationContext = null;
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static <T> T getBean(Class<T> requiredType) {
        try {
            return applicationContext.getBean(requiredType);
        } catch (NoSuchBeanDefinitionException exception) {
            log.debug("not found bean in spring cotext", exception);
            return null;
        }

    }

    public static <T> T getBean(String name) {
        return (T)applicationContext.getBean(name);
    }

    public static Boolean isEurekaLocalCacheRefreshed() {
        return eurekaLocalCacheRefreshed;
    }

    public static long getRpcInvokeTimeout() {
        if (Objects.isNull(rpcInvokeTimeout)) {
            rpcInvokeTimeout = Long.valueOf(applicationContext.getEnvironment().getProperty("dts.rpcInvokeTimeout",
                String.valueOf(RemoteConstant.RPC_INVOKE_TIMEOUT)));
        }
        return rpcInvokeTimeout;
    }

    @Override
    public void destroy() throws Exception {
        SpringContextHolder.clearHolder();
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        applicationContext.getBeanFactory().addBeanPostProcessor(new InstantiationAwareBeanPostProcessorAdapter() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                Environment enviroment = applicationContext.getEnvironment();
                String appName = enviroment.getProperty("spring.application.name");
                if (bean instanceof DataSource) {
                    DataSource datasource = (DataSource)bean;
                    try {
                        DataSourceAdapter dataSourceAdapter = new DataSourceAdapter(appName, datasource);
                        return dataSourceAdapter;
                    } catch (SQLException e) {
                        throw new BeanInitializationException("cant not adapter datasource", e);
                    }
                }
                if (bean instanceof RestTemplate) {
                    RestTemplate restTemplate = (RestTemplate)bean;
                    ArrayList<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
                    interceptors.add(new DtsRemoteInterceptor());
                    interceptors.addAll(restTemplate.getInterceptors());
                    restTemplate.setInterceptors(interceptors);
                    return restTemplate;
                }
                return bean;
            }
        });
        applicationContext.addApplicationListener(new ApplicationListener<ApplicationEvent>() {

            @Override
            public void onApplicationEvent(ApplicationEvent event) {
                if (event instanceof EurekaClientLocalCacheRefreshedEvent) {
                    eurekaLocalCacheRefreshed = true;
                }
            }

        });
        SpringContextHolder.applicationContext = applicationContext;
    }

}
