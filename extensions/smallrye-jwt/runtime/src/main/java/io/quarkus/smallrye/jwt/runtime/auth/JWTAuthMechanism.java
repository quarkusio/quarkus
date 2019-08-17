package io.quarkus.smallrye.jwt.runtime.auth;

import static io.undertow.httpcore.HttpHeaderNames.WWW_AUTHENTICATE;
import static io.undertow.httpcore.StatusCodes.UNAUTHORIZED;

import javax.enterprise.inject.spi.CDI;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.smallrye.jwt.auth.AbstractBearerTokenExtractor;
import io.smallrye.jwt.auth.cdi.PrincipalProducer;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.undertow.UndertowLogger;
import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.SecurityContext;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.IdentityManager;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;

/**
 * An AuthenticationMechanism that validates a caller based on a MicroProfile JWT bearer token
 */
public class JWTAuthMechanism implements AuthenticationMechanism {

    private JWTAuthContextInfo authContextInfo;
    private IdentityManager identityManager;

    public JWTAuthMechanism(JWTAuthContextInfo authContextInfo, IdentityManager identityManager) {
        this.authContextInfo = authContextInfo;
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
        String jwtToken = new UndertowBearerTokenExtractor(authContextInfo, exchange).getBearerToken();
        if (jwtToken != null) {
            try {
                JWTCredential credential = new JWTCredential(jwtToken, authContextInfo);
                if (UndertowLogger.SECURITY_LOGGER.isTraceEnabled()) {
                    UndertowLogger.SECURITY_LOGGER.tracef("Bearer token: %s", jwtToken);
                }
                // Install the JWT principal as the caller
                Account account = identityManager.verify(credential.getName(), credential);
                if (account != null) {
                    preparePrincipalProducer((JsonWebToken) account.getPrincipal());
                    securityContext.authenticationComplete(account, "MP-JWT", false);
                    UndertowLogger.SECURITY_LOGGER.debugf("Authenticated caller(%s) for path(%s) with roles: %s",
                            credential.getName(), exchange.getRequestPath(), account.getRoles());
                    return AuthenticationMechanismOutcome.AUTHENTICATED;
                } else {
                    UndertowLogger.SECURITY_LOGGER.info("Failed to authenticate JWT bearer token");
                    return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
                }
            } catch (Exception e) {
                UndertowLogger.SECURITY_LOGGER.infof(e, "Failed to validate JWT bearer token");
                return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
            }
        }

        // No suitable header has been found in this request,
        return AuthenticationMechanismOutcome.NOT_ATTEMPTED;
    }

    private void preparePrincipalProducer(JsonWebToken jwtPrincipal) {
        PrincipalProducer principalProducer = CDI.current().select(PrincipalProducer.class).get();
        principalProducer.setJsonWebToken(jwtPrincipal);
    }

    @Override
    public ChallengeResult sendChallenge(HttpServerExchange exchange, SecurityContext securityContext) {
        exchange.addResponseHeader(WWW_AUTHENTICATE, "Bearer {token}");
        UndertowLogger.SECURITY_LOGGER.debugf("Sending Bearer {token} challenge for %s", exchange);
        return new ChallengeResult(true, UNAUTHORIZED);
    }

    private static class UndertowBearerTokenExtractor extends AbstractBearerTokenExtractor {
        private HttpServerExchange httpExchange;

        UndertowBearerTokenExtractor(JWTAuthContextInfo authContextInfo, HttpServerExchange exchange) {
            super(authContextInfo);
            this.httpExchange = exchange;
        }

        @Override
        protected String getHeaderValue(String headerName) {
            return httpExchange.getRequestHeader(headerName);
        }

        @Override
        protected String getCookieValue(String cookieName) {
            Cookie cookie = httpExchange.getRequestCookies().get(cookieName);
            return cookie != null ? cookie.getValue() : null;
        }
    }
}
