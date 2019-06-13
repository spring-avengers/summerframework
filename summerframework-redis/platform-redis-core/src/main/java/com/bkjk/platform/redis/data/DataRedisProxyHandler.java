package com.bkjk.platform.redis.data;

import static com.bkjk.platform.redis.SerializerUtils.getValueSize;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.springframework.data.redis.serializer.RedisSerializer;

import com.bkjk.platform.redis.AbstractRedisMonitorListener;
import com.bkjk.platform.redis.SerializerUtils;
import com.sohu.tv.cachecloud.client.basic.util.NetUtils;
import com.sohu.tv.jedis.stat.data.UsefulDataCollector;
import com.sohu.tv.jedis.stat.model.UsefulDataModel;

public class DataRedisProxyHandler implements InvocationHandler {

    private static final String HASH_CODE = "hashCode";
    private static final String EQUALS = "equals";
    private static final String TO_STRING = "toString";
    private static ThreadLocal<UsefulDataModel> threadLocal = new ThreadLocal<>();
    private static String clientIp = NetUtils.getLocalHost();
    private static RedisSerializer serializer = SerializerUtils.getDefaultSerializer();
    private final Object subject;

    public DataRedisProxyHandler(Object subject) {
        this.subject = subject;
    }

    private Object doInvoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result;
        long start = System.nanoTime();
        int valueSize = getValueSize(serializer, args);

        UsefulDataModel costModel = UsefulDataModel.getCostModel(threadLocal);
        costModel.setCommand(method.getName());
        costModel.setStartTime(System.currentTimeMillis());
        costModel.setHostPort(clientIp);
        costModel.setValueBytesLength(valueSize);
        try {
            result = method.invoke(subject, args);
            costModel.setEndTime(System.currentTimeMillis());

            long cost = System.nanoTime() - start;
            AbstractRedisMonitorListener.forEachListener(redisCommandListener -> {
                redisCommandListener.afterCommand(clientIp, method.getName(), cost, valueSize, null);
            });
        } catch (Exception e) {
            costModel.setEndTime(System.currentTimeMillis());
            UsefulDataCollector.collectException(e, clientIp, System.currentTimeMillis());

            long cost = System.nanoTime() - start;
            AbstractRedisMonitorListener.forEachListener(redisCommandListener -> {
                redisCommandListener.afterCommand(clientIp, method.getName(), cost, valueSize, e);
            });
            throw e;
        } finally {
            threadLocal.remove();
            UsefulDataCollector.collectCostAndValueDistribute(costModel);
        }
        return result;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if (HASH_CODE.equals(methodName) || EQUALS.equals(methodName) || TO_STRING.equals(methodName)) {
            return method.invoke(subject, args);
        }

        return doInvoke(proxy, method, args);
    }

}
