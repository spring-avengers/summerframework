
package com.bkjk.platform.rabbit.logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;

import com.bkjk.platform.rabbit.RabbitAutoConfiguration;
import com.google.common.net.InetAddresses;

public abstract class AbstractTraceLog {

    private static class LocalHost {
        static final String ADDR = getLocalhost();

        private static String getLocalhost() {
            InetAddress localInetAddress = null;
            try {
                localInetAddress = InetAddress.getLocalHost();
            } catch (UnknownHostException e) {

            }

            if (localInetAddress == null) {
                return "127.0.0.1";
            }

            return InetAddresses.toAddrString(localInetAddress);
        }

    }

    protected static final String MESSAGE_HEAD_MESSAGEID = "MessageLogTraceId";
    protected static final String MESSAGE_HEAD_EXCHANGE = "MessageLogExchange";

    protected static final String MESSAGE_HEAD_ROUTEINGKEY = "MessageLogRouteingKey";

    private static TraceLogger traceLogger = NoopTraceLogger.instance;

    public static final Logger logger = LoggerFactory.getLogger(AbstractTraceLog.class);

    public static TraceLogger getTraceLogger() {
        return traceLogger;
    }

    public static void main(String[] args) {
        MessageTraceBean bean = new MessageTraceBean();
        bean.setApplicationName("myapp");
        bean.setChannel("hfhaf");
        System.out.println(bean.toString());
    }

    public static void setTraceLogger(TraceLogger traceLogger) {
        AbstractTraceLog.traceLogger = traceLogger;
    }

    protected MessageTraceBean buildTraceBean(Message message) {
        MessageTraceBean trace = new MessageTraceBean();
        trace.setTimestamp(now());
        trace.setClientIp(LocalHost.ADDR);
        trace.setApplicationName(RabbitAutoConfiguration.appEnv.getProperty("spring.application.name", "none"));
        trace.setPayload(new String(message.getBody()));
        trace.setSuccess("0");
        return trace;
    }

    public abstract MessageTraceBean createMessageTraceBean();

    public final void log() {
        try {
            getTraceLogger().log(createMessageTraceBean());
        } catch (Throwable ignore) {
            logger.error(ignore.getMessage(), ignore);
        }
    }

    private String now() {
        SimpleDateFormat lFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String gRtnStr = lFormat.format(new Date());
        return gRtnStr;
    }

}
