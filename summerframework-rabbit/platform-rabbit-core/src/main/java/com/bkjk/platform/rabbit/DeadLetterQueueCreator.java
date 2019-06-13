package com.bkjk.platform.rabbit;

import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;

import com.google.common.collect.Maps;

public class DeadLetterQueueCreator {

    private static final Logger logger = LoggerFactory.getLogger(DeadLetterQueueCreator.class);

    private AmqpAdmin rabbitAdmin;

    public DeadLetterQueueCreator(AmqpAdmin rabbitAdmin) {
        this.rabbitAdmin = rabbitAdmin;
    }

    public void createDeadLetterQueue(String fromExchange, String byRouteKey, String delayOrRetryRouteKey,
        String sourceQueue, String delayOrRetryQueueName, Long ttl) {
        if (sourceQueue == null || sourceQueue.isEmpty()) {
            logger.warn(
                "Have not config destination Queue, will not create delay queue by automatic，may be you must maintain binding by youself");
            return;
        }
        Properties properties = rabbitAdmin.getQueueProperties(delayOrRetryQueueName);
        if (properties == null) {
            Map<String, Object> delayQueueArgs = Maps.newHashMap();
            delayQueueArgs.put("x-message-ttl", ttl);
            delayQueueArgs.put("x-dead-letter-exchange", fromExchange);
            delayQueueArgs.put("x-dead-letter-routing-key", byRouteKey);
            Queue delayQueue = new Queue(delayOrRetryQueueName, true, false, false, delayQueueArgs);
            String returnQueueName = rabbitAdmin.declareQueue(delayQueue);
            if (returnQueueName != null) {
                Binding binding = BindingBuilder.bind(delayQueue)//
                    .to(new DirectExchange(DeadLetterConstant.DEFAULT_DEADLETTEREXCHANGE_NAME))//
                    .with(delayOrRetryRouteKey);//
                rabbitAdmin.declareBinding(binding);
            }
        }
    }

}
