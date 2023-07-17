package io.quarkus.micrometer.runtime.binder;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.ext.Provider;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.quarkus.arc.Arc;
import io.quarkus.arc.Unremovable;

/**
 * A client filter for the JAX-RS Client and MicroProfile REST Client that records OpenTelemetry data.
 */
@Unremovable
@Provider
public class RestClientMetricsFilter implements ClientRequestFilter, ClientResponseFilter {

    private final static String REQUEST_METRIC_PROPERTY = "restClientMetrics";
    private final MeterRegistry registry = Metrics.globalRegistry;

    private final HttpBinderConfiguration httpMetricsConfig;

    // RESTEasy requires no-arg constructor for CDI injection: https://issues.redhat.com/browse/RESTEASY-1538
    // In the classic Rest Client this is the constructor called whereas in the Reactive one,
    // the constructor using HttpBinderConfiguration is called.
    public RestClientMetricsFilter() {
        this(Arc.container().instance(HttpBinderConfiguration.class).get());
    }

    @Inject
    public RestClientMetricsFilter(final HttpBinderConfiguration httpMetricsConfig) {
        this.httpMetricsConfig = httpMetricsConfig;
    }

    @Override
    public void filter(final ClientRequestContext requestContext) {
        if (!httpMetricsConfig.isClientEnabled()) {
            return;
        }

        RequestMetricInfo requestMetric = new RestClientMetricInfo(requestContext);
        requestMetric.setSample(Timer.start(registry));
        requestContext.setProperty(REQUEST_METRIC_PROPERTY, requestMetric);
    }

    @Override
    public void filter(final ClientRequestContext requestContext, final ClientResponseContext responseContext) {
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

                Timer.Builder builder = Timer.builder(httpMetricsConfig.getHttpClientRequestsName())
                        .tags(Tags.of(
                                HttpCommonTags.method(requestContext.getMethod()),
                                HttpCommonTags.uri(requestPath, statusCode),
                                HttpCommonTags.outcome(statusCode),
                                HttpCommonTags.status(statusCode),
                                clientName(requestContext)));

                sample.stop(builder.register(registry));
            }
        }
    }

    private RequestMetricInfo getRequestMetric(ClientRequestContext requestContext) {
        return (RequestMetricInfo) requestContext.getProperty(REQUEST_METRIC_PROPERTY);
    }

    private Tag clientName(ClientRequestContext requestContext) {
        String host = requestContext.getUri().getHost();
        if (host == null) {
            host = "none";
        }
        return Tag.of("clientName", host);
    }

    static class RestClientMetricInfo extends RequestMetricInfo {
        ClientRequestContext requestContext;

        RestClientMetricInfo(ClientRequestContext requestContext) {
            super();
            this.requestContext = requestContext;
        }
    }
}
