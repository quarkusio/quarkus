package io.quarkus.elytron.security.oauth2.runtime.auth;

import io.undertow.UndertowLogger;
import io.undertow.httpcore.HttpHeaderNames;
import io.undertow.httpcore.StatusCodes;
import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.SecurityContext;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.IdentityManager;
import io.undertow.server.HttpServerExchange;

/**
 * An AuthenticationMechanism that validates a caller based on a bearer token
 */
public class OAuth2AuthMechanism implements AuthenticationMechanism {

    private IdentityManager identityManager;

    public OAuth2AuthMechanism(IdentityManager identityManager) {
        this.identityManager = identityManager;
    }

    /**
     * Extract the Authorization header and validate the bearer token if it exists. If it does, and is validated, this
     * builds the org.jboss.security.SecurityContext authenticated Subject that drives the container APIs as well as
     * the authorization layers.
     *
     * @param exchange - the http request exchange object
     * @param securityContext - the current security context that
     * @return one of AUTHENTICATED, NOT_AUTHENTICATED or NOT_ATTEMPTED depending on the header and authentication outcome.
     */
    @Override
    public AuthenticationMechanismOutcome authenticate(HttpServerExchange exchange, SecurityContext securityContext) {
        String authHeader = exchange.getRequestHeader("Authorization");
        String bearerToken = authHeader != null ? authHeader.substring(7) : null;
        if (bearerToken != null) {
            try {
                Oauth2Credential credential = new Oauth2Credential(bearerToken);
                if (UndertowLogger.SECURITY_LOGGER.isTraceEnabled()) {
                    UndertowLogger.SECURITY_LOGGER.tracef("Bearer token: %s", credential);
                }
                // Install the OAuth2 principal as the caller
                Account account = identityManager.verify(credential);
                if (account != null) {
                    securityContext.authenticationComplete(account, "BEARER_TOKEN", false);
                    UndertowLogger.SECURITY_LOGGER.debugf("Authenticated credential(%s) for path(%s) with roles: %s",
                            credential, exchange.getRequestPath(), account.getRoles());
                    return AuthenticationMechanismOutcome.AUTHENTICATED;
                } else {
                    UndertowLogger.SECURITY_LOGGER.info("Failed to authenticate OAuth2 bearer token");
                    return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
                }
            } catch (Exception e) {
                UndertowLogger.SECURITY_LOGGER.infof(e, "Failed to validate OAuth2 bearer token");
                return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
            }
        }

        // No suitable header has been found in this request,
        return AuthenticationMechanismOutcome.NOT_ATTEMPTED;
    }

    @Override
    public ChallengeResult sendChallenge(HttpServerExchange exchange, SecurityContext securityContext) {
        exchange.addResponseHeader(HttpHeaderNames.WWW_AUTHENTICATE, "Bearer {token}");
        UndertowLogger.SECURITY_LOGGER.debugf("Sending Bearer {token} challenge for %s", exchange);
        return new ChallengeResult(true, StatusCodes.UNAUTHORIZED);
    }
}
