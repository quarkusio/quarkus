package io.quarkus.oidc.runtime;

import java.time.Duration;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.quarkus.oidc.AuthorizationCodeTokens;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.impl.jose.JWT;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.auth.oauth2.impl.AccessTokenImpl;
import io.vertx.ext.auth.oauth2.impl.OAuth2AuthProviderImpl;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;
import io.vertx.ext.web.RoutingContext;

public class OidcRuntimeClient {
    final OAuth2Auth auth;

    public OidcRuntimeClient(OAuth2Auth auth) {
        this.auth = auth;
    }

    public String authorizationURL() {
        OAuth2Options config = OAuth2AuthProviderImpl.class.cast(auth).getConfig();
        final String path = config.getAuthorizationPath();
        return path.charAt(0) == '/' ? config.getSite() + path : path;
    }

    @SuppressWarnings("deprecation")
    public void verifyToken(UniEmitter<? super TokenVerificationResult> uniEmitter,
            TenantConfigContext resolvedContext, String token) {
        resolvedContext.client.decodeToken(token,
                new Handler<AsyncResult<AccessToken>>() {
                    @Override
                    public void handle(AsyncResult<AccessToken> event) {
                        if (event.failed()) {
                            uniEmitter.fail(new AuthenticationFailedException(event.cause()));
                        } else {
                            uniEmitter.complete(
                                    new TokenVerificationResult(event.result().accessToken(), event.result().principal()));
                        }
                    }
                });
    }

    public Uni<TokenVerificationResult> verifyTokenUni(TenantConfigContext resolvedContext, String token) {

        return Uni.createFrom().emitter(new Consumer<UniEmitter<? super TokenVerificationResult>>() {
            @Override
            public void accept(UniEmitter<? super TokenVerificationResult> emitter) {
                verifyToken(emitter, resolvedContext, token);
            }
        });

    }

    public void refreshToken(UniEmitter<? super AuthorizationCodeTokens> emitter, String refreshToken) {

        final AccessTokenImpl token = new AccessTokenImpl(new JsonObject(), auth);

        // always get the last token
        token.principal().put("refresh_token", refreshToken);

        token.refresh(new Handler<AsyncResult<Void>>() {
            @Override
            public void handle(AsyncResult<Void> result) {
                if (result.succeeded()) {
                    final String opaqueIdToken = token.opaqueIdToken();
                    final String opaqueAccessToken = token.opaqueAccessToken();
                    final String opaqueRefreshToken = token.opaqueRefreshToken() == null ? refreshToken
                            : token.opaqueRefreshToken();

                    AuthorizationCodeTokens tokens = new AuthorizationCodeTokens(opaqueIdToken, opaqueAccessToken,
                            opaqueRefreshToken);
                    emitter.complete(tokens);
                } else {
                    emitter.fail(result.cause());
                }
            }
        });
    }

    public void getCodeFlowTokens(UniEmitter<? super AuthorizationCodeTokens> emitter, JsonObject params) {

        auth.authenticate(params, new Handler<AsyncResult<User>>() {
            @Override
            public void handle(AsyncResult<User> result) {
                if (result.succeeded()) {
                    final AccessToken token = AccessToken.class.cast(result.result());
                    final String opaqueIdToken = token.opaqueIdToken();
                    final String opaqueAccessToken = token.opaqueAccessToken();
                    final String opaqueRefreshToken = token.opaqueRefreshToken();

                    AuthorizationCodeTokens tokens = new AuthorizationCodeTokens(opaqueIdToken, opaqueAccessToken,
                            opaqueRefreshToken);
                    emitter.complete(tokens);
                } else {
                    emitter.fail(result.cause());
                }
            }
        });
    }

    public String getLogoutPath() {
        return OAuth2AuthProviderImpl.class.cast(auth).getConfig().getLogoutPath();
    }

    public void decodeToken(String token, Handler<AsyncResult<AccessToken>> resultHandler) {
        auth.decodeToken(token, resultHandler);
    }

    public void createUserInfoToken(UniEmitter<? super JsonObject> uniEmitter, RoutingContext vertxContext,
            TokenAuthenticationRequest request) {
        AccessTokenImpl tokenImpl = new AccessTokenImpl(new JsonObject(), auth);
        String accessToken = vertxContext.get("access_token");
        if (accessToken == null) {
            accessToken = request.getToken().getToken();
        }
        tokenImpl.principal().put("access_token", accessToken);
        tokenImpl.userInfo(new Handler<AsyncResult<JsonObject>>() {
            @Override
            public void handle(AsyncResult<JsonObject> event) {
                if (event.failed()) {
                    uniEmitter.fail(new AuthenticationFailedException(event.cause()));
                } else {
                    uniEmitter.complete(event.result());
                }
            }
        });
    }

