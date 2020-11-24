package io.quarkus.oidc.runtime;

import static io.quarkus.oidc.runtime.OidcUtils.validateAndCreateIdentity;

import java.security.Principal;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.IdTokenCredential;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.Roles.Source;
import io.quarkus.oidc.OidcTokenCredential;
import io.quarkus.runtime.BlockingOperationControl;
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
import io.vertx.ext.auth.oauth2.impl.OAuth2TokenImpl;
import io.vertx.ext.jwt.JWT;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class OidcIdentityProvider implements IdentityProvider<TokenAuthenticationRequest> {

    static final String CODE_FLOW_ACCESS_TOKEN = "access_token";
    static final String REFRESH_TOKEN_GRANT_RESPONSE = "refresh_token_grant_response";
    static final String NEW_AUTHENTICATION = "new_authentication";

    @SuppressWarnings("deprecation")
    private static final Uni<AccessToken> NULL_CODE_ACCESS_TOKEN_UNI = Uni.createFrom().nullItem();
    private static final Uni<JsonObject> NULL_USER_INFO_UNI = Uni.createFrom().nullItem();
    private static final String CODE_ACCESS_TOKEN_RESULT = "code_flow_access_token_result";

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
        vertxContext.put(AuthenticationRequestContext.class.getName(), context);

        Uni<TenantConfigContext> tenantConfigContext = tenantResolver.resolveContext(vertxContext);

        return tenantConfigContext.onItem()
                .transformToUni(new Function<TenantConfigContext, Uni<? extends SecurityIdentity>>() {
                    @Override
                    public Uni<SecurityIdentity> apply(TenantConfigContext tenantConfigContext) {
                        return Uni.createFrom().deferred(new Supplier<Uni<? extends SecurityIdentity>>() {
                            @Override
                            public Uni<SecurityIdentity> get() {
                                return authenticate(request, vertxContext, tenantConfigContext);
                            }
                        });
                    }
                });
    }

    private Uni<SecurityIdentity> authenticate(TokenAuthenticationRequest request,
            RoutingContext vertxContext,
            TenantConfigContext resolvedContext) {
        if (resolvedContext.oidcConfig.publicKey.isPresent()) {
            return validateTokenWithoutOidcServer(request, resolvedContext);
        } else {
            return validateAllTokensWithOidcServer(vertxContext, request, resolvedContext);
        }
    }

    @SuppressWarnings("deprecation")
    private Uni<SecurityIdentity> validateAllTokensWithOidcServer(RoutingContext vertxContext,
            TokenAuthenticationRequest request,
            TenantConfigContext resolvedContext) {

        Uni<AccessToken> codeAccessTokenUni = verifyCodeFlowAccessTokenUni(vertxContext, request, resolvedContext);

        return codeAccessTokenUni.onItem().transformToUni(
                new Function<AccessToken, Uni<? extends SecurityIdentity>>() {
                    @Override
                    public Uni<SecurityIdentity> apply(AccessToken codeAccessToken) {
                        return validateTokenWithOidcServer(vertxContext, request, resolvedContext, codeAccessToken);
                    }
                });
    }

    @SuppressWarnings("deprecation")
    private Uni<SecurityIdentity> validateTokenWithOidcServer(RoutingContext vertxContext, TokenAuthenticationRequest request,
            TenantConfigContext resolvedContext, AccessToken codeAccessToken) {

        if (codeAccessToken != null) {
            vertxContext.put(CODE_ACCESS_TOKEN_RESULT, codeAccessToken);
        }

        Uni<JsonObject> userInfo = getUserInfoUni(vertxContext, request, resolvedContext);

        return userInfo.onItem().transformToUni(
                new Function<JsonObject, Uni<? extends SecurityIdentity>>() {
                    @Override
                    public Uni<SecurityIdentity> apply(JsonObject userInfo) {
                        return createSecurityIdentityWithOidcServerUni(vertxContext, request, resolvedContext, userInfo);
                    }
                });
    }

    private Uni<SecurityIdentity> createSecurityIdentityWithOidcServerUni(RoutingContext vertxContext,
            TokenAuthenticationRequest request, TenantConfigContext resolvedContext, final JsonObject userInfo) {

        final TokenCredential tokenCred = request.getToken();
        if (tokenCred instanceof AccessTokenCredential && ((AccessTokenCredential) tokenCred).isOpaque()) {
            // remote introspection is required, a blocking call
            return Uni.createFrom().emitter(
                    new Consumer<UniEmitter<? super SecurityIdentity>>() {
                        @Override
                        public void accept(UniEmitter<? super SecurityIdentity> uniEmitter) {
                            if (BlockingOperationControl.isBlockingAllowed()) {
                                createSecurityIdentityWithOidcServer(uniEmitter, vertxContext, request, resolvedContext,
                                        userInfo);
                            } else {
                                tenantResolver.getBlockingExecutor().execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        createSecurityIdentityWithOidcServer(uniEmitter, vertxContext, request, resolvedContext,
                                                userInfo);
                                    }
                                });
                            }
                        }
                    });
        } else {
            return Uni.createFrom().emitter(new Consumer<UniEmitter<? super SecurityIdentity>>() {
                @Override
                public void accept(UniEmitter<? super SecurityIdentity> uniEmitter) {
                    createSecurityIdentityWithOidcServer(uniEmitter, vertxContext, request, resolvedContext, userInfo);
                }
            });
        }
    }

    @SuppressWarnings("deprecation")
    private void createSecurityIdentityWithOidcServer(UniEmitter<? super SecurityIdentity> uniEmitter,
            RoutingContext vertxContext,
            TokenAuthenticationRequest request, TenantConfigContext resolvedContext, final JsonObject userInfo) {
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
                        if (tokenJson != null) {
                            OidcUtils.validatePrimaryJwtTokenType(resolvedContext.oidcConfig.token, tokenJson);
                            JsonObject rolesJson = getRolesJson(vertxContext, resolvedContext, tokenCred, tokenJson,
                                    userInfo);
                            try {
                                SecurityIdentity securityIdentity = validateAndCreateIdentity(vertxContext, tokenCred,
                                        resolvedContext.oidcConfig,
                                        tokenJson, rolesJson, userInfo);
                                if (tokenAutoRefreshPrepared(tokenJson, vertxContext, resolvedContext.oidcConfig)) {
                                    uniEmitter.fail(new TokenAutoRefreshException(securityIdentity));
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
                            if (userInfo != null) {
                                OidcUtils.setSecurityIdentityRoles(builder, resolvedContext.oidcConfig, userInfo);
                            }
                            OidcUtils.setBlockinApiAttribute(builder, vertxContext);
                            OidcUtils.setTenantIdAttribute(builder, resolvedContext.oidcConfig);
                            uniEmitter.complete(builder.build());
                        }
                    }
                });
    }

    private static boolean tokenAutoRefreshPrepared(JsonObject tokenJson, RoutingContext vertxContext,
            OidcTenantConfig oidcConfig) {
        if (tokenJson != null
                && oidcConfig.token.refreshExpired
                && oidcConfig.token.autoRefreshInterval.isPresent()
                && vertxContext.get(REFRESH_TOKEN_GRANT_RESPONSE) != Boolean.TRUE
                && vertxContext.get(NEW_AUTHENTICATION) != Boolean.TRUE) {
            final long autoRefreshInterval = oidcConfig.token.autoRefreshInterval.get().getSeconds();
            final long expiry = tokenJson.getLong("exp");
            final long now = System.currentTimeMillis() / 1000;
            return now + autoRefreshInterval > expiry;
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
                AccessToken result = (AccessToken) vertxContext.get(CODE_ACCESS_TOKEN_RESULT);
                rolesJson = result != null ? result.accessToken() : null;
                if (rolesJson == null) {
                    // JSON token representation may be null not only if it is an opaque access token
                    // but also if it is JWT and no JWK with a matching kid is available, asynchronous
                    // JWK refresh has not finished yet, but the fallback introspection request has succeeded.
                    rolesJson = OidcUtils.decodeJwtContent((String) vertxContext.get(CODE_FLOW_ACCESS_TOKEN));
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
    private Uni<AccessToken> verifyCodeFlowAccessTokenUni(RoutingContext vertxContext, TokenAuthenticationRequest request,
            TenantConfigContext resolvedContext) {
        if (request.getToken() instanceof IdTokenCredential
                && (resolvedContext.oidcConfig.authentication.verifyAccessToken
                        || resolvedContext.oidcConfig.roles.source.orElse(null) == Source.accesstoken)) {
            final String codeAccessToken = (String) vertxContext.get(CODE_FLOW_ACCESS_TOKEN);
            if (OidcUtils.isOpaqueToken(codeAccessToken)) {
                // remote introspection is required, a blocking call
                return Uni.createFrom().emitter(
                        new Consumer<UniEmitter<? super AccessToken>>() {
                            @Override
                            public void accept(UniEmitter<? super AccessToken> uniEmitter) {
                                if (BlockingOperationControl.isBlockingAllowed()) {
                                    verifyCodeFlowAccessToken(uniEmitter, resolvedContext, codeAccessToken);
                                } else {
                                    tenantResolver.getBlockingExecutor().execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            verifyCodeFlowAccessToken(uniEmitter, resolvedContext, codeAccessToken);
                                        }
                                    });
                                }
                            }
                        });
            } else {
                return Uni.createFrom().emitter(new Consumer<UniEmitter<? super AccessToken>>() {
                    @Override
                    public void accept(UniEmitter<? super AccessToken> uniEmitter) {
                        verifyCodeFlowAccessToken(uniEmitter, resolvedContext, codeAccessToken);
                    }
                });
            }
        } else {
            return NULL_CODE_ACCESS_TOKEN_UNI;
        }
    }

    @SuppressWarnings("deprecation")
    private void verifyCodeFlowAccessToken(UniEmitter<? super AccessToken> uniEmitter,
            TenantConfigContext resolvedContext, String codeAccessToken) {
        resolvedContext.auth.decodeToken(codeAccessToken,
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
        try {
            if (jwt.isExpired(tokenJson, auth.getConfig().getJWTOptions())) {
                return Uni.createFrom().failure(new AuthenticationFailedException());
            }
            return Uni.createFrom()
                    .item(validateAndCreateIdentity(null, request.getToken(), resolvedContext.oidcConfig, tokenJson,
                            tokenJson,
                            null));
        } catch (Throwable ex) {
            return Uni.createFrom().failure(new AuthenticationFailedException(ex));
        }
    }

    private Uni<JsonObject> getUserInfoUni(RoutingContext vertxContext, TokenAuthenticationRequest request,
            TenantConfigContext resolvedContext) {
        if (resolvedContext.oidcConfig.authentication.isUserInfoRequired()) {
            return Uni.createFrom().emitter(
                    new Consumer<UniEmitter<? super JsonObject>>() {
                        @Override
                        public void accept(UniEmitter<? super JsonObject> uniEmitter) {
                            if (BlockingOperationControl.isBlockingAllowed()) {
                                createUserInfoToken(uniEmitter, vertxContext, request, resolvedContext);
                            } else {
                                tenantResolver.getBlockingExecutor().execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        createUserInfoToken(uniEmitter, vertxContext, request, resolvedContext);
                                    }
                                });
                            }
                        }
                    });
        } else {
            return NULL_USER_INFO_UNI;
        }
    }

    private void createUserInfoToken(UniEmitter<? super JsonObject> uniEmitter, RoutingContext vertxContext,
            TokenAuthenticationRequest request, TenantConfigContext resolvedContext) {
        OAuth2TokenImpl tokenImpl = new OAuth2TokenImpl(resolvedContext.auth, new JsonObject());
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

}
