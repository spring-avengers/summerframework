package com.bkjk.platform.monitor.metric.micrometer.binder.openfeign;

import com.bkjk.platform.monitor.metric.MicrometerUtil;
import feign.Request;
import feign.Response;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;

@Aspect
public class OpenfeignMetricsBinder {
    public static final String CLASS_HEADER = "x-call-class";
    public static final String METHOD_HEADER = "x-call-method";

    private static String getUrl(String url) {
        int index = url.indexOf("?");
        if (index > 0) {
            url = url.substring(0, index);
        }
        return url;
    }

    public static void main(String[] args) {
        System.out.println(getUrl("a"));
        System.out.println(getUrl("a?a=3"));
    }

    private final Iterable<Tag> tags;

    public OpenfeignMetricsBinder() {
        this(emptyList());
    }

    public OpenfeignMetricsBinder(Iterable<Tag> tags) {
        this.tags = tags;
    }

    @Around("execution(* feign.Client.execute(..))")
    public Response around(ProceedingJoinPoint pjp) throws Throwable {
        long start = MicrometerUtil.monotonicTime();
        Request request = (Request)pjp.getArgs()[0];
        Response response = null;
        Throwable e = null;
        try {
            response = (Response)pjp.proceed();
        } catch (Throwable t) {
            throw e = t;
        } finally {
            long lapsed = MicrometerUtil.monotonicTime() - start;
            Timer timer = Metrics.timer("openfeign",
                Tags.of(tags)
                    .and(Tag.of("status", null == response ? "CLIENT_ERROR" : String.valueOf(response.status())),
                        Tag.of("method", request.method()), Tag.of("class", getKey(CLASS_HEADER, request.headers())),
                        Tag.of("classMethod", getKey(METHOD_HEADER, request.headers()))
                    // Tag.of("url", getUrl(request.url()))
                    ).and(MicrometerUtil.exceptionAndStatusKey(e)));
            timer.record(lapsed, TimeUnit.NANOSECONDS);
        }
        return response;
    }

    private String getKey(String key, Map<String, Collection<String>> headers) {
        if (headers.containsKey(key)) {
            return headers.get(key).stream().findFirst().orElseGet(() -> "none");
        }
        return "none";
    }
}
