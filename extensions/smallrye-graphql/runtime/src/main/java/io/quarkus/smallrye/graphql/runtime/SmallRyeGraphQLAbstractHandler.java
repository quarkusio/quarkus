package io.quarkus.smallrye.graphql.runtime;

import java.io.StringReader;

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
import io.vertx.ext.web.RoutingContext;

/**
 * Handler that does the execution of GraphQL Requests
 */
public abstract class SmallRyeGraphQLAbstractHandler implements Handler<RoutingContext> {

    private final CurrentIdentityAssociation currentIdentityAssociation;
    private final CurrentVertxRequest currentVertxRequest;

    private volatile ExecutionService executionService;

    protected static final JsonReaderFactory jsonReaderFactory = Json.createReaderFactory(null);

    public SmallRyeGraphQLAbstractHandler(
            CurrentIdentityAssociation currentIdentityAssociation,
            CurrentVertxRequest currentVertxRequest) {

        this.currentIdentityAssociation = currentIdentityAssociation;
        this.currentVertxRequest = currentVertxRequest;
    }

    @Override
    public void handle(final RoutingContext ctx) {
        ManagedContext requestContext = Arc.container().requestContext();
        if (requestContext.isActive()) {
            handleWithIdentity(ctx);
        } else {
            try {
                requestContext.activate();
                handleWithIdentity(ctx);
            } finally {
                requestContext.terminate();
            }
        }
    }

    private void handleWithIdentity(final RoutingContext ctx) {
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
}
