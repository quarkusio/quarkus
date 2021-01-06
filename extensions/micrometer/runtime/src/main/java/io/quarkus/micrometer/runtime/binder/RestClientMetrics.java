package io.quarkus.micrometer.runtime.binder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
import io.quarkus.micrometer.runtime.config.runtime.HttpClientConfig;

public class RestClientMetrics implements RestClientListener {
    private static final String HTTP_CLIENT_METRIC_NAME = "http.client.requests";
    private final static String REQUEST_METRIC_PROPERTY = "restClientMetrics";

    final List<Pattern> ignorePatterns;
    final Map<Pattern, String> matchPatterns;

    final MeterRegistry registry = Metrics.globalRegistry;
    final MetricsClientRequestFilter clientRequestFilter = new MetricsClientRequestFilter();
    final MetricsClientResponseFilter clientResponseFilter = new MetricsClientResponseFilter();

    public RestClientMetrics() {
        HttpClientConfig clientConfig = Arc.container().instance(HttpClientConfig.class).get();
        ignorePatterns = HttpMetricsCommon.getIgnorePatterns(clientConfig.ignorePatterns);
        matchPatterns = HttpMetricsCommon.getMatchPatterns(clientConfig.matchPatterns);
    }

    @Override
    public void onNewClient(Class<?> serviceInterface, RestClientBuilder builder) {
        builder.register(clientRequestFilter);
        builder.register(clientResponseFilter);
    }

    class MetricsClientRequestFilter implements ClientRequestFilter {
        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            HttpRequestMetric requestMetric = new HttpRequestMetric();
            requestMetric.parseUriPath(matchPatterns, ignorePatterns, requestContext.getUri().getPath());
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
