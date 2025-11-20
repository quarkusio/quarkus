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

    private final Meter.MeterProvider<Timer> timer;

    @Inject
    public RestClientMetricsFilter(final HttpBinderConfiguration httpMetricsConfig) {
        this.httpMetricsConfig = httpMetricsConfig;

        timer = Timer.builder(httpMetricsConfig.getHttpClientRequestsName())
                .withRegistry(registry);

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

                sample.stop(timer
                        .withTags(Tags.of(
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

    static class RestClientMetricInfo extends RequestMetricInfo {
        ClientRequestContext requestContext;

        RestClientMetricInfo(ClientRequestContext requestContext) {
            super();
            this.requestContext = requestContext;
        }
    }
}
