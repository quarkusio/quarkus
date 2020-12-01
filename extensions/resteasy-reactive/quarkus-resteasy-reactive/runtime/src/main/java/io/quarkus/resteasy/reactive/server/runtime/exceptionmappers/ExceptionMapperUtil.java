package io.quarkus.resteasy.reactive.server.runtime.exceptionmappers;

import java.util.function.Consumer;

import javax.ws.rs.core.Response;

import io.quarkus.resteasy.reactive.server.runtime.QuarkusResteasyReactiveRequestContext;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticator;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

final class ExceptionMapperUtil {

    private static final Response DEFAULT_UNAUTHORIZED_RESPONSE = Response.status(Response.Status.UNAUTHORIZED)
            .entity("Not Authenticated").build();

    static void responseFromAuthenticator(QuarkusResteasyReactiveRequestContext context) {
        RoutingContext routingContext = context.getContext();
        HttpAuthenticator authenticator = routingContext.get(HttpAuthenticator.class.getName());
        if (authenticator != null) {
            Uni<ChallengeData> challenge = authenticator.getChallenge(routingContext);
            context.suspend();
            challenge.subscribe().with(new Consumer<ChallengeData>() {
                @Override
                public void accept(ChallengeData challengeData) {
                    if (challengeData == null) {
                        context.setResult(DEFAULT_UNAUTHORIZED_RESPONSE);
                        context.resume();
                        return;
                    }
                    Response.ResponseBuilder status = Response.status(challengeData.status);
                    if (challengeData.headerName != null) {
                        status.header(challengeData.headerName.toString(), challengeData.headerContent);
                    }
                    context.setResult(status.build());
                    context.resume();
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) {
                    context.setResult(DEFAULT_UNAUTHORIZED_RESPONSE);
                    context.resume();
                }
            });
        } else {
            context.setResult(DEFAULT_UNAUTHORIZED_RESPONSE);
        }

    }
}
