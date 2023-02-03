package io.quarkus.resteasy.reactive.server.runtime.exceptionmappers;

import static org.jboss.resteasy.reactive.server.exceptionmappers.AsyncExceptionMappingUtil.DEFAULT_UNAUTHORIZED_RESPONSE;

import java.util.function.Function;

import jakarta.ws.rs.core.Response;

import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticator;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

final class SecurityExceptionMapperUtil {

    private SecurityExceptionMapperUtil() {
    }

    static Uni<Response> handleWithAuthenticator(RoutingContext routingContext) {
        HttpAuthenticator authenticator = routingContext.get(HttpAuthenticator.class.getName());
        if (authenticator != null) {
            Uni<ChallengeData> challenge = authenticator.getChallenge(routingContext);
            return challenge.map(new Function<ChallengeData, Response>() {
                @Override
                public Response apply(ChallengeData challengeData) {
                    if (challengeData == null) {
                        return DEFAULT_UNAUTHORIZED_RESPONSE;
                    }
                    Response.ResponseBuilder status = Response.status(challengeData.status);
                    if (challengeData.headerName != null) {
                        status.header(challengeData.headerName.toString(), challengeData.headerContent);
                    }
                    return status.build();
                }
            }).onFailure().recoverWithItem(DEFAULT_UNAUTHORIZED_RESPONSE);
        } else {
            return Uni.createFrom().item(DEFAULT_UNAUTHORIZED_RESPONSE);
        }
    }
}
