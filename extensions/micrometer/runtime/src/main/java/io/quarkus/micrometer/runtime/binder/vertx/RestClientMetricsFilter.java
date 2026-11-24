package io.quarkus.micrometer.runtime.binder.vertx;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestFilter;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientResponseFilter;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.quarkus.arc.Unremovable;
import io.quarkus.micrometer.runtime.binder.HttpBinderConfiguration;
import io.quarkus.micrometer.runtime.binder.HttpCommonTags;
import io.quarkus.micrometer.runtime.binder.MeterCache;
import io.quarkus.micrometer.runtime.binder.RequestMetricInfo;
import io.vertx.core.http.HttpClientOptions;

/**
 * A client filter for the Quarkus REST Client that records Micrometer data
 */
@Unremovable
@Provider
@SuppressWarnings("unused") // this is used by io.quarkus.micrometer.deployment.binder.HttpBinderProcessor
public class RestClientMetricsFilter implements ResteasyReactiveClientRequestFilter,
        ResteasyReactiveClientResponseFilter {

    private final static String REQUEST_METRIC_PROPERTY = "restClientMetrics";
    private final MeterRegistry registry = Metrics.globalRegistry;

    private final HttpBinderConfiguration httpMetricsConfig;

    private final MeterCache<Timer> timers;

    @Inject
    public RestClientMetricsFilter(final HttpBinderConfiguration httpMetricsConfig) {
        this.httpMetricsConfig = httpMetricsConfig;

        Meter.MeterProvider<Timer> timer = Timer.builder(httpMetricsConfig.getHttpClientRequestsName())
                .withRegistry(registry);
        timers = new MeterCache<>(timer);
    }

    @Override
    public void filter(ResteasyReactiveClientRequestContext requestContext) {
        if (!httpMetricsConfig.isClientEnabled()) {
            return;
        }

        RequestMetricInfo requestMetric = new RestClientMetricInfo(requestContext);
        requestMetric.setSample(Timer.start(registry));
        requestContext.setProperty(REQUEST_METRIC_PROPERTY, requestMetric);
    }

    @Override
    public void filter(ResteasyReactiveClientRequestContext requestContext, ClientResponseContext responseContext) {
        if (!httpMetricsConfig.isClientEnabled()) {
            return;
        }

        RequestMetricInfo requestMetric = getRequestMetric(requestContext);
        if (requestMetric != null) {
            String templatePath = (String) requestContext.getProperty("UrlPathTemplate");

            String requestPath = requestMetric.getNormalizedUriPath(
                    httpMetricsConfig.getClientMatchPatterns(),
                    httpMetricsConfig.getClientIgnorePatterns(),
                    templatePath == null ? requestContext.getUri().getPath() : templatePath);
            if (requestPath != null) {
                Timer.Sample sample = requestMetric.getSample();
                int statusCode = responseContext.getStatus();

                sample.stop(timers.get(new TimerKey(
                        address(requestContext),
                        HttpCommonTags.method(requestContext.getMethod()),
                        HttpCommonTags.uri(requestPath, requestContext.getUri().getPath(), statusCode,
                                httpMetricsConfig.isClientSuppress4xxErrors()),
                        HttpCommonTags.outcome(statusCode),
                        HttpCommonTags.status(statusCode),
                        clientName(requestContext))));
            }
        }
    }

    private RequestMetricInfo getRequestMetric(ClientRequestContext requestContext) {
        return (RequestMetricInfo) requestContext.getProperty(REQUEST_METRIC_PROPERTY);
    }

    private Tag address(ResteasyReactiveClientRequestContext requestContext) {
        String host = requestContext.getUri().getHost();
        int port = requestContext.getUri().getPort();
        if (host == null) {
            return Tag.of("address", "none");
        }
        return Tag.of("address", port > 0 ? host + ":" + port : host);
    }

    private Tag clientName(ResteasyReactiveClientRequestContext requestContext) {
        String host = null;
        HttpClientOptions httpClientOptions = requestContext.unwrap(HttpClientOptions.class);
        if ((httpClientOptions != null) && httpClientOptions.getMetricsName() != null) {
            host = VertxMeterBinderAdapter.extractClientName(httpClientOptions.getMetricsName());
        } else {
            if (requestContext.getUri().getHost() != null) {
                host = requestContext.getUri().getHost();
            }
        }

        return Tag.of("clientName", host != null ? host : "none");
    }

    /**
     * Key of the cached timers: holds exactly the tags of the timer.
     */
    record TimerKey(Tag address, Tag method, Tag uri, Tag outcome, Tag status, Tag clientName) implements MeterCache.Key {
        @Override
        public Tags tags() {
            return Tags.of(address, method, uri, outcome, status, clientName);
        }
    }

    static class RestClientMetricInfo extends RequestMetricInfo {
        ClientRequestContext requestContext;

        RestClientMetricInfo(ClientRequestContext requestContext) {
            super();
            this.requestContext = requestContext;
        }
    }
}
