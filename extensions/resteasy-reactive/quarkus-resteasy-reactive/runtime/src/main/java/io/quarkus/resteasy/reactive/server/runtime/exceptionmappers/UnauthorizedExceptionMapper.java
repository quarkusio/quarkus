package io.quarkus.resteasy.reactive.server.runtime.exceptionmappers;

import static io.quarkus.resteasy.reactive.server.runtime.exceptionmappers.ExceptionMapperUtil.responseFromAuthenticator;

import javax.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.spi.AsyncExceptionMapperContext;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveAsyncExceptionMapper;

import io.quarkus.resteasy.reactive.server.runtime.QuarkusResteasyReactiveRequestContext;
import io.quarkus.security.UnauthorizedException;

public class UnauthorizedExceptionMapper implements ResteasyReactiveAsyncExceptionMapper<UnauthorizedException> {

    @Override
    public Response toResponse(UnauthorizedException exception) {
        throw new IllegalStateException("This should never have been called");
    }

    @Override
    public void asyncResponse(UnauthorizedException exception, AsyncExceptionMapperContext ctx) {
        responseFromAuthenticator((QuarkusResteasyReactiveRequestContext) ctx.serverRequestContext());
    }
}
