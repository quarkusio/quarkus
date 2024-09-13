package io.quarkus.smallrye.health.runtime;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonGenerator;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.vertx.core.runtime.BufferOutputStream;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.smallrye.health.SmallRyeHealth;
import io.smallrye.health.SmallRyeHealthReporter;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.MutinyHelper;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public abstract class SmallRyeHealthHandlerBase implements Handler<RoutingContext> {

    static volatile boolean problemDetails = false;

    protected abstract Uni<SmallRyeHealth> getHealth(SmallRyeHealthReporter reporter, RoutingContext routingContext);

    private static final Map<String, ?> JSON_CONFIG = Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true);
    private static final JsonProvider JSON_PROVIDER = JsonProvider.provider();
    private static final JsonWriterFactory JSON_WRITER_FACTORY = JSON_PROVIDER.createWriterFactory(JSON_CONFIG);

    @Override
    public void handle(RoutingContext ctx) {
        ManagedContext requestContext = Arc.container().requestContext();
        if (requestContext.isActive()) {
            doHandle(ctx, null);
        } else {
            requestContext.activate();
            try {
                doHandle(ctx, requestContext);
            } catch (Exception e) {
                requestContext.terminate();
                throw e;
            }
        }
    }

    private void doHandle(RoutingContext ctx, ManagedContext requestContext) {
        QuarkusHttpUser user = (QuarkusHttpUser) ctx.user();
        if (user != null) {
            Arc.container().instance(CurrentIdentityAssociation.class).get().setIdentity(user.getSecurityIdentity());
        }
        SmallRyeHealthReporter reporter = Arc.container().instance(SmallRyeHealthReporter.class).get();
        Context context = Vertx.currentContext();
        Uni<SmallRyeHealth> healthUni = getHealth(reporter, ctx);
        if (context != null) {
            healthUni = healthUni.emitOn(MutinyHelper.executor(context));
        }
        healthUni.subscribe().with(health -> {
            if (requestContext != null) {
                requestContext.terminate();
            }
            HttpServerResponse resp = ctx.response();
            Buffer buffer = Buffer.buffer(256); // this size seems to cover the basic health checks
            if (health.isDown()) {
                resp.setStatusCode(503);
                if (problemDetails) {
                    resp.headers().set(HttpHeaders.CONTENT_TYPE, "application/problem+json");
                    try (BufferOutputStream outputStream = new BufferOutputStream(buffer)) {
                        JsonObjectBuilder objectBuilder = JSON_PROVIDER.createObjectBuilder();
                        objectBuilder
                                .add("type", "about:blank")
                                .add("status", 503)
                                .add("title", "Health Check Failed: " + ctx.normalizedPath())
                                .add("detail", ctx.request().uri() + ", invoked at " + Instant.now())
                                .add("instance", ctx.request().absoluteURI())
                                .add("health", health.getPayload());
                        JsonWriter writer = JSON_WRITER_FACTORY.createWriter(outputStream);
                        writer.writeObject(objectBuilder.build());
                        resp.end(buffer);
                        return;
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            }
            resp.headers()
                    .set(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8")
                    .set(HttpHeaders.CACHE_CONTROL, "no-store");

            try (BufferOutputStream outputStream = new BufferOutputStream(buffer)) {
                reporter.reportHealth(outputStream, health);
                resp.end(buffer);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, failure -> {
            if (requestContext != null) {
                requestContext.terminate();
            }
        });
    }
}
