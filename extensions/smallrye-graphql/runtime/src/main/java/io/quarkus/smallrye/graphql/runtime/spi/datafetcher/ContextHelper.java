package io.quarkus.smallrye.graphql.runtime.spi.datafetcher;

import java.util.List;
import java.util.Map;

import graphql.schema.DataFetchingEnvironment;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.ManagedContext;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.graphql.execution.context.SmallRyeContext;
import io.smallrye.graphql.execution.context.SmallRyeContextManager;
import io.smallrye.graphql.schema.model.Execute;
import io.smallrye.graphql.schema.model.Operation;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;

public class ContextHelper {

    private static final String KEY_CONTEXT_STATE = "requestContextState";
    private static final String KEY_SECURITY_IDENTITY = "securityIdentity";
    private static final String KEY_HTTP_HEADERS = "httpHeaders";

    @SuppressWarnings("unchecked")
    public static Map<String, List<String>> getHeaders() {
        SmallRyeContext smallRyeContext = SmallRyeContextManager.getCurrentSmallRyeContext();
        DataFetchingEnvironment dfe = smallRyeContext.unwrap(DataFetchingEnvironment.class);
        return (Map<String, List<String>>) dfe.getGraphQlContext().get(KEY_HTTP_HEADERS);
    }

    public static InjectableContext.ContextState getActiveState(DataFetchingEnvironment dfe) {
        final ManagedContext requestContext = Arc.container().requestContext();
        if (!requestContext.isActive()) {
            // Re-activate CDI State
            InjectableContext.ContextState storedContextState = dfe.getGraphQlContext().get(KEY_CONTEXT_STATE);
            requestContext.activate(storedContextState);

            // Re-activate security
            Uni<SecurityIdentity> storedDeferedIdentity = dfe.getGraphQlContext().get(KEY_SECURITY_IDENTITY);
            CurrentIdentityAssociation currentIdentityAssociation = Arc.container().select(CurrentIdentityAssociation.class)
                    .get();
            currentIdentityAssociation.setIdentity(storedDeferedIdentity);
        }

        return requestContext.getState();
    }

    public static void storeActiveState(DataFetchingEnvironment dfe, InjectableContext.ContextState contextState) {
        dfe.getGraphQlContext().put(KEY_CONTEXT_STATE, contextState);
    }

    public static void storeActiveState(Map<String, Object> metaData,
            InjectableContext.ContextState contextState,
            Uni<SecurityIdentity> deferedIdentity,
            Map<String, List<String>> headers) {

        metaData.put(KEY_CONTEXT_STATE, contextState);
        metaData.put(KEY_SECURITY_IDENTITY, deferedIdentity);
        metaData.put(KEY_HTTP_HEADERS, headers);
    }

    public static boolean blockingShouldExecuteNonBlocking(Operation operation, Context vc) {
        // Rule is that by default this should execute blocking except if marked as non blocking)
        return operation.getExecute().equals(Execute.NON_BLOCKING);
    }

    public static boolean nonBlockingShouldExecuteBlocking(Operation operation, Context vc) {
        // Rule is that by default this should execute non-blocking except if marked as blocking)
        return operation.getExecute().equals(Execute.BLOCKING) && vc.isEventLoopContext();
    }

    public static boolean shouldReactivateRequestContext(final ManagedContext requestContext,
            final InjectableContext.ContextState requestContextState) {
        return !requestContext.isActive() || !requestContext.getState().equals(requestContextState);
    }

}
