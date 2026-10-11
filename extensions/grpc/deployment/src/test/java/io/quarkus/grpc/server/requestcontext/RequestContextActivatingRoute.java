package io.quarkus.grpc.server.requestcontext;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.vertx.ext.web.Router;

/**
 * Activates the request context before the gRPC handler of the unified server runs, the way a reactive route filter
 * that touches a request scoped bean does.
 */
@ApplicationScoped
public class RequestContextActivatingRoute {

    void init(@Observes Router router) {
        router.route().order(-1000).handler(context -> {
            ManagedContext requestContext = Arc.container().requestContext();
            if (!requestContext.isActive()) {
                requestContext.activate();
                context.addEndHandler(ignored -> requestContext.terminate());
            }
            context.next();
        });
    }
}
