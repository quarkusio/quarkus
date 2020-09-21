package io.quarkus.oidc.runtime;

import static io.quarkus.oidc.runtime.OidcUtils.validateAndCreateIdentity;

import java.security.Principal;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.IdTokenCredential;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.Roles.Source;
import io.quarkus.oidc.OidcTokenCredential;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.auth.oauth2.impl.OAuth2AuthProviderImpl;
import io.vertx.ext.jwt.JWT;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class OidcIdentityProvider implements IdentityProvider<TokenAuthenticationRequest> {
    @Inject
    DefaultTenantConfigResolver tenantResolver;

    @Override
    public Class<TokenAuthenticationRequest> getRequestType() {
        return TokenAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(TokenAuthenticationRequest request,
            AuthenticationRequestContext context) {
        OidcTokenCredential credential = (OidcTokenCredential) request.getToken();
        RoutingContext vertxContext = credential.getRoutingContext();
        return Uni.createFrom().deferred(new Supplier<Uni<SecurityIdentity>>() {
            @Override
            public Uni<SecurityIdentity> get() {
                if (tenantResolver.isBlocking(vertxContext)) {
                    return context.runBlocking(new Supplier<SecurityIdentity>() {
                        @Override
                        public SecurityIdentity get() {
                            return authenticate(request, vertxContext).await().indefinitely();
                        }
                    });
                }

                return authenticate(request, vertxContext);
            }
        });

    }

    private Uni<SecurityIdentity> authenticate(TokenAuthenticationRequest request,
            RoutingContext vertxContext) {
        TenantConfigContext resolvedContext = tenantResolver.resolve(vertxContext, true);

        if (resolvedContext.oidcConfig.publicKey.isPresent()) {
            return validateTokenWithoutOidcServer(request, resolvedContext);
        } else {
            return validateTokenWithOidcServer(vertxContext, request, resolvedContext);
        }
    }

    @SuppressWarnings("deprecation")
    private Uni<SecurityIdentity> validateTokenWithOidcServer(RoutingContext vertxContext, TokenAuthenticationRequest request,
            TenantConfigContext resolvedContext) {

        if (request.getToken() instanceof IdTokenCredential
                && (resolvedContext.oidcConfig.authentication.verifyAccessToken
                        || resolvedContext.oidcConfig.roles.source.orElse(null) == Source.accesstoken)) {
            vertxContext.put("code_flow_access_token_result",
                    verifyCodeFlowAccessToken(vertxContext, request, resolvedContext));
        }
        return Uni.createFrom().emitter(new Consumer<UniEmitter<? super SecurityIdentity>>() {
            @Override
            public void accept(UniEmitter<? super SecurityIdentity> uniEmitter) {

                resolvedContext.auth.decodeToken(request.getToken().getToken(),
                        new Handler<AsyncResult<AccessToken>>() {
                            @Override
                            public void handle(AsyncResult<AccessToken> event) {
                                if (event.failed()) {
                                    uniEmitter.fail(new AuthenticationFailedException(event.cause()));
                                    return;
                                }

                                // Token has been verified, as a JWT or an opaque token, possibly involving
                                // an introspection request.
                                final TokenCredential tokenCred = request.getToken();

                                JsonObject tokenJson = event.result().accessToken();

                                if (tokenJson == null) {
                                    // JSON token representation may be null not only if it is an opaque access token
                                    // but also if it is JWT and no JWK with a matching kid is available, asynchronous
                                    // JWK refresh has not finished yet, but the fallback introspection request has succeeded.
                                    tokenJson = OidcUtils.decodeJwtContent(tokenCred.getToken());
                                }
                                JsonObject userInfo = null;
                                if (resolvedContext.oidcConfig.authentication.isUserInfoRequired()) {
                                    userInfo = getUserInfo(event.result(), (String) vertxContext.get("access_token"));
                                }
                                if (tokenJson != null) {
                                    OidcUtils.validatePrimaryJwtTokenType(resolvedContext.oidcConfig.token, tokenJson);
                                    JsonObject rolesJson = getRolesJson(vertxContext, resolvedContext, tokenCred, tokenJson,
                                            userInfo);
                                    try {
                                        SecurityIdentity securityIdentity = validateAndCreateIdentity(vertxContext, tokenCred,
                                                resolvedContext.oidcConfig,
                                                tokenJson, rolesJson, userInfo);
                                        if (tokenAutoRefreshPrepared(tokenJson, vertxContext, resolvedContext.oidcConfig)) {
                                            throw new TokenAutoRefreshException(securityIdentity);
                                        } else {
                                            uniEmitter.complete(securityIdentity);
                                        }
                                    } catch (Throwable ex) {
                                        uniEmitter.fail(ex);
                                    }
                                } else if (tokenCred instanceof IdTokenCredential
                                        || tokenCred instanceof AccessTokenCredential
                                                && !((AccessTokenCredential) tokenCred).isOpaque()) {
                                    uniEmitter
                                            .fail(new AuthenticationFailedException("JWT token can not be converted to JSON"));
                                } else {
                                    // Opaque Bearer Access Token
                                    QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
                                    builder.addCredential(tokenCred);
                                    OidcUtils.setSecurityIdentityUserInfo(builder, userInfo);
                                    if (event.result().principal().containsKey("username")) {
                                        final String userName = event.result().principal().getString("username");
                                        builder.setPrincipal(new Principal() {
                                            @Override
                                            public String getName() {
                                                return userName;
                                            }
                                        });
                                    }
                                    if (event.result().principal().containsKey("scope")) {
                                        for (String role : event.result().principal().getString("scope").split(" ")) {
                                            builder.addRole(role.trim());
                                        }
                                    }
                                    uniEmitter.complete(builder.build());
                                }
                            }
                        });
            }
        });
    }

    private static boolean tokenAutoRefreshPrepared(JsonObject tokenJson, RoutingContext vertxContext,
            OidcTenantConfig oidcConfig) {
        if (tokenJson != null
                && oidcConfig.token.refreshExpired
                && oidcConfig.token.autoRefreshInterval.isPresent()
                && vertxContext.get("tokenAutoRefreshInProgress") != Boolean.TRUE
                && vertxContext.get("new_authentication") != Boolean.TRUE) {
            final long autoRefreshInterval = oidcConfig.token.autoRefreshInterval.get().getSeconds();
            final long expiry = tokenJson.getLong("exp");
            final long now = System.currentTimeMillis() / 1000;
            if (now + autoRefreshInterval > expiry) {
                vertxContext.put("tokenAutoRefreshInProgress", Boolean.TRUE);
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private static JsonObject getRolesJson(RoutingContext vertxContext, TenantConfigContext resolvedContext,
            TokenCredential tokenCred,
            JsonObject tokenJson, JsonObject userInfo) {
        JsonObject rolesJson = tokenJson;
        if (resolvedContext.oidcConfig.roles.source.isPresent()) {
            if (resolvedContext.oidcConfig.roles.source.get() == Source.userinfo) {
                rolesJson = userInfo;
            } else if (tokenCred instanceof IdTokenCredential
                    && resolvedContext.oidcConfig.roles.source.get() == Source.accesstoken) {
                AccessToken result = (AccessToken) vertxContext.get("code_flow_access_token_result");
                rolesJson = result.accessToken();
                if (rolesJson == null) {
                    // JSON token representation may be null not only if it is an opaque access token
                    // but also if it is JWT and no JWK with a matching kid is available, asynchronous
                    // JWK refresh has not finished yet, but the fallback introspection request has succeeded.
                    rolesJson = OidcUtils.decodeJwtContent((String) vertxContext.get("access_token"));
                }
                if (rolesJson == null) {
                    // this is the introspection response which may contain a 'scope' property
                    rolesJson = result.principal();
                }
            }
        }
        return rolesJson;
    }

    @SuppressWarnings("deprecation")
    private static AccessToken verifyCodeFlowAccessToken(RoutingContext vertxContext, TokenAuthenticationRequest request,
            TenantConfigContext resolvedContext) {
        return Uni.createFrom().emitter(new Consumer<UniEmitter<? super AccessToken>>() {
            @Override
            public void accept(UniEmitter<? super AccessToken> uniEmitter) {
                resolvedContext.auth.decodeToken((String) vertxContext.get("access_token"),
                        new Handler<AsyncResult<AccessToken>>() {
                            @Override
                            public void handle(AsyncResult<AccessToken> event) {
                                if (event.failed()) {
                                    uniEmitter.fail(new AuthenticationFailedException(event.cause()));
                                }
                                uniEmitter.complete(event.result());
                            }
                        });
            }
        }).await().indefinitely();
    }

    private static Uni<SecurityIdentity> validateTokenWithoutOidcServer(TokenAuthenticationRequest request,
            TenantConfigContext resolvedContext) {
        OAuth2AuthProviderImpl auth = ((OAuth2AuthProviderImpl) resolvedContext.auth);
        JWT jwt = auth.getJWT();
        JsonObject tokenJson = null;
        try {
            tokenJson = jwt.decode(request.getToken().getToken());
        } catch (Throwable ex) {
            return Uni.createFrom().failure(new AuthenticationFailedException(ex));
        }
        if (jwt.isExpired(tokenJson, auth.getConfig().getJWTOptions())) {
            return Uni.createFrom().failure(new AuthenticationFailedException());
        } else {
            try {
                return Uni.createFrom()
                        .item(validateAndCreateIdentity(null, request.getToken(), resolvedContext.oidcConfig, tokenJson,
                                tokenJson,
                                null));
            } catch (Throwable ex) {
                return Uni.createFrom().failure(ex);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static JsonObject getUserInfo(AccessToken tokenImpl, String opaqueAccessToken) {
        return Uni.createFrom().emitter(
                new Consumer<UniEmitter<? super JsonObject>>() {
                    @Override
                    public void accept(UniEmitter<? super JsonObject> uniEmitter) {
                        if (opaqueAccessToken != null) {
                            tokenImpl.principal().put("access_token", opaqueAccessToken);
                        }
                        tokenImpl.userInfo(new Handler<AsyncResult<JsonObject>>() {
                            @Override
                            public void handle(AsyncResult<JsonObject> event) {
                                if (event.failed()) {
                                    uniEmitter.fail(event.cause());
                                } else {
                                    uniEmitter.complete(event.result());
                                }
                            }
                        });
                    }
                }).await().indefinitely();
    }
}
