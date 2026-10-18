package io.quarkus.jfr.runtime.internal.http.rest.client;

import java.net.URI;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestFilter;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientResponseFilter;

import io.quarkus.arc.Arc;
import io.quarkus.jfr.api.IdProducer;
import io.vertx.core.http.HttpClientOptions;

/**
 * Records a {@link RestClientPeriodEvent} for every REST Client invocation.
 * <p>
 * The event is started before the request is sent and committed once the response has been received
 * or the request has failed. Streaming responses (Multi, SSE) are not recorded as their duration is
 * not well-defined.
 */
@Provider
public class RestClientFilter implements ResteasyReactiveClientRequestFilter, ResteasyReactiveClientResponseFilter {

    private static final String JFR_EVENT_PROPERTY = "quarkus.jfr.restClientEvent";
    private static final String URL_PATH_TEMPLATE_PROPERTY = "UrlPathTemplate";
    private static final String CLIENT_NAME_SEPARATOR = "|";

    private final IdProducer idProducer;

    public RestClientFilter(IdProducer idProducer) {
        this.idProducer = idProducer;
    }

    @Override
    public void filter(ResteasyReactiveClientRequestContext requestContext) {
        // streaming invocations (Multi, SSE) do not register a body handler as the body is consumed as it arrives
        if (!requestContext.unwrap(RestClientRequestContext.class).isRegisterBodyHandler()) {
            return;
        }
        RestClientPeriodEvent event = new RestClientPeriodEvent();
        // the IDs are captured now because the request scope is not active anymore when the response arrives
        if (Arc.container().requestContext().isActive()) {
            event.traceId = idProducer.getTraceId();
            event.spanId = idProducer.getSpanId();
        }
        event.begin();
        requestContext.setProperty(JFR_EVENT_PROPERTY, event);
    }

    @Override
    public void filter(ResteasyReactiveClientRequestContext requestContext, ClientResponseContext responseContext) {
        RestClientPeriodEvent event = (RestClientPeriodEvent) requestContext.getProperty(JFR_EVENT_PROPERTY);
        if (event == null) {
            return;
        }
        event.end();
        if (!event.shouldCommit()) {
            return;
        }
        event.httpMethod = requestContext.getMethod();
        URI uri = requestContext.getUri();
        String pathTemplate = (String) requestContext.getProperty(URL_PATH_TEMPLATE_PROPERTY);
        event.uri = pathTemplate != null ? pathTemplate : uri.getPath();
        event.address = uri.getPort() > 0 ? uri.getHost() + ":" + uri.getPort() : uri.getHost();
        event.clientName = clientName(requestContext);
        event.status = responseContext.getStatus();
        event.failure = effectiveFailureClassName(requestContext);
        event.commit();
    }

    private static String clientName(ResteasyReactiveClientRequestContext requestContext) {
        HttpClientOptions httpClientOptions = requestContext.unwrap(HttpClientOptions.class);
        if (httpClientOptions == null) {
            return null;
        }
        // the REST Client sets the metrics name to "rest-client|<configKey>"
        String metricsName = httpClientOptions.getMetricsName();
        if (metricsName == null || !metricsName.contains(CLIENT_NAME_SEPARATOR)) {
            return null;
        }
        return metricsName.substring(metricsName.indexOf(CLIENT_NAME_SEPARATOR) + 1);
    }

    private static String effectiveFailureClassName(ResteasyReactiveClientRequestContext requestContext) {
        Throwable throwable = requestContext.unwrap(RestClientRequestContext.class).getThrowable();
        if (throwable == null) {
            return null;
        }
        // the actual I/O failure is more informative than the ProcessingException wrapping it
        if (throwable instanceof ProcessingException && throwable.getCause() != null) {
            throwable = throwable.getCause();
        }
        return throwable.getClass().getName();
    }
}
