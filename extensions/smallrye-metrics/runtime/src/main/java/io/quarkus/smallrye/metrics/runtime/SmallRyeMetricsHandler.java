package io.quarkus.smallrye.metrics.runtime;

import java.io.IOException;
import java.util.stream.Stream;

import javax.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import io.smallrye.metrics.MetricsRequestHandler;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class SmallRyeMetricsHandler implements Handler<RoutingContext> {

    private String metricsPath;

    private static final Logger LOGGER = Logger.getLogger(SmallRyeMetricsHandler.class.getName());

    public void setMetricsPath(String metricsPath) {
        this.metricsPath = metricsPath;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        MetricsRequestHandler internalHandler = CDI.current().select(MetricsRequestHandler.class).get();
        HttpServerResponse response = routingContext.response();
        HttpServerRequest request = routingContext.request();
        Stream<String> acceptHeaders = request.headers().getAll("Accept").stream();

        try {
            internalHandler.handleRequest(request.path(), metricsPath, request.rawMethod(), acceptHeaders,
                    (status, message, headers) -> {
                        response.setStatusCode(status);
                        headers.forEach(response::putHeader);
                        response.end(Buffer.buffer(message));
                    });
        } catch (IOException e) {
            response.setStatusCode(503);
            response.end();
            LOGGER.error(e);
        }
    }
}
