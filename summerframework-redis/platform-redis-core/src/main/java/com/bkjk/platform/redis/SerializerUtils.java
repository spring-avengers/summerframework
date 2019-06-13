package com.bkjk.platform.redis;

import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

public class SerializerUtils {
    public static RedisSerializer<?> getDefaultSerializer() {

        return new GenericJackson2JsonRedisSerializer();
    }

    public static int getValueSize(RedisSerializer serializer, Object[] args) {
        try {
            if (args == null || args.length == 0) {
                return 0;
            }
            return serializer.serialize(args).length;
        } catch (Exception e) {
            return 0;
        }
    }
}
