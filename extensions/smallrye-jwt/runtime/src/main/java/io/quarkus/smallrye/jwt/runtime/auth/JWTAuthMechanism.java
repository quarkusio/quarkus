package io.quarkus.smallrye.jwt.runtime.auth;

import static io.vertx.core.http.HttpHeaders.COOKIE;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.vertx.http.runtime.security.HTTPAuthenticationMechanism;
import io.smallrye.jwt.auth.AbstractBearerTokenExtractor;
import io.smallrye.jwt.auth.cdi.PrincipalProducer;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.RoutingContext;

/**
 * An AuthenticationMechanism that validates a caller based on a MicroProfile JWT bearer token
 */
@ApplicationScoped
public class JWTAuthMechanism implements HTTPAuthenticationMechanism {

    @Inject
    private JWTAuthContextInfo authContextInfo;

    private void preparePrincipalProducer(JsonWebToken jwtPrincipal) {
        PrincipalProducer principalProducer = CDI.current().select(PrincipalProducer.class).get();
        principalProducer.setJsonWebToken(jwtPrincipal);
    }

    @Override
    public CompletionStage<SecurityIdentity> authenticate(RoutingContext context,
            IdentityProviderManager identityProviderManager) {
        String jwtToken = new VertxBearerTokenExtractor(authContextInfo, context).getBearerToken();
        if (jwtToken != null) {
            return identityProviderManager
                    .authenticate(new TokenAuthenticationRequest(new TokenCredential(jwtToken, "bearer")));
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Boolean> sendChallenge(RoutingContext context) {
        context.response().headers().set(HttpHeaderNames.WWW_AUTHENTICATE, "Bearer {token}");
        context.response().setStatusCode(HttpResponseStatus.UNAUTHORIZED.code());
        return CompletableFuture.completedFuture(true);
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
}
