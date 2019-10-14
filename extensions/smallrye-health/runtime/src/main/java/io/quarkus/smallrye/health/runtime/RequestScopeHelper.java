package io.quarkus.smallrye.health.runtime;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;

public class RequestScopeHelper {

    /**
     * Activates the request scope is not yet activated.
     *
     * @return {@code true} if activated by this method, {@code false} if already activated.
     */
    static boolean activeRequestScope() {
        ManagedContext context = Arc.container().requestContext();
        if (!context.isActive()) {
            context.activate();
            return true;
        }
        return false;
    }

    private RequestScopeHelper() {
        // avoid direct instantiation
    }
}