    public static OidcRuntimeClient discoverOidcEndpoints(Vertx vertx, OAuth2Options options,
            OidcTenantConfig oidcConfig) {
        return Uni.createFrom().emitter(new Consumer<UniEmitter<? super OidcRuntimeClient>>() {
            public void accept(UniEmitter<? super OidcRuntimeClient> uniEmitter) {
                KeycloakAuth.discover(vertx, options, new Handler<AsyncResult<OAuth2Auth>>() {
                    @Override
                    public void handle(AsyncResult<OAuth2Auth> event) {
                        if (event.failed()) {
                            uniEmitter.fail(toOidcException(event.cause(), options.getSite()));
                        } else {
                            uniEmitter.complete(createClient(event.result(), oidcConfig));
                        }
                    }
                });
            }
        }).await().atMost(Duration.ofSeconds(OidcCommonUtils.getMaximumConnectionDelay(oidcConfig) + 3));
    }

    public static OidcRuntimeClient setOidcEndpoints(Vertx vertx, OAuth2Options options, OidcTenantConfig oidcConfig) {
        if (options.getJwkPath() != null) {
            return Uni.createFrom().emitter(new Consumer<UniEmitter<? super OidcRuntimeClient>>() {
                @SuppressWarnings("deprecation")
                @Override
                public void accept(UniEmitter<? super OidcRuntimeClient> uniEmitter) {
                    OAuth2Auth auth = OAuth2Auth.create(vertx, options);
                    auth.loadJWK(res -> {
                        if (res.failed()) {
                            uniEmitter.fail(toOidcException(res.cause(), options.getSite()));
                        } else {
                            uniEmitter.complete(createClient(auth, oidcConfig));
                        }
                    });
                }
            }).await().atMost(Duration.ofSeconds(OidcCommonUtils.getMaximumConnectionDelay(oidcConfig) + 3));
        } else {
            return new OidcRuntimeClient(OAuth2Auth.create(vertx, options));
        }
    }

    private static OidcRuntimeClient createClient(OAuth2Auth client, OidcTenantConfig oidcConfig) {
        client.missingKeyHandler(new JwkSetRefreshHandler(client, oidcConfig.token.forcedJwkRefreshInterval));
        return new OidcRuntimeClient(client);
    }

    @SuppressWarnings("deprecation")
    public static OidcRuntimeClient createClientWithPublicKey(OAuth2Options options, String publicKey) {
        options.addPubSecKey(new PubSecKeyOptions()
                .setAlgorithm("RS256")
                .setPublicKey(publicKey));

        return new OidcRuntimeClient(new OAuth2AuthProviderImpl(null, options));
    }

    protected static OIDCException toOidcException(Throwable cause, String authServerUrl) {
        final String message = OidcCommonUtils.formatConnectionErrorMessage(authServerUrl);
        return new OIDCException(message, cause);
    }

    public static class JwkSetRefreshHandler implements Handler<String> {
        private static final Logger LOG = Logger.getLogger(JwkSetRefreshHandler.class);
        private OAuth2Auth auth;
        private volatile long lastForcedRefreshTime;
        private volatile long forcedJwksRefreshIntervalMilliSecs;

        public JwkSetRefreshHandler(OAuth2Auth auth, Duration forcedJwksRefreshInterval) {
            this.auth = auth;
            this.forcedJwksRefreshIntervalMilliSecs = forcedJwksRefreshInterval.toMillis();
        }

        @SuppressWarnings("deprecation")
        @Override
        public void handle(String kid) {
            final long now = System.currentTimeMillis();
            if (now > lastForcedRefreshTime + forcedJwksRefreshIntervalMilliSecs) {
                lastForcedRefreshTime = now;
                LOG.debugf("No JWK with %s key id is available, trying to refresh the JWK set", kid);
                auth.loadJWK(res -> {
                    if (res.failed()) {
                        LOG.debugf("Failed to refresh the JWK set: %s", res.cause());
                    }
                });
            }
        }
    }

    public JsonObject validateTokenWithoutOidcServer(String token) throws Exception {
        JWT jwt = new JWT();
        JsonObject object = jwt.decode(token);
        // TODO CES - Vert.x 4 Migrate expiration check:
        // this check throws the exception internally if the token has expired
        //        jwt.isExpired(tokenJson, authImpl.getConfig().getJWTOptions());
        return object;
    }
}
