package io.quarkus.elytron.security.oauth2.runtime.auth;

import java.util.Collections;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * An AuthenticationMechanism that validates a caller based on a bearer token
 */
@ApplicationScoped
public class OAuth2AuthMechanism implements HttpAuthenticationMechanism {

    protected static final ChallengeData CHALLENGE_DATA = new ChallengeData(
            HttpResponseStatus.UNAUTHORIZED.code(),
            HttpHeaderNames.WWW_AUTHENTICATE,
            "Bearer {token}");

    /**
     * Extract the Authorization header and validate the bearer token if it exists. If it does, and is validated, this
     * builds the org.jboss.security.SecurityContext authenticated Subject that drives the container APIs as well as
     * the authorization layers.
     *
     * @param context - the http request exchange object
     * @param identityProviderManager - the current security context that
     * @return one of AUTHENTICATED, NOT_AUTHENTICATED or NOT_ATTEMPTED depending on the header and authentication outcome.
     */
    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context,
            IdentityProviderManager identityProviderManager) {
        String authHeader = context.request().headers().get("Authorization");
        String bearerToken = authHeader != null ? authHeader.substring(7) : null;
        if (bearerToken != null) {
            // Install the OAuth2 principal as the caller
            return identityProviderManager
                    .authenticate(new TokenAuthenticationRequest(new TokenCredential(bearerToken, "bearer")));

        }
        // No suitable header has been found in this request,
        return Uni.createFrom().nullItem();
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        return Uni.createFrom().item(CHALLENGE_DATA);
    }

    @Override
    public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
        return Collections.singleton(TokenAuthenticationRequest.class);
    }

    @Override
    public HttpCredentialTransport getCredentialTransport() {
        return new HttpCredentialTransport(HttpCredentialTransport.Type.AUTHORIZATION, "bearer");
    }
}
