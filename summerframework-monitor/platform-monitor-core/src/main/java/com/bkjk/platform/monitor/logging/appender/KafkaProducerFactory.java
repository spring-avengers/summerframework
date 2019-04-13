
package com.bkjk.platform.monitor.logging.appender;

import java.util.Properties;

import org.apache.kafka.clients.producer.Producer;

public interface KafkaProducerFactory {

    Producer<byte[], byte[]> newKafkaProducer(Properties config);

}
