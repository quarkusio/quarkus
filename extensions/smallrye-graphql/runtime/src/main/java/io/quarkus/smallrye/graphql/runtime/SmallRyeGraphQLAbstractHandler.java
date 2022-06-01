package io.quarkus.smallrye.graphql.runtime;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.smallrye.graphql.execution.ExecutionService;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler that does the execution of GraphQL Requests
 */
public abstract class SmallRyeGraphQLAbstractHandler implements Handler<RoutingContext> {

    private final CurrentIdentityAssociation currentIdentityAssociation;
    private final CurrentVertxRequest currentVertxRequest;
    private final ManagedContext currentManagedContext;

    private final boolean runBlocking;

    private volatile ExecutionService executionService;

    protected static final JsonReaderFactory jsonReaderFactory = Json.createReaderFactory(null);

    public SmallRyeGraphQLAbstractHandler(
            CurrentIdentityAssociation currentIdentityAssociation,
            CurrentVertxRequest currentVertxRequest,
            boolean runBlocking) {

        this.currentIdentityAssociation = currentIdentityAssociation;
        this.currentVertxRequest = currentVertxRequest;
        this.currentManagedContext = Arc.container().requestContext();
        this.runBlocking = runBlocking;
    }

    @Override
    public void handle(final RoutingContext ctx) {

        if (currentManagedContext.isActive()) {
            handleWithIdentity(ctx);
        } else {

            currentManagedContext.activate();
            handleWithIdentity(ctx);

            ctx.response().bodyEndHandler((e) -> {
                currentManagedContext.terminate();
            });
        }
    }

    private Void handleWithIdentity(final RoutingContext ctx) {
        if (currentIdentityAssociation != null) {
            QuarkusHttpUser existing = (QuarkusHttpUser) ctx.user();
            if (existing != null) {
                SecurityIdentity identity = existing.getSecurityIdentity();
                currentIdentityAssociation.setIdentity(identity);
            } else {
                currentIdentityAssociation.setIdentity(QuarkusHttpUser.getSecurityIdentity(ctx, null));
            }
        }
        currentVertxRequest.setCurrent(ctx);
        doHandle(ctx);
        return null;
    }

    protected abstract void doHandle(final RoutingContext ctx);

    protected JsonObject inputToJsonObject(String input) {
        try (JsonReader jsonReader = jsonReaderFactory.createReader(new StringReader(input))) {
            return jsonReader.readObject();
        }
    }

    protected ExecutionService getExecutionService() {
        if (this.executionService == null) {
            this.executionService = Arc.container().instance(ExecutionService.class).get();
        }
        return this.executionService;
    }

    protected Map<String, Object> getMetaData(RoutingContext ctx) {
        // Add some context to dfe
        Map<String, Object> metaData = new ConcurrentHashMap<>();
        metaData.put("runBlocking", runBlocking);
        metaData.put("httpHeaders", getHeaders(ctx));
        return metaData;
    }

    private Map<String, List<String>> getHeaders(RoutingContext ctx) {
        Map<String, List<String>> h = new HashMap<>();
        MultiMap headers = ctx.request().headers();
        for (String header : headers.names()) {
            h.put(header, headers.getAll(header));
        }
        return h;
    }
}
