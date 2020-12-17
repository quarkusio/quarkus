package io.quarkus.micrometer.runtime.binder;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.spi.RestClientListener;
import org.jboss.logging.Logger;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;

public class RestClientMetrics implements RestClientListener {
    private static final Logger log = Logger.getLogger(RestClientMetrics.class);
    private static final String HTTP_CLIENT_METRIC_NAME = "http.client.requests";
    private final static String REQUEST_TIMER_SAMPLE_PROPERTY = "requestStartTime";

    final MeterRegistry registry = Metrics.globalRegistry;
    final MetricsClientRequestFilter clientRequestFilter = new MetricsClientRequestFilter();
    final MetricsClientResponseFilter clientResponseFilter = new MetricsClientResponseFilter();

    @Override
    public void onNewClient(Class<?> serviceInterface, RestClientBuilder builder) {
        builder.register(clientRequestFilter);
        builder.register(clientResponseFilter);
    }

    class MetricsClientRequestFilter implements ClientRequestFilter {
        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            requestContext.setProperty(REQUEST_TIMER_SAMPLE_PROPERTY, Timer.start(registry));
        }
    }

    class MetricsClientResponseFilter implements ClientResponseFilter {
        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
            Timer.Sample sample = getRequestSample(requestContext);
            if (sample != null) {
                String requestPath = getRequestPath(requestContext);
                int statusCode = responseContext.getStatus();
                Timer.Builder builder = Timer.builder(HTTP_CLIENT_METRIC_NAME)
                        .tags(Tags.of(
                                HttpTags.method(requestContext.getMethod()),
                                HttpTags.uri(requestPath, statusCode),
                                HttpTags.outcome(statusCode),
                                HttpTags.status(statusCode),
                                clientName(requestContext)));

                sample.stop(builder.register(registry));
            }
        }

        private String getRequestPath(ClientRequestContext requestContext) {
            return requestContext.getUri().getPath();
        }

        private Timer.Sample getRequestSample(ClientRequestContext requestContext) {
            return (Timer.Sample) requestContext.getProperty(REQUEST_TIMER_SAMPLE_PROPERTY);
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
