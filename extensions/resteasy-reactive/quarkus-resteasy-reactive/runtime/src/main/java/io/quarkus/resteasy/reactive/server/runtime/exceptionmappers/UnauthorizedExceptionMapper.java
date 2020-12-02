package io.quarkus.resteasy.reactive.server.runtime.exceptionmappers;

import javax.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import io.quarkus.security.UnauthorizedException;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

public class UnauthorizedExceptionMapper {

    @ServerExceptionMapper(UnauthorizedException.class)
    public Uni<Response> handle(RoutingContext routingContext) {
        return SecurityExceptionMapperUtil.handleWithAuthenticator(routingContext);
    }
}
