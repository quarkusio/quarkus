package io.quarkus.vertx.http.security.token;

import java.time.Duration;

import io.quarkus.arc.Arc;
import io.quarkus.security.credential.PasswordCredential;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

/**
 * Stores generated one-time authentication token and authenticates {@link SecurityIdentity}
 * based on one-time authentication token. Must be implemented as a {@link jakarta.enterprise.context.ApplicationScoped}
 * or {@link jakarta.inject.Singleton} CDI bean.
 */
public interface OneTimeTokenAuthenticator extends IdentityProvider<OneTimeTokenAuthenticationRequest> {

    /**
     * Event data key that the authenticator can optionally set to the one-time authentication token request
     * absolute URL if redirection to the page where user was before authentication has been triggered is enabled.
     */
    String REDIRECT_LOCATION_KEY = "io.quarkus.security.spi.runtime.otac#REDIRECT_LOCATION";

    /**
     * Stores generated one-time authentication token.
     * When used in a production, this method encrypts one-time authentication token before it is stored.
     * Only one one-time authentication token is allowed per user, if user already had generated the one-time
     * authentication token, invocation of this method must replace previous authentication token with a new one.
     *
     * @param securityIdentity {@link SecurityIdentity}
     * @param oneTimeTokenCredential one-time authentication token credential
     * @param requestInfo contextual information about request to store the token
     * @return Uni<Void>; never null
     */
    Uni<Void> store(SecurityIdentity securityIdentity, PasswordCredential oneTimeTokenCredential, RequestInfo requestInfo);

    /**
     * Authenticates incoming request based on passed one-time authentication token. Expired one-time authentication tokens
     * must result in authentication failures.
     *
     * @param oneTimeTokenAuthenticationRequest authentication request with one-time authentication token credential
     * @param authenticationRequestContext authentication request context; simplifies blocking operations
     * @return SecurityIdentity for which the one-time authentication token was generated
     */
    Uni<SecurityIdentity> authenticate(OneTimeTokenAuthenticationRequest oneTimeTokenAuthenticationRequest,
            AuthenticationRequestContext authenticationRequestContext);

    /**
     * Provides contextual information about incoming request to store one-time authentication token.
     *
     * @param redirectLocation absolute URL from which request to generate token arrived; nullable
     * @param expiresIn token expiration; never null
     */
    record RequestInfo(String redirectLocation, Duration expiresIn) {
    }

    /**
     * Creates {@link OneTimeTokenAuthenticator} which stores all the generated one-time tokens in memory.
     * If you run your Quarkus application in multiple instances, this implementation is not suitable for you.
     *
     * @return OneTimeTokenAuthenticator
     */
    static OneTimeTokenAuthenticator createInMemoryAuthenticator() {
        var identityProviderManager = Arc.container().instance(IdentityProviderManager.class).get();
        var vertx = Arc.container().instance(Vertx.class).get();
        return new OneTimeTokenInMemoryAuthenticator(identityProviderManager, vertx);
    }
}
