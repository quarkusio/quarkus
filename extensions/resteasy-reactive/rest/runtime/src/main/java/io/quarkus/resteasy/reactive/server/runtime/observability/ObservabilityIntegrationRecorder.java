package io.quarkus.resteasy.reactive.server.runtime.observability;

import static io.quarkus.resteasy.reactive.server.runtime.observability.ObservabilityUtil.*;

import jakarta.ws.rs.HttpMethod;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.util.PathHelper;
import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.handlers.ClassRoutingHandler;
import org.jboss.resteasy.reactive.server.mapping.RequestMapper;

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
        // do what RestInitialHandler does
        var initMappers = new RequestMapper<>(deployment.getClassMappers());
        var requestMatch = initMappers.map(getPathWithoutPrefix(rc, deployment));
        if (requestMatch == null) {
            return;
        }
        var remaining = requestMatch.remaining.isEmpty() ? "/" : requestMatch.remaining;

        var serverRestHandlers = requestMatch.value.handlers;
        if (serverRestHandlers == null || serverRestHandlers.length < 1) {
            // nothing we can do
            return;
        }
        var firstHandler = serverRestHandlers[0];
        if (!(firstHandler instanceof ClassRoutingHandler)) {
            // nothing we can do
            return;
        }

        var classRoutingHandler = (ClassRoutingHandler) firstHandler;
        var mappers = classRoutingHandler.getMappers();

        var requestMethod = rc.request().method().name();

        // do what ClassRoutingHandler does
        var mapper = mappers.get(requestMethod);
        if (mapper == null) {
            if (requestMethod.equals(HttpMethod.HEAD) || requestMethod.equals(HttpMethod.OPTIONS)) {
                mapper = mappers.get(HttpMethod.GET);
            }
            if (mapper == null) {
                mapper = mappers.get(null);
            }
            if (mapper == null) {
                // can't match the path
                return;
            }
        }
        var target = mapper.map(remaining);
        if (target == null) {
            if (requestMethod.equals(HttpMethod.HEAD)) {
                mapper = mappers.get(HttpMethod.GET);
                if (mapper != null) {
                    target = mapper.map(remaining);
                }
            }

            if (target == null) {
                // can't match the path
                return;
            }
        }

        var templatePath = requestMatch.template.template + target.template.template;
        if (templatePath.endsWith("/")) {
            templatePath = templatePath.substring(0, templatePath.length() - 1);
        }

        setUrlPathTemplate(rc, templatePath);
    }

    private static String getPathWithoutPrefix(RoutingContext rc, Deployment deployment) {
        return PathHelper.getPathWithoutPrefix(rc.normalizedPath(), deployment.getPrefix());
    }
}
