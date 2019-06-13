
package com.bkjk.platform.rabbit;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.ClassUtils;

import com.bkjk.platform.rabbit.delay.DelayQueueScaner;
import com.bkjk.platform.rabbit.extend.RabbitTemplateExtend;
import com.bkjk.platform.rabbit.logger.AbstractTraceLog;
import com.bkjk.platform.rabbit.logger.DatabaseMySQLTraceLogger;
import com.bkjk.platform.rabbit.logger.NoopTraceLogger;
import com.bkjk.platform.rabbit.logger.Slf4jTraceLogger;
import com.bkjk.platform.rabbit.utils.LogbackUtil;
import com.rabbitmq.client.Channel;

import ch.qos.logback.classic.Level;

@Configuration
@ConditionalOnClass({RabbitTemplate.class, Channel.class})
@EnableConfigurationProperties(RabbitProperties.class)
@Import(RabbitAnnotationDrivenConfiguration.class)
public class RabbitAutoConfiguration {

    @Configuration
    @ConditionalOnClass(name = "ch.qos.logback.classic.LoggerContext")
    protected static class LogbackCreator {

        @PostConstruct
        public void init() {
            LogbackUtil.getLogger("traceLog", Level.INFO, "rabbit");
        }
    }

    @Configuration
    @ConditionalOnClass(RabbitMessagingTemplate.class)
    @ConditionalOnMissingBean(RabbitMessagingTemplate.class)
    @Import(RabbitTemplateConfiguration.class)
    protected static class MessagingTemplateConfiguration {

        @Bean
        @ConditionalOnSingleCandidate(RabbitTemplate.class)
        public RabbitMessagingTemplate rabbitMessagingTemplate(RabbitTemplate rabbitTemplate) {
            return new RabbitMessagingTemplate(rabbitTemplate);
        }

    }

    @Configuration
    @ConditionalOnMissingBean(ConnectionFactory.class)
    protected static class RabbitConnectionFactoryCreator {

        @Bean
        public CachingConnectionFactory rabbitConnectionFactory(RabbitProperties config) throws Exception {
            RabbitConnectionFactoryBean factory = new RabbitConnectionFactoryBean();
            if (config.determineHost() != null) {
                factory.setHost(config.determineHost());
            }
            factory.setPort(config.determinePort());
            if (config.determineUsername() != null) {
                factory.setUsername(config.determineUsername());
            }
            if (config.determinePassword() != null) {
                factory.setPassword(config.determinePassword());
            }
            if (config.determineVirtualHost() != null) {
                factory.setVirtualHost(config.determineVirtualHost());
            }
            if (config.getRequestedHeartbeat() != null) {
                factory.setRequestedHeartbeat(config.getRequestedHeartbeat());
            }
            RabbitProperties.Ssl ssl = config.getSsl();
            if (ssl.isEnabled()) {
                factory.setUseSSL(true);
                if (ssl.getAlgorithm() != null) {
                    factory.setSslAlgorithm(ssl.getAlgorithm());
                }
                factory.setKeyStore(ssl.getKeyStore());
                factory.setKeyStorePassphrase(ssl.getKeyStorePassword());
                factory.setTrustStore(ssl.getTrustStore());
                factory.setTrustStorePassphrase(ssl.getTrustStorePassword());
            }
            if (config.getConnectionTimeout() != null) {
                factory.setConnectionTimeout(config.getConnectionTimeout());
            }
            factory.afterPropertiesSet();
            CachingConnectionFactory connectionFactory = new CachingConnectionFactory(factory.getObject());
            connectionFactory.setAddresses(config.determineAddresses());
            connectionFactory.setPublisherConfirms(config.isPublisherConfirms());
            connectionFactory.setPublisherReturns(config.isPublisherReturns());
            if (config.getCache().getChannel().getSize() != null) {
                connectionFactory.setChannelCacheSize(config.getCache().getChannel().getSize());
            }
            if (config.getCache().getConnection().getMode() != null) {
                connectionFactory.setCacheMode(config.getCache().getConnection().getMode());
            }
            if (config.getCache().getConnection().getSize() != null) {
                connectionFactory.setConnectionCacheSize(config.getCache().getConnection().getSize());
            }
            if (config.getCache().getChannel().getCheckoutTimeout() != null) {
                connectionFactory.setChannelCheckoutTimeout(config.getCache().getChannel().getCheckoutTimeout());
            }
            return connectionFactory;
        }

    }

    @Configuration
    @Import(RabbitConnectionFactoryCreator.class)
    protected static class RabbitTemplateConfiguration {

        private final ObjectProvider<MessageConverter> messageConverter;

        private final RabbitProperties properties;

        public RabbitTemplateConfiguration(ObjectProvider<MessageConverter> messageConverter,
            RabbitProperties properties) {
            this.messageConverter = messageConverter;
            this.properties = properties;
        }

