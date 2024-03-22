package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.vertx.http.runtime.security.BasicAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * Just like {@link BasicAuthenticationMechanism}, but only challenge on demand, for when
 * {@link io.quarkus.oidc.runtime.CodeAuthenticationMechanism} is not selected explicitly, it can happen
 * that challenge is send to {@link BasicAuthenticationMechanism}.
 */
@ApplicationScoped
public class CustomBasicHttpAuthMechanism extends BasicAuthenticationMechanism {
    public CustomBasicHttpAuthMechanism() {
        super(null, false);
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        if (context.request().getHeader("custom") != null) {
            return super.getChallenge(context);
        }
        return Uni.createFrom().nullItem();
    }
}
