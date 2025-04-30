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

    static Uni<Response> handleWithAuthenticator(RoutingContext routingContext, String exceptionMessage) {
        HttpAuthenticator authenticator = routingContext.get(HttpAuthenticator.class.getName());
        if (authenticator != null) {
            Uni<ChallengeData> challenge = authenticator.getChallenge(routingContext);
            return challenge.map(new Function<ChallengeData, Response>() {
                @Override
                public Response apply(ChallengeData challengeData) {
                    if (challengeData == null) {
                        return exceptionMessage != null ? createResponse(exceptionMessage) : DEFAULT_UNAUTHORIZED_RESPONSE;
                    }
                    Response.ResponseBuilder responseBuilder = Response.status(challengeData.status);
                    if (challengeData.headerName != null) {
                        responseBuilder.header(challengeData.headerName.toString(), challengeData.headerContent);
                    }
                    if (exceptionMessage != null && challengeData.status == 401) {
                        responseBuilder.entity(exceptionMessage);
                    }
                    return responseBuilder.build();
                }
            }).onFailure().recoverWithItem(
                    exceptionMessage != null ? createResponse(exceptionMessage) : DEFAULT_UNAUTHORIZED_RESPONSE);
        } else {
            return Uni.createFrom()
                    .item(exceptionMessage != null ? createResponse(exceptionMessage) : DEFAULT_UNAUTHORIZED_RESPONSE);
        }
    }

    private static Response createResponse(String responseBody) {
        return Response.status(Response.Status.UNAUTHORIZED).entity(responseBody).build();
    }
}
