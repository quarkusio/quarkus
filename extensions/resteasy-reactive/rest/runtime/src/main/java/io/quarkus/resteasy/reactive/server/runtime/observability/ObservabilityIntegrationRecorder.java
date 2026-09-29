package io.quarkus.resteasy.reactive.server.runtime.observability;

import static io.quarkus.resteasy.reactive.server.runtime.observability.ObservabilityUtil.*;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.util.PathHelper;
import org.jboss.resteasy.reactive.server.core.Deployment;

import io.quarkus.resteasy.reactive.server.runtime.RuntimeResourceMatcher;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.security.AuthenticationException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class ObservabilityIntegrationRecorder {

    private static final Logger log = Logger.getLogger(ObservabilityIntegrationRecorder.class);

    /**
     * Returns a handler that sets the special property URI Template path needed by various observability integrations
     */
    public Handler<RoutingContext> preAuthFailureHandler(RuntimeValue<Deployment> deploymentRV) {
        return new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                if (shouldHandle(event)) {
                    try {
                        setTemplatePath(event, deploymentRV.getValue());
                    } catch (Exception e) {
                        log.debug("Unable to set template path for observability", e);
                    }
                }
                event.next();
            }

            private boolean shouldHandle(RoutingContext event) {
                if (!event.failed()) {
                    return false;
                }
                return event.failure() instanceof AuthenticationException
                        || event.failure() instanceof ForbiddenException
                        || event.failure() instanceof UnauthorizedException;
            }
        };
    }

    public static void setTemplatePath(RoutingContext rc, Deployment deployment) {
        RuntimeResourceMatcher.Match match = new RuntimeResourceMatcher(deployment)
                .match(getPathWithoutPrefix(rc, deployment), rc.request().method().name());
        if (match != null) {
            setUrlPathTemplate(rc, match.template());
        }
    }

    private static String getPathWithoutPrefix(RoutingContext rc, Deployment deployment) {
        return PathHelper.getPathWithoutPrefix(rc.normalizedPath(), deployment.getPrefix());
    }
}
