package io.quarkus.resteasy.reactive.server.runtime.exceptionmappers;

import static io.quarkus.resteasy.reactive.server.runtime.exceptionmappers.ExceptionMapperUtil.responseFromAuthenticator;

import javax.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.spi.AsyncExceptionMapperContext;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveAsyncExceptionMapper;

import io.quarkus.resteasy.reactive.server.runtime.QuarkusResteasyReactiveRequestContext;
import io.quarkus.security.AuthenticationFailedException;

public class AuthenticationFailedExceptionMapper
        implements ResteasyReactiveAsyncExceptionMapper<AuthenticationFailedException> {

    @Override
    public Response toResponse(AuthenticationFailedException exception) {
        throw new IllegalStateException("This should never have been called");
    }

    @Override
    public void asyncResponse(AuthenticationFailedException exception, AsyncExceptionMapperContext ctx) {
        responseFromAuthenticator((QuarkusResteasyReactiveRequestContext) ctx.serverRequestContext());
    }
}
