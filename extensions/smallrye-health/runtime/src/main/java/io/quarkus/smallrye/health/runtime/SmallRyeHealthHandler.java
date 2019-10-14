package io.quarkus.smallrye.health.runtime;

import java.io.ByteArrayOutputStream;

import javax.enterprise.inject.spi.CDI;

import io.quarkus.arc.Arc;
import io.smallrye.health.SmallRyeHealth;
import io.smallrye.health.SmallRyeHealthReporter;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

@SuppressWarnings("serial")
public class SmallRyeHealthHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext event) {
        boolean activated = RequestScopeHelper.activeRequestScope();

        try {
            SmallRyeHealthReporter reporter = CDI.current().select(SmallRyeHealthReporter.class).get();
            SmallRyeHealth health = reporter.getHealth();
            HttpServerResponse resp = event.response();
            if (health.isDown()) {
                resp.setStatusCode(503);
            }
            resp.headers().set(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            reporter.reportHealth(outputStream, health);
            resp.end(Buffer.buffer(outputStream.toByteArray()));
        } finally {
            if (activated) {
                Arc.container().requestContext().terminate();
            }
        }
    }
}
