package io.quarkus.smallrye.graphql.runtime.spi.datafetcher;

import graphql.schema.DataFetchingEnvironment;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.ManagedContext;

public final class RequestContextHelper {

    private RequestContextHelper() {
    }

    public static void reactivate(ManagedContext requestContext, DataFetchingEnvironment dfe) {
        if (!requestContext.isActive()) {
            Object maybeState = dfe.getGraphQlContext().getOrDefault("state", null);
            if (maybeState != null) {
                requestContext.activate((InjectableContext.ContextState) maybeState);
            } else {
                requestContext.activate();
            }
        }
    }
}
