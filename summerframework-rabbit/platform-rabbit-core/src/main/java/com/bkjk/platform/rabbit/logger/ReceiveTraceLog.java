
package com.bkjk.platform.rabbit.logger;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;

import java.util.Arrays;

public class ReceiveTraceLog extends AbstractTraceLog {

    private final SimpleMessageListenerContainer listenerContainer;

    private final Channel channel;

    private final Message message;

    public ReceiveTraceLog(final SimpleMessageListenerContainer listenerContainer, final Channel channel,
        final Message message) {
        this.listenerContainer = listenerContainer;
        this.channel = channel;
        this.message = message;
    }

    @Override
    public MessageTraceBean createMessageTraceBean() {
        ConnectionFactory connectionFactory = listenerContainer.getConnectionFactory();
        MessageTraceBean trace = super.buildTraceBean(message);
        String messageId = (String)message.getMessageProperties().getHeaders().get(MESSAGE_HEAD_MESSAGEID);
        String exchange = (String)message.getMessageProperties().getHeaders().get(MESSAGE_HEAD_EXCHANGE);
        String routingKey = (String)message.getMessageProperties().getHeaders().get(MESSAGE_HEAD_ROUTEINGKEY);
        trace.setMessageId(messageId);
        trace.setType("consumer");
        trace.setChannel(Integer.valueOf(channel.getChannelNumber()).toString());
        trace.setNode("rabbit#" + connectionFactory.getUsername());
        trace.setConnection(connectionFactory.getHost() + ":" + connectionFactory.getPort());
        trace.setVhost(connectionFactory.getVirtualHost());
        trace.setUser(connectionFactory.getUsername());
        trace.setExchange(exchange);
        trace.setQueue(Arrays.toString(listenerContainer.getQueueNames()));
        trace.setRoutingKeys(routingKey);
        trace.setProperties(JsonUtil.toJsonString(message.getMessageProperties()));
        return trace;
    }

}
