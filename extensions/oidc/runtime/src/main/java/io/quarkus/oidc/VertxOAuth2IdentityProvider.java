package io.quarkus.oidc;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;

import io.quarkus.oidc.runtime.OidcUtils;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.auth.oauth2.OAuth2Auth;

@ApplicationScoped
public class VertxOAuth2IdentityProvider implements IdentityProvider<TokenAuthenticationRequest> {

    private volatile OAuth2Auth auth;
    private volatile OidcConfig config;

    public OAuth2Auth getAuth() {
        return auth;
    }

    public VertxOAuth2IdentityProvider setAuth(OAuth2Auth auth) {
        this.auth = auth;
        return this;
    }

    public VertxOAuth2IdentityProvider setConfig(OidcConfig config) {
        this.config = config;
        return this;
    }

    @Override
    public Class<TokenAuthenticationRequest> getRequestType() {
        return TokenAuthenticationRequest.class;
    }

    @SuppressWarnings("deprecation")
    @Override
    public CompletionStage<SecurityIdentity> authenticate(TokenAuthenticationRequest request,
            AuthenticationRequestContext context) {
        CompletableFuture<SecurityIdentity> result = new CompletableFuture<>();
        auth.decodeToken(request.getToken().getToken(), new Handler<AsyncResult<AccessToken>>() {
            @Override
            public void handle(AsyncResult<AccessToken> event) {
                if (event.failed()) {
                    result.completeExceptionally(event.cause());
                    return;
                }
                AccessToken token = event.result();
                QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();

                JsonWebToken jwtPrincipal = null;
                try {
                    jwtPrincipal = new VertxJwtCallerPrincipal(JwtClaims.parse(token.accessToken().encode()));
                } catch (InvalidJwtException e) {
                    result.completeExceptionally(e);
                    return;
                }
                builder.setPrincipal(jwtPrincipal);
                try {
                    String clientId = config.getClientId().isPresent() ? config.getClientId().get() : null;
                    for (String role : OidcUtils.findRoles(clientId, config.getRoles(), token.accessToken())) {
                        builder.addRole(role);
                    }
                } catch (Exception e) {
                    result.completeExceptionally(e);
                    return;
                }

                builder.addCredential(request.getToken());
                result.complete(builder.build());
            }
        });

        return result;
    }

}
