package io.quarkus.resteasy.runtime;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticator;
import io.vertx.ext.web.RoutingContext;

@Provider
@Priority(Priorities.USER + 1)
public class AuthenticationFailedExceptionMapper implements ExceptionMapper<AuthenticationFailedException> {
    private static final Logger log = Logger.getLogger(AuthenticationFailedExceptionMapper.class.getName());

    private volatile CurrentVertxRequest currentVertxRequest;

    CurrentVertxRequest currentVertxRequest() {
        if (currentVertxRequest == null) {
            currentVertxRequest = CDI.current().select(CurrentVertxRequest.class).get();
        }
        return currentVertxRequest;
    }

    @Override
    public Response toResponse(AuthenticationFailedException exception) {
        RoutingContext context = currentVertxRequest().getCurrent();
        if (context != null) {
            HttpAuthenticator authenticator = context.get(HttpAuthenticator.class.getName());
            if (authenticator != null) {
                ChallengeData challengeData = authenticator.getChallenge(context)
                        .await().indefinitely();
                Response.ResponseBuilder status = Response.status(challengeData.status);
                if (challengeData.headerName != null) {
                    status.header(challengeData.headerName.toString(), challengeData.headerContent);
                }
                log.debugf("Returning an authentication challenge, status code: %d", challengeData.status);
                return status.build();
            } else {
                log.error("HttpAuthenticator is not found, returning HTTP status 401");
            }
        } else {
            log.error("RoutingContext is not found, returning HTTP status 401");
        }
        return Response.status(401).entity("Not Authenticated").build();
    }
}
