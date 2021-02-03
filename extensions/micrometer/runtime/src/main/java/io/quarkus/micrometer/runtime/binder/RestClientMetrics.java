package io.quarkus.micrometer.runtime.binder;

import java.io.IOException;

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
public class RestClientMetrics implements RestClientListener {
    private static final String HTTP_CLIENT_METRIC_NAME = "http.client.requests";
    private final static String REQUEST_METRIC_PROPERTY = "restClientMetrics";

    final MeterRegistry registry = Metrics.globalRegistry;
    MetricsClientRequestFilter clientRequestFilter;
    MetricsClientResponseFilter clientResponseFilter;

    @Override
    public void onNewClient(Class<?> serviceInterface, RestClientBuilder builder) {
        builder.register(getClientRequestFilter());
        builder.register(getClientResponseFilter());
    }

    // Lazy init: multiple instances aren't harmful, synchronization not necessary
    MetricsClientRequestFilter getClientRequestFilter() {
        MetricsClientRequestFilter clientFilter = this.clientRequestFilter;
        if (clientFilter == null) {
            HttpBinderConfiguration httpMetricsConfig = Arc.container().instance(HttpBinderConfiguration.class).get();
            clientFilter = this.clientRequestFilter = new MetricsClientRequestFilter(httpMetricsConfig);
        }
        return clientFilter;
    }

    // Lazy init: multiple instances aren't harmful, synchronization not necessary
    MetricsClientResponseFilter getClientResponseFilter() {
        MetricsClientResponseFilter clientFilter = this.clientResponseFilter;
        if (clientFilter == null) {
            clientFilter = this.clientResponseFilter = new MetricsClientResponseFilter();
        }
        return clientFilter;
    }

    class MetricsClientRequestFilter implements ClientRequestFilter {
        HttpBinderConfiguration binderConfiguration;

        MetricsClientRequestFilter(HttpBinderConfiguration binderConfiguration) {
            this.binderConfiguration = binderConfiguration;
        }

        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            HttpRequestMetric requestMetric = new HttpRequestMetric();
            requestMetric.parseUriPath(binderConfiguration.getClientMatchPatterns(),
                    binderConfiguration.getClientIgnorePatterns(), requestContext.getUri().getPath());
            if (requestMetric.isMeasure()) {
                requestMetric.setSample(Timer.start(registry));
                requestContext.setProperty(REQUEST_METRIC_PROPERTY, requestMetric);
            }
        }
    }

    class MetricsClientResponseFilter implements ClientResponseFilter {
        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
            HttpRequestMetric requestMetric = getRequestMetric(requestContext);
            if (requestMetric != null) {
                Timer.Sample sample = requestMetric.sample;
                String requestPath = requestMetric.getHttpRequestPath();
                int statusCode = responseContext.getStatus();
                Timer.Builder builder = Timer.builder(HTTP_CLIENT_METRIC_NAME)
                        .tags(Tags.of(
                                HttpMetricsCommon.method(requestContext.getMethod()),
                                HttpMetricsCommon.uri(requestPath, statusCode),
                                HttpMetricsCommon.outcome(statusCode),
                                HttpMetricsCommon.status(statusCode),
                                clientName(requestContext)));

                sample.stop(builder.register(registry));
            }
        }

        private HttpRequestMetric getRequestMetric(ClientRequestContext requestContext) {
            return (HttpRequestMetric) requestContext.getProperty(REQUEST_METRIC_PROPERTY);
        }

        private Tag clientName(ClientRequestContext requestContext) {
            String host = requestContext.getUri().getHost();
            if (host == null) {
                host = "none";
            }
            return Tag.of("clientName", host);
        }
    }
}
