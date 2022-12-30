package io.quarkus.oidc.runtime;

import static io.quarkus.oidc.runtime.OidcUtils.validateAndCreateIdentity;

import java.security.Principal;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;
import org.jose4j.lang.UnresolvableKeyException;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.IdTokenCredential;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.Roles.Source;
import io.quarkus.oidc.TokenIntrospection;
import io.quarkus.oidc.TokenIntrospectionCache;
import io.quarkus.oidc.UserInfo;
import io.quarkus.oidc.UserInfoCache;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class OidcIdentityProvider implements IdentityProvider<TokenAuthenticationRequest> {

    private static final Logger LOG = Logger.getLogger(OidcIdentityProvider.class);

    static final String REFRESH_TOKEN_GRANT_RESPONSE = "refresh_token_grant_response";
    static final String NEW_AUTHENTICATION = "new_authentication";

    private static final Uni<TokenVerificationResult> NULL_CODE_ACCESS_TOKEN_UNI = Uni.createFrom().nullItem();
    private static final Uni<UserInfo> NULL_USER_INFO_UNI = Uni.createFrom().nullItem();
    private static final String CODE_ACCESS_TOKEN_RESULT = "code_flow_access_token_result";

    @Inject
    DefaultTenantConfigResolver tenantResolver;

    private BlockingTaskRunner<Void> uniVoidOidcContext = new BlockingTaskRunner<Void>();
    private BlockingTaskRunner<TokenIntrospection> getIntrospectionRequestContext = new BlockingTaskRunner<TokenIntrospection>();
    private BlockingTaskRunner<UserInfo> getUserInfoRequestContext = new BlockingTaskRunner<UserInfo>();

    @Override
    public Class<TokenAuthenticationRequest> getRequestType() {
        return TokenAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(TokenAuthenticationRequest request,
            AuthenticationRequestContext context) {
        if (!(request.getToken() instanceof AccessTokenCredential || request.getToken() instanceof IdTokenCredential)) {
            return Uni.createFrom().nullItem();
        }
        LOG.debug("Starting creating SecurityIdentity");
        RoutingContext vertxContext = HttpSecurityUtils.getRoutingContextAttribute(request);
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
            LOG.debug("Performing token verification with a configured public key");
            return validateTokenWithoutOidcServer(request, resolvedContext);
        } else {
            return validateAllTokensWithOidcServer(vertxContext, request, resolvedContext);
        }
    }

    private Uni<SecurityIdentity> validateAllTokensWithOidcServer(RoutingContext vertxContext,
            TokenAuthenticationRequest request,
            TenantConfigContext resolvedContext) {

        Uni<UserInfo> userInfo = resolvedContext.oidcConfig.authentication.isUserInfoRequired().orElse(false)
                ? getUserInfoUni(vertxContext, request, resolvedContext)
                : NULL_USER_INFO_UNI;

        return userInfo.onItemOrFailure().transformToUni(
                new BiFunction<UserInfo, Throwable, Uni<? extends SecurityIdentity>>() {
                    @Override
                    public Uni<SecurityIdentity> apply(UserInfo userInfo, Throwable t) {
                        if (t != null) {
                            return Uni.createFrom().failure(new AuthenticationFailedException(t));
                        }
                        return validateTokenWithOidcServer(vertxContext, request, resolvedContext, userInfo);
                    }
                });
    }

    private Uni<SecurityIdentity> validateTokenWithOidcServer(RoutingContext vertxContext, TokenAuthenticationRequest request,
            TenantConfigContext resolvedContext, UserInfo userInfo) {

        Uni<TokenVerificationResult> codeAccessTokenUni = verifyCodeFlowAccessTokenUni(vertxContext, request, resolvedContext,
                userInfo);

        return codeAccessTokenUni.onItemOrFailure().transformToUni(
                new BiFunction<TokenVerificationResult, Throwable, Uni<? extends SecurityIdentity>>() {
                    @Override
                    public Uni<SecurityIdentity> apply(TokenVerificationResult codeAccessToken, Throwable t) {
                        if (t != null) {
                            return Uni.createFrom().failure(new AuthenticationFailedException(t));
                        }

                        if (codeAccessToken != null) {
                            vertxContext.put(CODE_ACCESS_TOKEN_RESULT, codeAccessToken);
                        }

                        return createSecurityIdentityWithOidcServer(vertxContext, request, resolvedContext, userInfo);
                    }
                });
    }

    private Uni<SecurityIdentity> createSecurityIdentityWithOidcServer(RoutingContext vertxContext,
            TokenAuthenticationRequest request, TenantConfigContext resolvedContext, final UserInfo userInfo) {
        Uni<TokenVerificationResult> tokenUni = null;
        if (isInternalIdToken(request)) {
            if (vertxContext.get(NEW_AUTHENTICATION) == Boolean.TRUE) {
                // No need to verify it in this case as 'CodeAuthenticationMechanism' has just created it
                tokenUni = Uni.createFrom()
                        .item(new TokenVerificationResult(OidcUtils.decodeJwtContent(request.getToken().getToken()), null));
            } else {
                tokenUni = verifySelfSignedTokenUni(resolvedContext, request.getToken().getToken());
            }
        } else {
            tokenUni = verifyTokenUni(resolvedContext, request.getToken().getToken(), userInfo);
        }

        return tokenUni.onItemOrFailure()
                .transformToUni(new BiFunction<TokenVerificationResult, Throwable, Uni<? extends SecurityIdentity>>() {
                    @Override
                    public Uni<SecurityIdentity> apply(TokenVerificationResult result, Throwable t) {
                        if (t != null) {
                            return Uni.createFrom().failure(new AuthenticationFailedException(t));
                        }
                        // Token has been verified, as a JWT or an opaque token, possibly involving
                        // an introspection request.
                        final TokenCredential tokenCred = request.getToken();

                        JsonObject tokenJson = result.localVerificationResult;
                        if (tokenJson == null) {
                            // JSON token representation may be null not only if it is an opaque access token
                            // but also if it is JWT and no JWK with a matching kid is available, asynchronous
                            // JWK refresh has not finished yet, but the fallback introspection request has succeeded.
                            tokenJson = OidcUtils.decodeJwtContent(tokenCred.getToken());
                        }
                        if (tokenJson != null) {
                            try {
                                OidcUtils.validatePrimaryJwtTokenType(resolvedContext.oidcConfig.token, tokenJson);
                                JsonObject rolesJson = getRolesJson(vertxContext, resolvedContext, tokenCred, tokenJson,
                                        userInfo);
                                SecurityIdentity securityIdentity = validateAndCreateIdentity(vertxContext, tokenCred,
                                        resolvedContext, tokenJson, rolesJson, userInfo, result.introspectionResult);
                                if (tokenAutoRefreshPrepared(tokenJson, vertxContext, resolvedContext.oidcConfig)) {
                                    return Uni.createFrom().failure(new TokenAutoRefreshException(securityIdentity));
                                } else {
                                    return Uni.createFrom().item(securityIdentity);
                                }
                            } catch (Throwable ex) {
                                return Uni.createFrom().failure(new AuthenticationFailedException(ex));
                            }
                        } else if (tokenCred instanceof IdTokenCredential
                                || tokenCred instanceof AccessTokenCredential
                                        && !((AccessTokenCredential) tokenCred).isOpaque()) {
                            return Uni.createFrom()
                                    .failure(new AuthenticationFailedException("JWT token can not be converted to JSON"));
                        } else {
                            // Opaque Bearer Access Token
                            QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
                            builder.addCredential(tokenCred);
                            OidcUtils.setSecurityIdentityUserInfo(builder, userInfo);
                            OidcUtils.setSecurityIdentityConfigMetadata(builder, resolvedContext);
                            final String userName;
                            if (result.introspectionResult == null) {
                                if (resolvedContext.oidcConfig.token.allowOpaqueTokenIntrospection &&
                                        resolvedContext.oidcConfig.token.verifyAccessTokenWithUserInfo) {
                                    userName = "";
                                } else {
                                    // we don't expect this to ever happen
                                    LOG.debug("Illegal state - token introspection result is not available.");
                                    return Uni.createFrom().failure(new AuthenticationFailedException());
                                }
                            } else {
                                OidcUtils.setSecurityIdentityIntrospection(builder, result.introspectionResult);
                                String principalMember = "";
                                if (result.introspectionResult.contains(OidcConstants.INTROSPECTION_TOKEN_USERNAME)) {
                                    principalMember = OidcConstants.INTROSPECTION_TOKEN_USERNAME;
                                } else if (result.introspectionResult.contains(OidcConstants.INTROSPECTION_TOKEN_SUB)) {
                                    // fallback to "sub", if "username" is not present
                                    principalMember = OidcConstants.INTROSPECTION_TOKEN_SUB;
                                }
                                userName = principalMember.isEmpty() ? ""
                                        : result.introspectionResult.getString(principalMember);
                                if (result.introspectionResult.contains(OidcConstants.TOKEN_SCOPE)) {
                                    for (String role : result.introspectionResult.getString(OidcConstants.TOKEN_SCOPE)
                                            .split(" ")) {
                                        builder.addRole(role.trim());
                                    }
                                }
                            }
                            builder.setPrincipal(new Principal() {
                                @Override
                                public String getName() {
                                    return userName;
                                }
                            });
                            if (userInfo != null) {
                                OidcUtils.setSecurityIdentityRoles(builder, resolvedContext.oidcConfig,
                                        new JsonObject(userInfo.getJsonObject().toString()));
                            }
                            OidcUtils.setBlockingApiAttribute(builder, vertxContext);
                            OidcUtils.setTenantIdAttribute(builder, resolvedContext.oidcConfig);
                            OidcUtils.setRoutingContextAttribute(builder, vertxContext);
                            return Uni.createFrom().item(builder.build());
                        }
                    }
                });
    }

    private static boolean isInternalIdToken(TokenAuthenticationRequest request) {
        return (request.getToken() instanceof IdTokenCredential) && ((IdTokenCredential) request.getToken()).isInternal();
    }

    private static boolean tokenAutoRefreshPrepared(JsonObject tokenJson, RoutingContext vertxContext,
            OidcTenantConfig oidcConfig) {
        if (tokenJson != null
                && oidcConfig.token.refreshExpired
                && oidcConfig.token.getRefreshTokenTimeSkew().isPresent()
                && vertxContext.get(REFRESH_TOKEN_GRANT_RESPONSE) != Boolean.TRUE
                && vertxContext.get(NEW_AUTHENTICATION) != Boolean.TRUE) {
            final long refreshTokenTimeSkew = oidcConfig.token.getRefreshTokenTimeSkew().get().getSeconds();
            final long expiry = tokenJson.getLong("exp");
            final long now = System.currentTimeMillis() / 1000;
            return now + refreshTokenTimeSkew > expiry;
        }
        return false;
    }

    private static JsonObject getRolesJson(RoutingContext vertxContext, TenantConfigContext resolvedContext,
            TokenCredential tokenCred,
            JsonObject tokenJson, UserInfo userInfo) {
        JsonObject rolesJson = tokenJson;
        if (resolvedContext.oidcConfig.roles.source.isPresent()) {
            if (resolvedContext.oidcConfig.roles.source.get() == Source.userinfo) {
                rolesJson = new JsonObject(userInfo.getJsonObject().toString());
            } else if (tokenCred instanceof IdTokenCredential
                    && resolvedContext.oidcConfig.roles.source.get() == Source.accesstoken) {
                rolesJson = ((TokenVerificationResult) vertxContext.get(CODE_ACCESS_TOKEN_RESULT)).localVerificationResult;
                if (rolesJson == null) {
                    // JSON token representation may be null not only if it is an opaque access token
                    // but also if it is JWT and no JWK with a matching kid is available, asynchronous
                    // JWK refresh has not finished yet, but the fallback introspection request has succeeded.
                    rolesJson = OidcUtils.decodeJwtContent((String) vertxContext.get(OidcConstants.ACCESS_TOKEN_VALUE));
                }
            }
        }
        return rolesJson;
    }

    private Uni<TokenVerificationResult> verifyCodeFlowAccessTokenUni(RoutingContext vertxContext,
            TokenAuthenticationRequest request,
            TenantConfigContext resolvedContext, UserInfo userInfo) {
        if (request.getToken() instanceof IdTokenCredential
                && (resolvedContext.oidcConfig.authentication.verifyAccessToken
                        || resolvedContext.oidcConfig.roles.source.orElse(null) == Source.accesstoken)) {
            final String codeAccessToken = (String) vertxContext.get(OidcConstants.ACCESS_TOKEN_VALUE);
            return verifyTokenUni(resolvedContext, codeAccessToken, userInfo);
        } else {
            return NULL_CODE_ACCESS_TOKEN_UNI;
        }
    }

    private Uni<TokenVerificationResult> verifyTokenUni(TenantConfigContext resolvedContext, String token, UserInfo userInfo) {
        if (OidcUtils.isOpaqueToken(token)) {
            if (!resolvedContext.oidcConfig.token.allowOpaqueTokenIntrospection) {
                LOG.debug("Token is opaque but the opaque token introspection is not allowed");
                throw new AuthenticationFailedException();
            }
            // verify opaque access token with UserInfo if enabled and introspection URI is absent
            if (resolvedContext.oidcConfig.token.verifyAccessTokenWithUserInfo
                    && resolvedContext.provider.getMetadata().getIntrospectionUri() == null) {
                if (userInfo == null) {
                    return Uni.createFrom().failure(
                            new AuthenticationFailedException("Opaque access token verification failed as user info is null."));
                } else {
                    // valid token verification result
                    return Uni.createFrom().item(new TokenVerificationResult(null, null));
                }
            }
            LOG.debug("Starting the opaque token introspection");
            return introspectTokenUni(resolvedContext, token);
        } else if (resolvedContext.provider.getMetadata().getJsonWebKeySetUri() == null
                || resolvedContext.oidcConfig.token.requireJwtIntrospectionOnly) {
            // Verify JWT token with the remote introspection
            LOG.debug("Starting the JWT token introspection");
            return introspectTokenUni(resolvedContext, token);
        } else {
            // Verify JWT token with the local JWK keys with a possible remote introspection fallback
            try {
                LOG.debug("Verifying the JWT token with the local JWK keys");
                return Uni.createFrom().item(resolvedContext.provider.verifyJwtToken(token));
            } catch (Throwable t) {
                if (t.getCause() instanceof UnresolvableKeyException) {
                    LOG.debug("No matching JWK key is found, refreshing and repeating the verification");
                    return refreshJwksAndVerifyTokenUni(resolvedContext, token);
                } else {
                    LOG.debugf("Token verification has failed: %s", t.getMessage());
                    return Uni.createFrom().failure(t);
                }
            }
        }
    }

    private Uni<TokenVerificationResult> verifySelfSignedTokenUni(TenantConfigContext resolvedContext, String token) {
        try {
            return Uni.createFrom().item(resolvedContext.provider.verifySelfSignedJwtToken(token));
        } catch (Throwable t) {
            return Uni.createFrom().failure(t);
        }
    }

    private Uni<TokenVerificationResult> refreshJwksAndVerifyTokenUni(TenantConfigContext resolvedContext, String token) {
        return resolvedContext.provider.refreshJwksAndVerifyJwtToken(token)
                .onFailure(f -> f.getCause() instanceof UnresolvableKeyException
                        && resolvedContext.oidcConfig.token.allowJwtIntrospection)
                .recoverWithUni(f -> introspectTokenUni(resolvedContext, token));
    }

    private Uni<TokenVerificationResult> introspectTokenUni(TenantConfigContext resolvedContext, String token) {
        TokenIntrospectionCache tokenIntrospectionCache = tenantResolver.getTokenIntrospectionCache();
        Uni<TokenIntrospection> tokenIntrospectionUni = tokenIntrospectionCache == null ? null
                : tokenIntrospectionCache
                        .getIntrospection(token, resolvedContext.oidcConfig, getIntrospectionRequestContext);
        if (tokenIntrospectionUni == null) {
            tokenIntrospectionUni = newTokenIntrospectionUni(resolvedContext, token);
        } else {
            tokenIntrospectionUni = tokenIntrospectionUni.onItem().ifNull()
                    .switchTo(newTokenIntrospectionUni(resolvedContext, token));
        }
        return tokenIntrospectionUni.onItem().transform(t -> new TokenVerificationResult(null, t));
    }

    private Uni<TokenIntrospection> newTokenIntrospectionUni(TenantConfigContext resolvedContext, String token) {
        Uni<TokenIntrospection> tokenIntrospectionUni = resolvedContext.provider.introspectToken(token);
        if (tenantResolver.getTokenIntrospectionCache() == null || !resolvedContext.oidcConfig.allowTokenIntrospectionCache) {
            return tokenIntrospectionUni;
        } else {
            return tokenIntrospectionUni.call(new Function<TokenIntrospection, Uni<?>>() {

                @Override
                public Uni<?> apply(TokenIntrospection introspection) {
                    return tenantResolver.getTokenIntrospectionCache().addIntrospection(token, introspection,
                            resolvedContext.oidcConfig, uniVoidOidcContext);
                }
            });
        }
    }

    private static Uni<SecurityIdentity> validateTokenWithoutOidcServer(TokenAuthenticationRequest request,
            TenantConfigContext resolvedContext) {

        try {
            TokenVerificationResult result = resolvedContext.provider.verifyJwtToken(request.getToken().getToken());
            return Uni.createFrom()
                    .item(validateAndCreateIdentity(null, request.getToken(), resolvedContext,
                            result.localVerificationResult, result.localVerificationResult, null, null));
        } catch (Throwable t) {
            return Uni.createFrom().failure(new AuthenticationFailedException(t));
        }
    }

    private Uni<UserInfo> getUserInfoUni(RoutingContext vertxContext, TokenAuthenticationRequest request,
            TenantConfigContext resolvedContext) {
        if (isInternalIdToken(request) && resolvedContext.oidcConfig.cacheUserInfoInIdtoken) {
            JsonObject userInfo = OidcUtils.decodeJwtContent(request.getToken().getToken())
                    .getJsonObject(OidcUtils.USER_INFO_ATTRIBUTE);
            if (userInfo != null) {
                return Uni.createFrom().item(new UserInfo(userInfo.encode()));
            }
        }

        LOG.debug("Requesting UserInfo");
        String accessToken = vertxContext.get(OidcConstants.ACCESS_TOKEN_VALUE);
        if (accessToken == null) {
            accessToken = request.getToken().getToken();
        }

        UserInfoCache userInfoCache = tenantResolver.getUserInfoCache();
        Uni<UserInfo> userInfoUni = userInfoCache == null ? null
                : userInfoCache.getUserInfo(accessToken, resolvedContext.oidcConfig, getUserInfoRequestContext);
        if (userInfoUni == null) {
            userInfoUni = newUserInfoUni(resolvedContext, accessToken);
        } else {
            userInfoUni = userInfoUni.onItem().ifNull()
                    .switchTo(newUserInfoUni(resolvedContext, accessToken));
        }
        return userInfoUni;
    }

    private Uni<UserInfo> newUserInfoUni(TenantConfigContext resolvedContext, String accessToken) {
        Uni<UserInfo> userInfoUni = resolvedContext.provider.getUserInfo(accessToken);
        if (tenantResolver.getUserInfoCache() == null || !resolvedContext.oidcConfig.allowUserInfoCache
                || resolvedContext.oidcConfig.cacheUserInfoInIdtoken) {
            return userInfoUni;
        } else {
            return userInfoUni.call(new Function<UserInfo, Uni<?>>() {

                @Override
                public Uni<?> apply(UserInfo userInfo) {
                    return tenantResolver.getUserInfoCache().addUserInfo(accessToken, userInfo,
                            resolvedContext.oidcConfig, uniVoidOidcContext);
                }
            });
        }
    }
}
