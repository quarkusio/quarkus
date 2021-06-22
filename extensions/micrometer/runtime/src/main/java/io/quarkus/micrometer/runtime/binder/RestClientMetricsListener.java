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

/**
 * This is initialized via ServiceFactory (static/non-CDI initialization)
 */
public class RestClientMetricsListener implements RestClientListener {

    private final static String REQUEST_METRIC_PROPERTY = "restClientMetrics";

    final MeterRegistry registry = Metrics.globalRegistry;
    boolean initialized = false;
    boolean clientMetricsEnabled = false;

    HttpBinderConfiguration httpMetricsConfig;
    MetricsClientRequestFilter clientRequestFilter;
    MetricsClientResponseFilter clientResponseFilter;

    @Override
    public void onNewClient(Class<?> serviceInterface, RestClientBuilder builder) {
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
            this.httpMetricsConfig = Arc.container().instance(HttpBinderConfiguration.class).get();
            clientMetricsEnabled = httpMetricsConfig.isClientEnabled();
            if (clientMetricsEnabled) {
                this.clientRequestFilter = new MetricsClientRequestFilter(httpMetricsConfig);
                this.clientResponseFilter = new MetricsClientResponseFilter();
            }
            this.clientMetricsEnabled = clientMetricsEnabled;
            this.initialized = true;
        }
        return clientMetricsEnabled;
    }

    class MetricsClientRequestFilter implements ClientRequestFilter {
        HttpBinderConfiguration binderConfiguration;

        MetricsClientRequestFilter(HttpBinderConfiguration binderConfiguration) {
            this.binderConfiguration = binderConfiguration;
        }

        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            RequestMetricInfo requestMetric = new RestClientMetricInfo(requestContext);
            requestMetric.setSample(Timer.start(registry));
            requestContext.setProperty(REQUEST_METRIC_PROPERTY, requestMetric);
        }
    }

    class MetricsClientResponseFilter implements ClientResponseFilter {
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

    class RestClientMetricInfo extends RequestMetricInfo {
        ClientRequestContext requestContext;

        RestClientMetricInfo(ClientRequestContext requestContext) {
            super();
            this.requestContext = requestContext;
        }
    }
}
