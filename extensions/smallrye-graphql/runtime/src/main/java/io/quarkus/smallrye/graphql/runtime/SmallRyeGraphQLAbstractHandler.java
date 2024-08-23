package io.quarkus.smallrye.graphql.runtime;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonReaderFactory;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.ManagedContext;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.smallrye.graphql.execution.ExecutionService;
import io.smallrye.graphql.execution.context.SmallRyeContextManager;
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
    private final Handler<Void> currentManagedContextTerminationHandler;
    private final Handler<Throwable> currentManagedContextExceptionHandler;
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
        this.currentManagedContextTerminationHandler = createCurrentManagedContextTerminationHandler();
        this.currentManagedContextExceptionHandler = createCurrentManagedContextTerminationHandler();
    }

    private <T> Handler<T> createCurrentManagedContextTerminationHandler() {
        return new Handler<>() {
            @Override
            public void handle(Object e) {
                terminate();
            }
        };
    }

    @Override
    public void handle(final RoutingContext ctx) {

        ctx.response()
                .endHandler(currentManagedContextTerminationHandler)
                .exceptionHandler(currentManagedContextExceptionHandler)
                .closeHandler(currentManagedContextTerminationHandler);
        if (!currentManagedContext.isActive()) {
            currentManagedContext.activate();
        }
        try {
            handleWithIdentity(ctx);
            deactivate();
        } catch (Throwable t) {
            terminate();
            throw t;
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

    protected Map<String, Object> getMetaData(RoutingContext ctx) {
        // Add some context to dfe
        Map<String, Object> metaData = new ConcurrentHashMap<>();
        metaData.put("runBlocking", runBlocking);
        metaData.put("httpHeaders", getHeaders(ctx));
        InjectableContext.ContextState state = currentManagedContext.getState();
        metaData.put("state", state);
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

    private void deactivate() {
        SmallRyeContextManager.clearCurrentSmallRyeContext();
        currentManagedContext.deactivate();
    }

    private void terminate() {
        SmallRyeContextManager.clearCurrentSmallRyeContext();
        currentManagedContext.terminate();
    }
}
