
package com.bkjk.platform.rabbit.extend;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;

import com.bkjk.platform.rabbit.DeadLetterConstant;
import com.bkjk.platform.rabbit.DeadLetterQueueCreator;
import com.bkjk.platform.rabbit.logger.PublishTraceLog;
import com.google.common.collect.Maps;
import com.rabbitmq.client.Channel;

public class RabbitTemplateExtend extends RabbitTemplate {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitTemplateExtend.class);

    private static final ThreadLocal<Map<String, Long>> DELAY_QUEUE_CONTENT = new ThreadLocal<Map<String, Long>>() {
        @Override
        protected synchronized Map<String, Long> initialValue() {
            return Maps.newHashMap();
        }
    };

    private DeadLetterQueueCreator deadLetterQueueCreator;

    public RabbitTemplateExtend() {
        super();
    }

    public RabbitTemplateExtend(ConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    @Override
    public void doSend(Channel channel, String exchange, String routingKey, Message message, boolean mandatory,
        CorrelationData correlationData) throws Exception {

        try {
            PublishTraceLog traceLog =
                new PublishTraceLog(this, channel, exchange, routingKey, message, mandatory, correlationData);
            traceLog.log();
        } catch (Throwable e) {
            LOGGER.warn(e.getMessage(), e);
        }
        try {
            Map<String, Long> delayParam = DELAY_QUEUE_CONTENT.get();
            if (delayParam != null && delayParam.size() == 1) {
                String sourceQueue = delayParam.keySet().iterator().next();
                Long interval = delayParam.get(sourceQueue);
                if (interval > 0) {
                    String delayQueue = sourceQueue + DeadLetterConstant.DEFAULT_DELAY_QUEUENAME_PREFIX;
                    String delayRouteKey = routingKey + DeadLetterConstant.DEFAULT_DELAY_QUEUENAME_PREFIX;
                    deadLetterQueueCreator.createDeadLetterQueue(exchange, routingKey, delayRouteKey, sourceQueue,
                        delayQueue, interval);
                    String delayExchange = DeadLetterConstant.DEFAULT_DEADLETTEREXCHANGE_NAME;
                    super.doSend(channel, delayExchange, delayRouteKey, message, mandatory, correlationData);
                    return;
                }
            }
            super.doSend(channel, exchange, routingKey, message, mandatory, correlationData);
        } finally {
            DELAY_QUEUE_CONTENT.remove();
        }

    }

    public void setDelayQueue(String queueName, Long interval) {
        Map<String, Long> delayQueueParam = Maps.newHashMap();
        delayQueueParam.put(queueName, interval);
        DELAY_QUEUE_CONTENT.set(delayQueueParam);
    }

    public void setRabbitAdmin(AmqpAdmin amqpAdmin) {
        this.deadLetterQueueCreator = new DeadLetterQueueCreator(amqpAdmin);
    }

}
