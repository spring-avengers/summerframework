
package com.bkjk.platform.monitor.logging.appender;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.kafka.clients.producer.KafkaProducer;

public class DefaultKafkaProducerFactory implements KafkaProducerFactory {

    private static final Map<Properties, KafkaProducer<byte[], byte[]>> PRODUCER_CACHE =
        new ConcurrentHashMap<Properties, KafkaProducer<byte[], byte[]>>();

    @Override
    public org.apache.kafka.clients.producer.Producer<byte[], byte[]> newKafkaProducer(final Properties config) {
        if (PRODUCER_CACHE.containsKey(config)) {
            return PRODUCER_CACHE.get(config);
        } else {
            KafkaProducer<byte[], byte[]> kafkaProducer = new KafkaProducer<byte[], byte[]>(config);
            PRODUCER_CACHE.put(config, kafkaProducer);
            return kafkaProducer;
        }

    }

}
