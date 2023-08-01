package io.quarkus.smallrye.health.runtime;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.vertx.core.runtime.BufferOutputStream;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.smallrye.health.SmallRyeHealth;
import io.smallrye.health.SmallRyeHealthReporter;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

abstract class SmallRyeHealthHandlerBase implements Handler<RoutingContext> {

    private static final Logger log = Logger.getLogger(SmallRyeHealthHandlerBase.class);

    protected abstract Uni<SmallRyeHealth> getHealth(SmallRyeHealthReporter reporter, RoutingContext routingContext);

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
        getHealth(reporter, ctx).subscribe().with(health -> {
            HttpServerResponse resp = ctx.response();
            if (health.isDown()) {
                resp.setStatusCode(503);
            }
            resp.headers().set(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
            Buffer buffer = Buffer.buffer(256); // this size seems to cover the basic health checks
            try (BufferOutputStream outputStream = new BufferOutputStream(buffer)) {
                reporter.reportHealth(outputStream, health);
            } catch (Throwable e) {
                log.error("Unable to prepare health response message", e);
                resp.setStatusCode(500);
                resp.end();
                return;
            }
            resp.end(buffer);
        }, throwable -> {
            log.error("Unable to prepare health response message", throwable);
            HttpServerResponse resp = ctx.response();
            resp.setStatusCode(500);
            resp.end();
        });
    }

}
