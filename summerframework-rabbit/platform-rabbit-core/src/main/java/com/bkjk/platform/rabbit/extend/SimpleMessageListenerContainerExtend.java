
package com.bkjk.platform.rabbit.extend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;

import com.bkjk.platform.rabbit.logger.ReceiveTraceLog;
import com.rabbitmq.client.Channel;

public class SimpleMessageListenerContainerExtend extends SimpleMessageListenerContainer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleMessageListenerContainerExtend.class);

    @Override
    protected void invokeListener(Channel channel, Message message) throws Exception {
        try {
            super.invokeListener(channel, message);
        } finally {
            try {
                ReceiveTraceLog traceLog = new ReceiveTraceLog(this, channel, message);
                traceLog.log();
            } catch (Throwable e) {
                LOGGER.warn(e.getMessage(), e);
            }
        }
    }
}
