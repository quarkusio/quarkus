package io.quarkus.oidc;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.vertx.http.runtime.security.HTTPAuthenticationMechanism;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class VertxOAuth2AuthenticationMechanism implements HTTPAuthenticationMechanism {

    private static final String BEARER = "Bearer";

    private volatile String authServerURI;
    private volatile OAuth2Auth auth;
    private volatile OidcConfig config;

    public OidcConfig getConfig() {
        return config;
    }

    public VertxOAuth2AuthenticationMechanism setConfig(OidcConfig config) {
        this.config = config;
        return this;
    }

    public String getAuthServerURI() {
        return authServerURI;
    }

    public VertxOAuth2AuthenticationMechanism setAuthServerURI(String authServerURI) {
        this.authServerURI = authServerURI;
        return this;
    }

    public OAuth2Auth getAuth() {
        return auth;
    }

    public VertxOAuth2AuthenticationMechanism setAuth(OAuth2Auth auth) {
        this.auth = auth;
        return this;
    }

    @Override
    public CompletionStage<SecurityIdentity> authenticate(RoutingContext context,
            IdentityProviderManager identityProviderManager) {
        // when the handler is working as bearer only, then the `Authorization` header is required

        final HttpServerRequest request = context.request();
        final String authorization = request.headers().get(HttpHeaders.AUTHORIZATION);

        if (authorization == null) {
            return CompletableFuture.completedFuture(null);
        }

        int idx = authorization.indexOf(' ');

        if (idx <= 0) {
            return CompletableFuture.completedFuture(null);
        }

        if (!BEARER.equalsIgnoreCase(authorization.substring(0, idx))) {
            return CompletableFuture.completedFuture(null);
        }

        String token = authorization.substring(idx + 1);
        return identityProviderManager.authenticate(new TokenAuthenticationRequest(new TokenCredential(token, BEARER)));
    }

    @Override
    public CompletionStage<Boolean> sendChallenge(RoutingContext context) {
        context.response().setStatusCode(302);
        context.response().headers().set(HttpHeaders.LOCATION, authURI(authServerURI));
        return CompletableFuture.completedFuture(true);
    }

    private String authURI(String redirectURL) {
        final JsonObject config = new JsonObject()
                .put("state", redirectURL);

        config.put("redirect_uri", authServerURI);

        //        if (extraParams != null) {
        //            config.mergeIn(extraParams);
        //        }
        //
        //        if (scopes.size() > 0) {
        //            JsonArray _scopes = new JsonArray();
        //            // scopes are passed as an array because the auth provider has the knowledge on how to encode them
        //            for (String authority : scopes) {
        //                _scopes.add(authority);
        //            }
        //
        //            config.put("scopes", _scopes);
        //        }

        return auth.authorizeURL(config);
    }
}
