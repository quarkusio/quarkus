package io.quarkus.oidc;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.auth.oauth2.OAuth2Auth;

@ApplicationScoped
public class VertxOAuth2IdentityProvider implements IdentityProvider<TokenAuthenticationRequest> {

    private volatile OAuth2Auth auth;

    public OAuth2Auth getAuth() {
        return auth;
    }

    public VertxOAuth2IdentityProvider setAuth(OAuth2Auth auth) {
        this.auth = auth;
        return this;
    }

    @Override
    public Class<TokenAuthenticationRequest> getRequestType() {
        return TokenAuthenticationRequest.class;
    }

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

                //this is not great, we are re-parsing
                //this needs some love from someone who knows smallrye JWT better to avoid re-parsing
                try {
                    JwtClaims jwtClaims = JwtClaims.parse(token.accessToken().encode());

                    String username = token.principal().getString("username");
                    builder.setPrincipal(new VertxJwtCallerPrincipal(username, jwtClaims));
                } catch (InvalidJwtException e) {
                    result.completeExceptionally(e);
                    return;
                }

                JsonObject realmAccess = token.accessToken().getJsonObject("realm_access");
                if (realmAccess != null) {
                    JsonArray roles = realmAccess.getJsonArray("roles");
                    if (roles != null) {
                        for (Object authority : roles) {
                            builder.addRole(authority.toString());
                        }
                    }
                }
                builder.addCredential(request.getToken());
                result.complete(builder.build());
            }
        });

        return result;
    }
}
