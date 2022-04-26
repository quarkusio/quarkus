package io.quarkus.micrometer.runtime.binder;

import java.io.IOException;

import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.spi.RestClientListener;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.quarkus.arc.Arc;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;

/**
 * This is initialized via ServiceFactory (static/non-CDI initialization)
 */
public class RestClientMetricsListener implements RestClientListener {

    private final static String REQUEST_METRIC_PROPERTY = "restClientMetrics";

    private final MeterRegistry registry = Metrics.globalRegistry;
    private boolean initialized = false;
    private boolean clientMetricsEnabled = false;

    private MetricsClientRequestFilter clientRequestFilter;
    private MetricsClientResponseFilter clientResponseFilter;

    @Override
    public void onNewClient(Class<?> serviceInterface, RestClientBuilder builder) {
        MicrometerConfig micrometerConfig = Arc.container().instance(MicrometerConfig.class).get();
        if (!micrometerConfig.enabled) {
            return;
        }
        if (prepClientMetrics()) {
            // This must run AFTER the OpenTelmetry client request filter
            builder.register(this.clientRequestFilter, Priorities.HEADER_DECORATOR + 1);
            // This must run Before the OpenTelmetry client response filter
            builder.register(this.clientResponseFilter, Priorities.HEADER_DECORATOR + 1);
        }
    }

    boolean prepClientMetrics() {
        boolean clientMetricsEnabled = this.clientMetricsEnabled;
        if (!this.initialized) {
            HttpBinderConfiguration httpMetricsConfig = Arc.container().instance(HttpBinderConfiguration.class).get();
            clientMetricsEnabled = httpMetricsConfig.isClientEnabled();
            if (clientMetricsEnabled) {
                this.clientRequestFilter = new MetricsClientRequestFilter(registry);
                this.clientResponseFilter = new MetricsClientResponseFilter(registry, httpMetricsConfig);
            }
            this.clientMetricsEnabled = clientMetricsEnabled;
            this.initialized = true;
        }
        return clientMetricsEnabled;
    }

    static class MetricsClientRequestFilter implements ClientRequestFilter {
        private final MeterRegistry registry;

        MetricsClientRequestFilter(MeterRegistry registry) {
            this.registry = registry;
        }

        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            RequestMetricInfo requestMetric = new RestClientMetricInfo(requestContext);
            requestMetric.setSample(Timer.start(registry));
            requestContext.setProperty(REQUEST_METRIC_PROPERTY, requestMetric);
        }
    }

    static class MetricsClientResponseFilter implements ClientResponseFilter {
        private final MeterRegistry registry;
        private final HttpBinderConfiguration httpMetricsConfig;

        MetricsClientResponseFilter(MeterRegistry registry,
                HttpBinderConfiguration httpMetricsConfig) {
            this.registry = registry;
            this.httpMetricsConfig = httpMetricsConfig;
        }

        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
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
    }

    static class RestClientMetricInfo extends RequestMetricInfo {
        ClientRequestContext requestContext;

        RestClientMetricInfo(ClientRequestContext requestContext) {
            super();
            this.requestContext = requestContext;
        }
    }
}
