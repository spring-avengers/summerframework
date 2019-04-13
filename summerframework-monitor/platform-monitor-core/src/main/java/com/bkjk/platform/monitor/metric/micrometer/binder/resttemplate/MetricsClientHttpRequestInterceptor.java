package com.bkjk.platform.monitor.metric.micrometer.binder.resttemplate;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.actuate.metrics.web.client.DefaultRestTemplateExchangeTagsProvider;
import org.springframework.boot.actuate.metrics.web.client.RestTemplateExchangeTagsProvider;
import org.springframework.core.NamedThreadLocal;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.AsyncClientHttpRequestExecution;
import org.springframework.http.client.AsyncClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.util.UriTemplateHandler;

import com.bkjk.platform.monitor.metric.MicrometerUtil;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.lang.Nullable;

public class MetricsClientHttpRequestInterceptor
    implements ClientHttpRequestInterceptor, AsyncClientHttpRequestInterceptor {

    private static final ThreadLocal<String> urlTemplateHolder = new NamedThreadLocal<>("Rest Template URL Template");

    private final MeterRegistry meterRegistry;
    private final RestTemplateExchangeTagsProvider tagProvider = new DefaultRestTemplateExchangeTagsProvider();
    private final String metricName;

    public MetricsClientHttpRequestInterceptor(MeterRegistry meterRegistry, String metricName) {
        this.meterRegistry = meterRegistry;
        this.metricName = metricName;
    }

    UriTemplateHandler createUriTemplateHandler(UriTemplateHandler delegate) {
        return new UriTemplateHandler() {

            @Override
            public URI expand(String url, Map<String, ?> arguments) {
                urlTemplateHolder.set(url);
                return delegate.expand(url, arguments);
            }

            @Override
            public URI expand(String url, Object... arguments) {
                urlTemplateHolder.set(url);
                return delegate.expand(url, arguments);
            }
        };
    }

    private Timer.Builder getTimeBuilder(@Nullable String urlTemplate, HttpRequest request,
        @Nullable ClientHttpResponse response, @Nullable Throwable e) {
        return Timer.builder(this.metricName).tags(Tags.concat(this.tagProvider.getTags(urlTemplate, request, response),
            MicrometerUtil.exceptionAndStatusKey(e))).description("Timer of RestTemplate operation");
    }

    @Override
    public ListenableFuture<ClientHttpResponse> intercept(HttpRequest request, byte[] body,
        AsyncClientHttpRequestExecution execution) throws IOException {
        final String urlTemplate = urlTemplateHolder.get();
        urlTemplateHolder.remove();
        final Clock clock = meterRegistry.config().clock();
        final long startTime = clock.monotonicTime();
        ListenableFuture<ClientHttpResponse> future;
        try {
            future = execution.executeAsync(request, body);
        } catch (IOException e) {
            getTimeBuilder(urlTemplate, request, null, e).register(meterRegistry)
                .record(clock.monotonicTime() - startTime, TimeUnit.NANOSECONDS);
            throw e;
        }
        future.addCallback(new ListenableFutureCallback<ClientHttpResponse>() {
            @Override
            public void onFailure(final Throwable ex) {
                getTimeBuilder(urlTemplate, request, null, ex).register(meterRegistry)
                    .record(clock.monotonicTime() - startTime, TimeUnit.NANOSECONDS);
            }

            @Override
            public void onSuccess(final ClientHttpResponse response) {
                getTimeBuilder(urlTemplate, request, response, null).register(meterRegistry)
                    .record(clock.monotonicTime() - startTime, TimeUnit.NANOSECONDS);
            }
        });
        return future;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
        throws IOException {
        final String urlTemplate = urlTemplateHolder.get();
        urlTemplateHolder.remove();
        final Clock clock = meterRegistry.config().clock();
        final long startTime = clock.monotonicTime();
        IOException ex = null;
        ClientHttpResponse response = null;
        try {
            response = execution.execute(request, body);
            return response;
        } catch (IOException e) {
            ex = e;
            throw e;
        } finally {
            getTimeBuilder(urlTemplate, request, response, ex).register(this.meterRegistry)
                .record(clock.monotonicTime() - startTime, TimeUnit.NANOSECONDS);
        }
    }
}
