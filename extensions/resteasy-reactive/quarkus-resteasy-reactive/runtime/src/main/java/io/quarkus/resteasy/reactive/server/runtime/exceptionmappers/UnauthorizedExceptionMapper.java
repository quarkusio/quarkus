package io.quarkus.resteasy.reactive.server.runtime.exceptionmappers;

import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import io.quarkus.security.UnauthorizedException;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

public class UnauthorizedExceptionMapper {

    @ServerExceptionMapper(value = UnauthorizedException.class, priority = Priorities.USER + 1)
    public Uni<Response> handle(RoutingContext routingContext) {
        return SecurityExceptionMapperUtil.handleWithAuthenticator(routingContext);
    }
}
