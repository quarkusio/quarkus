package io.quarkus.rest.runtime.providers.exceptionmappers;

import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.spi.QuarkusRestExceptionMapper;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticator;
import io.vertx.ext.web.RoutingContext;

/**
 * TODO: We'll probably need to make QuarkusRestExceptionMapper work in an async manner as
 * this implementation blocks
 */
public class UnauthorizedExceptionMapper implements QuarkusRestExceptionMapper<UnauthorizedException> {

    private static final Logger log = Logger.getLogger(UnauthorizedExceptionMapper.class.getName());

    @Override
    public Response toResponse(UnauthorizedException exception) {
        return doToResponse(CDI.current().select(CurrentVertxRequest.class).get().getCurrent());
    }

    @Override
    public Response toResponse(UnauthorizedException exception, QuarkusRestRequestContext ctx) {
        return doToResponse(ctx.getContext());
    }

    private Response doToResponse(RoutingContext context) {
        if (context != null) {
            HttpAuthenticator authenticator = context.get(HttpAuthenticator.class.getName());
            if (authenticator != null) {
                ChallengeData challengeData = authenticator.getChallenge(context)
                        .await().indefinitely();
                if (challengeData != null) {
                    Response.ResponseBuilder status = Response.status(challengeData.status);
                    if (challengeData.headerName != null) {
                        status.header(challengeData.headerName.toString(), challengeData.headerContent);
                    }
                    return status.build();
                } else {
                    return Response.status(Response.Status.UNAUTHORIZED).build();
                }
            }
        }
        return Response.status(Response.Status.UNAUTHORIZED).entity("Not authorized").build();
    }
}
