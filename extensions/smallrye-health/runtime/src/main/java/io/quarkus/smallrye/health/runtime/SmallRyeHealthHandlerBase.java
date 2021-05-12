package io.quarkus.smallrye.health.runtime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.smallrye.health.SmallRyeHealth;
import io.smallrye.health.SmallRyeHealthReporter;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

abstract class SmallRyeHealthHandlerBase implements Handler<RoutingContext> {

    protected abstract SmallRyeHealth getHealth(SmallRyeHealthReporter reporter, RoutingContext routingContext);

    @Override
    public void handle(RoutingContext ctx) {
        ManagedContext requestContext = Arc.container().requestContext();
        if (requestContext.isActive()) {
            doHandle(ctx);
        } else {
            requestContext.activate();
            try {
                doHandle(ctx);
            } finally {
                requestContext.terminate();
            }
        }
    }

    private void doHandle(RoutingContext ctx) {
        QuarkusHttpUser user = (QuarkusHttpUser) ctx.user();
        if (user != null) {
            Arc.container().instance(CurrentIdentityAssociation.class).get().setIdentity(user.getSecurityIdentity());
        }
        SmallRyeHealthReporter reporter = Arc.container().instance(SmallRyeHealthReporter.class).get();
        SmallRyeHealth health = getHealth(reporter, ctx);
        HttpServerResponse resp = ctx.response();
        if (health.isDown()) {
            resp.setStatusCode(503);
        }
        resp.headers().set(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            reporter.reportHealth(outputStream, health);
            resp.end(Buffer.buffer(outputStream.toByteArray()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
