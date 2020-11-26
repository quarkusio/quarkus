package io.quarkus.rest.server.runtime.exceptionmappers;

import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveExceptionMapper;

import io.quarkus.rest.server.runtime.QuarkusResteasyReactiveRequestContext;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticator;
import io.vertx.ext.web.RoutingContext;

/**
 * TODO: We'll probably need to make ResteasyReactiveExceptionMapper work in an async manner as
 * this implementation blocks
 */
public class AuthenticationFailedExceptionMapper implements ResteasyReactiveExceptionMapper<AuthenticationFailedException> {

    @Override
    public Response toResponse(AuthenticationFailedException exception) {
        return doToResponse(CDI.current().select(CurrentVertxRequest.class).get().getCurrent());
    }

    @Override
    public Response toResponse(AuthenticationFailedException exception, ResteasyReactiveRequestContext ctx) {
        return doToResponse(((QuarkusResteasyReactiveRequestContext) ctx).getContext());
    }

    private Response doToResponse(RoutingContext routingContext) {
        if (routingContext != null) {
            HttpAuthenticator authenticator = routingContext.get(HttpAuthenticator.class.getName());
            if (authenticator != null) {
                ChallengeData challengeData = authenticator.getChallenge(routingContext)
                        .await().indefinitely();
                Response.ResponseBuilder status = Response.status(challengeData.status);
                if (challengeData.headerName != null) {
                    status.header(challengeData.headerName.toString(), challengeData.headerContent);
                }
                return status.build();
            }
        }
        return Response.status(Response.Status.UNAUTHORIZED).entity("Not Authenticated").build();
    }
}
