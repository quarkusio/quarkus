package io.quarkus.resteasy.reactive.server.runtime.exceptionmappers;

import static io.quarkus.vertx.http.runtime.security.HttpSecurityUtils.addAuthenticationFailureToEvent;

import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.security.AuthenticationFailedException;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

public class AuthenticationFailedExceptionMapper {

    @ServerExceptionMapper(value = AuthenticationFailedException.class, priority = Priorities.USER + 1)
    public Uni<Response> handle(RoutingContext routingContext, AuthenticationFailedException exception) {
        addAuthenticationFailureToEvent(exception, routingContext);
        return SecurityExceptionMapperUtil.handleWithAuthenticator(routingContext,
                LaunchMode.isDev() ? exception.getMessage() : null);
    }
}
