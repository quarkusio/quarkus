package io.quarkus.smallrye.jwt.runtime.auth;

import static io.vertx.core.http.HttpHeaders.COOKIE;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import io.smallrye.jwt.auth.AbstractBearerTokenExtractor;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.Cookie;
import io.vertx.ext.web.RoutingContext;

/**
 * An AuthenticationMechanism that validates a caller based on a MicroProfile JWT bearer token
 */
@ApplicationScoped
public class JWTAuthMechanism implements HttpAuthenticationMechanism {
    protected static final String COOKIE_HEADER = "Cookie";
    protected static final String AUTHORIZATION_HEADER = "Authorization";
    protected static final String BEARER = "Bearer";

    @Inject
    private JWTAuthContextInfo authContextInfo;

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context,
            IdentityProviderManager identityProviderManager) {
        String jwtToken = new VertxBearerTokenExtractor(authContextInfo, context).getBearerToken();
        if (jwtToken != null) {
            context.put(HttpAuthenticationMechanism.class.getName(), this);
            return identityProviderManager
                    .authenticate(HttpSecurityUtils.setRoutingContextAttribute(
                            new TokenAuthenticationRequest(new JsonWebTokenCredential(jwtToken)), context));
        }
        return Uni.createFrom().optional(Optional.empty());
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        ChallengeData result = new ChallengeData(
                HttpResponseStatus.UNAUTHORIZED.code(),
                HttpHeaderNames.WWW_AUTHENTICATE,
                BEARER);
        return Uni.createFrom().item(result);
    }

    private static class VertxBearerTokenExtractor extends AbstractBearerTokenExtractor {
        private RoutingContext httpExchange;

        VertxBearerTokenExtractor(JWTAuthContextInfo authContextInfo, RoutingContext exchange) {
            super(authContextInfo);
            this.httpExchange = exchange;
        }

        @Override
        protected String getHeaderValue(String headerName) {
            return httpExchange.request().headers().get(headerName);
        }

        @Override
        protected String getCookieValue(String cookieName) {
            String cookieHeader = httpExchange.request().headers().get(COOKIE);

            if (cookieHeader != null && httpExchange.cookieCount() == 0) {
                Set<io.netty.handler.codec.http.cookie.Cookie> nettyCookies = ServerCookieDecoder.STRICT.decode(cookieHeader);
                for (io.netty.handler.codec.http.cookie.Cookie cookie : nettyCookies) {
                    if (cookie.name().equals(cookieName)) {
                        return cookie.value();
                    }
                }
            }
            Cookie cookie = httpExchange.getCookie(cookieName);
            return cookie != null ? cookie.getValue() : null;
        }
    }

    @Override
    public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
        return Collections.singleton(TokenAuthenticationRequest.class);
    }

    @Override
    public Uni<HttpCredentialTransport> getCredentialTransport(RoutingContext context) {
        final String tokenHeaderName = authContextInfo.getTokenHeader();
        if (COOKIE_HEADER.equals(tokenHeaderName)) {
            String tokenCookieName = authContextInfo.getTokenCookie();

            if (tokenCookieName == null) {
                tokenCookieName = BEARER;
            }
            return Uni.createFrom().item(new HttpCredentialTransport(HttpCredentialTransport.Type.COOKIE, tokenCookieName));
        } else if (AUTHORIZATION_HEADER.equals(tokenHeaderName)) {
            return Uni.createFrom().item(new HttpCredentialTransport(HttpCredentialTransport.Type.AUTHORIZATION, BEARER));
        } else {
            return Uni.createFrom()
                    .item(new HttpCredentialTransport(HttpCredentialTransport.Type.OTHER_HEADER, tokenHeaderName));
        }
    }
}
