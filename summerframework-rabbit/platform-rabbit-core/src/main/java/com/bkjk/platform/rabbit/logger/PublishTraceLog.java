
package com.bkjk.platform.rabbit.logger;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;

import java.util.UUID;

public class PublishTraceLog extends AbstractTraceLog {

    private static String[] chars = new String[] {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n",
        "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
        "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V",
        "W", "X", "Y", "Z"};

    private final RabbitTemplate rabbitTemplate;
    private final Channel channel;
    private final String exchange;
    private final String routingKey;
    private final Message message;
    private final boolean mandatory;
    private final CorrelationData correlationData;

    public PublishTraceLog(final RabbitTemplate rabbitTemplate, final Channel channel, final String exchange,
        final String routingKey, final Message message, final boolean mandatory,
        final CorrelationData correlationData) {
        this.rabbitTemplate = rabbitTemplate;
        this.channel = channel;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.message = message;
        this.mandatory = mandatory;
        this.correlationData = correlationData;
    }

    @Override
    public MessageTraceBean createMessageTraceBean() {
        String messageId = generateShortUuid();
        message.getMessageProperties().setHeader(MESSAGE_HEAD_MESSAGEID, messageId);
        message.getMessageProperties().setHeader(MESSAGE_HEAD_EXCHANGE, rabbitTemplate.getExchange());
        message.getMessageProperties().setHeader(MESSAGE_HEAD_ROUTEINGKEY, rabbitTemplate.getRoutingKey());
        MessageTraceBean trace = super.buildTraceBean(message);
        ConnectionFactory connectionFactory = rabbitTemplate.getConnectionFactory();
        trace.setMessageId(messageId);
        trace.setType("publish");
        trace.setNode("rabbit#" + connectionFactory.getUsername());
        trace.setConnection(connectionFactory.getHost() + ":" + connectionFactory.getPort());
        trace.setVhost(connectionFactory.getVirtualHost());
        trace.setUser(connectionFactory.getUsername());
        trace.setChannel(Integer.valueOf(channel.getChannelNumber()).toString());
        trace.setExchange(exchange);
        trace.setQueue("none");
        trace.setRoutingKeys(routingKey);
        trace.setProperties(JsonUtil.toJsonString(message.getMessageProperties()));
        return trace;
    }

    private String generateShortUuid() {
        StringBuffer shortBuffer = new StringBuffer();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        for (int i = 0; i < 8; i++) {
            String str = uuid.substring(i * 4, i * 4 + 4);
            int x = Integer.parseInt(str, 16);
            shortBuffer.append(chars[x % 0x3E]);
        }
        return shortBuffer.toString();
    }

}