        @Bean
        @ConditionalOnSingleCandidate(ConnectionFactory.class)
        @ConditionalOnProperty(prefix = "spring.rabbitmq", name = "dynamic", matchIfMissing = true)
        @ConditionalOnMissingBean(AmqpAdmin.class)
        public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory, RabbitTemplate rabbitTemplate,
            SimpleRabbitListenerContainerFactoryConfigurer listenerContainerCOnfigurer) {
            AmqpAdmin amqpAdmin = new RabbitAdmin(connectionFactory);
            if (rabbitTemplate instanceof RabbitTemplateExtend) {
                RabbitTemplateExtend templateExtend = (RabbitTemplateExtend)rabbitTemplate;
                templateExtend.setRabbitAdmin(amqpAdmin);
            }
            listenerContainerCOnfigurer.setRetryAmqpAdmin(amqpAdmin);
            listenerContainerCOnfigurer.setRetryRabbitTemplate(rabbitTemplate);
            return amqpAdmin;
        }

        private RetryTemplate createRetryTemplate(RabbitProperties.Retry properties) {
            RetryTemplate template = new RetryTemplate();
            SimpleRetryPolicy policy = new SimpleRetryPolicy();
            policy.setMaxAttempts(properties.getMaxAttempts());
            template.setRetryPolicy(policy);
            ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
            backOffPolicy.setInitialInterval(properties.getInitialInterval());
            backOffPolicy.setMultiplier(properties.getMultiplier());
            backOffPolicy.setMaxInterval(properties.getMaxInterval());
            template.setBackOffPolicy(backOffPolicy);
            return template;
        }

        @Bean
        public DelayQueueScaner DelayQueueScaner(RabbitTemplate rabbitTemplate) {
            return new DelayQueueScaner(rabbitTemplate);
        }

        private boolean determineMandatoryFlag() {
            Boolean mandatory = this.properties.getTemplate().getMandatory();
            return (mandatory != null ? mandatory : this.properties.isPublisherReturns());
        }

        @Bean
        @ConditionalOnSingleCandidate(ConnectionFactory.class)
        @ConditionalOnMissingBean(RabbitTemplate.class)
        public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
            RabbitTemplate rabbitTemplate = new RabbitTemplateExtend(connectionFactory);
            MessageConverter messageConverter = this.messageConverter.getIfUnique();
            if (messageConverter != null) {
                rabbitTemplate.setMessageConverter(messageConverter);
            }
            rabbitTemplate.setMandatory(determineMandatoryFlag());
            RabbitProperties.Template templateProperties = this.properties.getTemplate();
            RabbitProperties.Retry retryProperties = templateProperties.getRetry();
            if (retryProperties.isEnabled()) {
                rabbitTemplate.setRetryTemplate(createRetryTemplate(retryProperties));
            }
            if (templateProperties.getReceiveTimeout() != null) {
                rabbitTemplate.setReceiveTimeout(templateProperties.getReceiveTimeout());
            }
            if (templateProperties.getReplyTimeout() != null) {
                rabbitTemplate.setReplyTimeout(templateProperties.getReplyTimeout());
            }
            return rabbitTemplate;
        }

    }

    public static Environment appEnv;
    public static final String RABBIT_TRACE_LOG_TYPE_KEY = "rabbit.trace.log-type";
    public static final String RABBIT_TRACE_LOG_TYPE_FILE = "file";

    public static final String RABBIT_TRACE_LOG_TYPE_MYSQL = "mysql";

    public static final String RABBIT_TRACE_LOG_TYPE_NONE = "none";

    @Autowired
    private Environment env;

    @Autowired(required = false)
    private DataSource dataSource;

    @Value("${rabbit.trace.queue.capacity:2000}")
    private int capacity;

    @PostConstruct
    public void init() {
        appEnv = env;
        if (env.containsProperty(RABBIT_TRACE_LOG_TYPE_KEY)) {
            String type = env.getProperty(RABBIT_TRACE_LOG_TYPE_KEY);
            if (type.equals(RABBIT_TRACE_LOG_TYPE_FILE)) {
                AbstractTraceLog.setTraceLogger(Slf4jTraceLogger.instance);
            } else if (type.equals(RABBIT_TRACE_LOG_TYPE_MYSQL)) {
                AbstractTraceLog.setTraceLogger(new DatabaseMySQLTraceLogger(dataSource, capacity));
            } else if (type.equals(RABBIT_TRACE_LOG_TYPE_NONE)) {
                AbstractTraceLog.setTraceLogger(NoopTraceLogger.instance);
            }
        } else {
            if (dataSource != null
                && ClassUtils.isPresent("com.mysql.jdbc.Driver", RabbitAutoConfiguration.class.getClassLoader())) {
                AbstractTraceLog.setTraceLogger(new DatabaseMySQLTraceLogger(dataSource, capacity));
            } else {
                AbstractTraceLog.setTraceLogger(Slf4jTraceLogger.instance);
            }
        }
    }

}
