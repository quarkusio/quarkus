package io.quarkus.resteasy.runtime;

import java.util.concurrent.ExecutionException;

import javax.annotation.Priority;
import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

import io.quarkus.security.UnauthorizedException;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticator;
import io.vertx.ext.web.RoutingContext;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 05/10/2019
 */
@Provider
@Priority(Priorities.USER + 1)
public class UnauthorizedExceptionMapper implements ExceptionMapper<UnauthorizedException> {

    private static final Logger log = Logger.getLogger(UnauthorizedExceptionMapper.class.getName());

    private volatile CurrentVertxRequest currentVertxRequest;

    CurrentVertxRequest currentVertxRequest() {
        if (currentVertxRequest == null) {
            currentVertxRequest = CDI.current().select(CurrentVertxRequest.class).get();
        }
        return currentVertxRequest;
    }

    @Override
    public Response toResponse(UnauthorizedException exception) {
        RoutingContext context = currentVertxRequest().getCurrent();
        if (context != null) {
            HttpAuthenticator authenticator = context.get(HttpAuthenticator.class.getName());
            if (authenticator != null) {
                try {
                    ChallengeData challengeData = authenticator.getChallenge(context)
                            .toCompletableFuture()
                            .get();
                    Response.ResponseBuilder status = Response.status(challengeData.status);
                    if (challengeData.headerName != null) {
                        status.header(challengeData.headerName.toString(), challengeData.headerContent);
                    }
                    return status.build();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Failed to read challenge data for unauthorized response", e);
                    return Response.status(401).entity("Not authorized").build();
                }
            }
        }
        return Response.status(401).entity("Not authorized").build();
    }
}
