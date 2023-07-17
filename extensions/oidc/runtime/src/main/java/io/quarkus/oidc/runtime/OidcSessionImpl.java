package io.quarkus.oidc.runtime;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.oidc.IdToken;
import io.quarkus.oidc.OidcSession;
import io.quarkus.oidc.OidcTenantConfig;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class OidcSessionImpl implements OidcSession {

    @Inject
    RoutingContext routingContext;

    @Inject
    DefaultTenantConfigResolver resolver;

    @Inject
    @IdToken
    JsonWebToken idToken;

    @Override
    public String getTenantId() {
        return routingContext.get(OidcUtils.TENANT_ID_ATTRIBUTE);
    }

    @Override
    public Uni<Void> logout() {
        String sessionCookieName = routingContext.get(OidcUtils.SESSION_COOKIE_NAME);
        if (sessionCookieName != null) {
            Uni<OidcTenantConfig> oidcConfigUni = resolver.resolveConfig(routingContext);
            return oidcConfigUni.onItem().transformToUni(new Function<OidcTenantConfig, Uni<? extends Void>>() {

                @Override
                public Uni<Void> apply(OidcTenantConfig oidcConfig) {
                    return OidcUtils.removeSessionCookie(routingContext, oidcConfig, sessionCookieName,
                            resolver.getTokenStateManager());
                }

            });
        }
        return Uni.createFrom().voidItem();
    }

    @Override
    public Instant expiresIn() {
        final long nowSecs = System.currentTimeMillis() / 1000;
        return Instant.ofEpochSecond(idToken.getExpirationTime() - nowSecs);
    }

    @Override
    public Instant expiresAt() {
        return Instant.ofEpochSecond(idToken.getExpirationTime());
    }

    @Override
    public Duration validFor() {
        final long nowSecs = System.currentTimeMillis() / 1000;
        return Duration.ofSeconds(idToken.getExpirationTime() - nowSecs);
    }

    @Override
    public JsonWebToken getIdToken() {
        return idToken;
    }
}
