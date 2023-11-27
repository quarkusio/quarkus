package io.quarkus.oidc.runtime;

import static io.quarkus.oidc.runtime.OidcUtils.validateAndCreateIdentity;
import static io.quarkus.vertx.http.runtime.security.HttpSecurityUtils.getRoutingContextAttribute;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.jwt.Claims;
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
import io.quarkus.security.AuthenticationCompletionException;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.security.spi.runtime.BlockingSecurityExecutor;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class OidcIdentityProvider implements IdentityProvider<TokenAuthenticationRequest> {

    private static final Logger LOG = Logger.getLogger(OidcIdentityProvider.class);

    static final String REFRESH_TOKEN_GRANT_RESPONSE = "refresh_token_grant_response";
    static final String NEW_AUTHENTICATION = "new_authentication";

    private static final Uni<TokenVerificationResult> NULL_CODE_ACCESS_TOKEN_UNI = Uni.createFrom().nullItem();
    private static final String CODE_ACCESS_TOKEN_RESULT = "code_flow_access_token_result";

    protected final DefaultTenantConfigResolver tenantResolver;
    private final BlockingTaskRunner<Void> uniVoidOidcContext;
    private final BlockingTaskRunner<TokenIntrospection> getIntrospectionRequestContext;
    private final BlockingTaskRunner<UserInfo> getUserInfoRequestContext;

    OidcIdentityProvider(DefaultTenantConfigResolver tenantResolver, BlockingSecurityExecutor blockingExecutor) {
        this.tenantResolver = tenantResolver;
        this.uniVoidOidcContext = new BlockingTaskRunner<>(blockingExecutor);
        this.getIntrospectionRequestContext = new BlockingTaskRunner<>(blockingExecutor);
        this.getUserInfoRequestContext = new BlockingTaskRunner<>(blockingExecutor);
    }

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

        return resolveTenantConfigContext(request, context).onItem()
                .transformToUni(new Function<TenantConfigContext, Uni<? extends SecurityIdentity>>() {
                    @Override
                    public Uni<SecurityIdentity> apply(TenantConfigContext tenantConfigContext) {
                        return Uni.createFrom().deferred(new Supplier<Uni<? extends SecurityIdentity>>() {
                            @Override
                            public Uni<SecurityIdentity> get() {
                                return authenticate(request, getRequestData(request), tenantConfigContext);
                            }
                        });
                    }
                });
    }

    protected Uni<TenantConfigContext> resolveTenantConfigContext(TokenAuthenticationRequest request,
            AuthenticationRequestContext context) {
        return tenantResolver.resolveContext(
                getRoutingContextAttribute(request).put(AuthenticationRequestContext.class.getName(), context));
    }

    protected Map<String, Object> getRequestData(TokenAuthenticationRequest request) {
        return getRoutingContextAttribute(request).data();
    }

    private Uni<SecurityIdentity> authenticate(TokenAuthenticationRequest request, Map<String, Object> requestData,
            TenantConfigContext resolvedContext) {
        if (resolvedContext.oidcConfig.publicKey.isPresent()) {
            LOG.debug("Performing token verification with a configured public key");
            return validateTokenWithoutOidcServer(request, resolvedContext);
        } else {
            return validateAllTokensWithOidcServer(requestData, request, resolvedContext);
        }
    }

    private Uni<SecurityIdentity> validateAllTokensWithOidcServer(Map<String, Object> requestData,
            TokenAuthenticationRequest request, TenantConfigContext resolvedContext) {

        if (resolvedContext.oidcConfig.token.verifyAccessTokenWithUserInfo.orElse(false)
                && isOpaqueAccessToken(requestData, request, resolvedContext)) {
            // UserInfo has to be acquired first as a precondition for verifying opaque access tokens.
            // Typically it will be done for bearer access tokens therefore even if the access token has expired
            // the client will be able to refresh if needed, no refresh token is available to Quarkus during the
            // bearer access token verification
            if (resolvedContext.oidcConfig.authentication.isUserInfoRequired().orElse(false)) {
                return getUserInfoUni(requestData, request, resolvedContext).onItemOrFailure().transformToUni(
                        new BiFunction<UserInfo, Throwable, Uni<? extends SecurityIdentity>>() {
                            @Override
                            public Uni<SecurityIdentity> apply(UserInfo userInfo, Throwable t) {
                                if (t != null) {
                                    return Uni.createFrom().failure(new AuthenticationFailedException(t));
                                }
                                return validateTokenWithUserInfoAndCreateIdentity(requestData, request, resolvedContext,
                                        userInfo);
                            }
                        });
            } else {
                return validateTokenWithUserInfoAndCreateIdentity(requestData, request, resolvedContext, null);
            }
        } else {
            final Uni<TokenVerificationResult> primaryTokenUni;
            if (isInternalIdToken(request)) {
                if (requestData.get(NEW_AUTHENTICATION) == Boolean.TRUE) {
                    // No need to verify it in this case as 'CodeAuthenticationMechanism' has just created it
                    primaryTokenUni = Uni.createFrom()
                            .item(new TokenVerificationResult(OidcUtils.decodeJwtContent(request.getToken().getToken()), null));
                } else {
                    primaryTokenUni = verifySelfSignedTokenUni(resolvedContext, request.getToken().getToken());
                }
            } else {
                primaryTokenUni = verifyTokenUni(requestData, resolvedContext, request.getToken(),
                        isIdToken(request), null);
            }

            // Verify Code Flow access token first if it is available and has to be verified.
            // It may be refreshed if it has or has nearly expired
            Uni<TokenVerificationResult> codeAccessTokenUni = verifyCodeFlowAccessTokenUni(requestData, request,
                    resolvedContext,
                    null);
            return codeAccessTokenUni.onItemOrFailure().transformToUni(
                    new BiFunction<TokenVerificationResult, Throwable, Uni<? extends SecurityIdentity>>() {
                        @Override
                        public Uni<SecurityIdentity> apply(TokenVerificationResult codeAccessTokenResult, Throwable t) {
                            if (t != null) {
                                return Uni.createFrom().failure(t instanceof AuthenticationFailedException ? t
                                        : new AuthenticationFailedException(t));
                            }
                            if (codeAccessTokenResult != null) {
                                if (tokenAutoRefreshPrepared(codeAccessTokenResult, requestData,
                                        resolvedContext.oidcConfig)) {
                                    return Uni.createFrom().failure(new TokenAutoRefreshException(null));
                                }
                                requestData.put(CODE_ACCESS_TOKEN_RESULT, codeAccessTokenResult);
                            }
                            return getUserInfoAndCreateIdentity(primaryTokenUni, requestData, request, resolvedContext);
                        }
                    });

        }
    }

    private Uni<SecurityIdentity> validateTokenWithUserInfoAndCreateIdentity(Map<String, Object> requestData,
            TokenAuthenticationRequest request,
            TenantConfigContext resolvedContext, UserInfo userInfo) {
        Uni<TokenVerificationResult> codeAccessTokenUni = verifyCodeFlowAccessTokenUni(requestData, request, resolvedContext,
                userInfo);

        return codeAccessTokenUni.onItemOrFailure().transformToUni(
                new BiFunction<TokenVerificationResult, Throwable, Uni<? extends SecurityIdentity>>() {
                    @Override
                    public Uni<SecurityIdentity> apply(TokenVerificationResult codeAccessToken, Throwable t) {
                        if (t != null) {
                            return Uni.createFrom().failure(new AuthenticationFailedException(t));
                        }

                        if (codeAccessToken != null) {
                            requestData.put(CODE_ACCESS_TOKEN_RESULT, codeAccessToken);
                        }

                        Uni<TokenVerificationResult> tokenUni = verifyTokenUni(requestData, resolvedContext,
                                request.getToken(),
                                false, userInfo);

                        return tokenUni.onItemOrFailure()
                                .transformToUni(
                                        new BiFunction<TokenVerificationResult, Throwable, Uni<? extends SecurityIdentity>>() {
                                            @Override
                                            public Uni<SecurityIdentity> apply(TokenVerificationResult result, Throwable t) {
                                                if (t != null) {
                                                    return Uni.createFrom().failure(new AuthenticationFailedException(t));
                                                }

                                                return createSecurityIdentityWithOidcServer(result, requestData, request,
                                                        resolvedContext, userInfo);
                                            }
                                        });

                    }
                });
    }

    private Uni<SecurityIdentity> getUserInfoAndCreateIdentity(Uni<TokenVerificationResult> tokenUni,
            Map<String, Object> requestData, TokenAuthenticationRequest request,
            TenantConfigContext resolvedContext) {

        return tokenUni.onItemOrFailure()
                .transformToUni(new BiFunction<TokenVerificationResult, Throwable, Uni<? extends SecurityIdentity>>() {
                    @Override
                    public Uni<SecurityIdentity> apply(TokenVerificationResult result, Throwable t) {
                        if (t != null) {
                            return Uni.createFrom().failure(new AuthenticationFailedException(t));
                        }
                        if (resolvedContext.oidcConfig.authentication.isUserInfoRequired().orElse(false)) {
                            return getUserInfoUni(requestData, request, resolvedContext).onItemOrFailure().transformToUni(
                                    new BiFunction<UserInfo, Throwable, Uni<? extends SecurityIdentity>>() {
                                        @Override
                                        public Uni<SecurityIdentity> apply(UserInfo userInfo, Throwable t) {
                                            if (t != null) {
                                                return Uni.createFrom().failure(new AuthenticationFailedException(t));
                                            }
                                            return createSecurityIdentityWithOidcServer(result, requestData, request,
                                                    resolvedContext, userInfo);
                                        }
                                    });
                        } else {
                            return createSecurityIdentityWithOidcServer(result, requestData, request, resolvedContext, null);
                        }

                    }
                });

    }

    private boolean isOpaqueAccessToken(Map<String, Object> requestData, TokenAuthenticationRequest request,
            TenantConfigContext resolvedContext) {
        if (request.getToken() instanceof AccessTokenCredential) {
            return ((AccessTokenCredential) request.getToken()).isOpaque();
        } else if (request.getToken() instanceof IdTokenCredential
                && (resolvedContext.oidcConfig.authentication.verifyAccessToken
                        || resolvedContext.oidcConfig.roles.source.orElse(null) == Source.accesstoken)) {
            final String codeAccessToken = (String) requestData.get(OidcConstants.ACCESS_TOKEN_VALUE);
            return OidcUtils.isOpaqueToken(codeAccessToken);
        }
        return false;
    }

    private Uni<SecurityIdentity> createSecurityIdentityWithOidcServer(TokenVerificationResult result,
            Map<String, Object> requestData, TokenAuthenticationRequest request, TenantConfigContext resolvedContext,
            final UserInfo userInfo) {

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
                if (userInfo != null && resolvedContext.oidcConfig.token.isSubjectRequired()
                        && !tokenJson.getString(Claims.sub.name()).equals(userInfo.getString(Claims.sub.name()))) {
                    String errorMessage = String
                            .format("Token and UserInfo do not have matching `sub` claims");
                    return Uni.createFrom().failure(new AuthenticationCompletionException(errorMessage));
                }

                JsonObject rolesJson = getRolesJson(requestData, resolvedContext, tokenCred, tokenJson,
                        userInfo);
                SecurityIdentity securityIdentity = validateAndCreateIdentity(requestData, tokenCred,
                        resolvedContext, tokenJson, rolesJson, userInfo, result.introspectionResult, request);
                // If the primary token is a bearer access token then there's no point of checking if
                // it should be refreshed as RT is only available for the code flow tokens
                if (isIdToken(request)
                        && tokenAutoRefreshPrepared(result, requestData, resolvedContext.oidcConfig)) {
                    return Uni.createFrom().failure(new TokenAutoRefreshException(securityIdentity));
                } else {
                    return Uni.createFrom().item(securityIdentity);
                }
            } catch (Throwable ex) {
                return Uni.createFrom().failure(new AuthenticationFailedException(ex));
            }
        } else if (isIdToken(request)
                || tokenCred instanceof AccessTokenCredential
                        && !((AccessTokenCredential) tokenCred).isOpaque()) {
            return Uni.createFrom()
                    .failure(new AuthenticationFailedException("JWT token can not be converted to JSON"));
        } else {
            // ID Token or Bearer access token has been introspected or verified via Userinfo acquisition
            QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
            builder.addCredential(tokenCred);
            OidcUtils.setSecurityIdentityUserInfo(builder, userInfo);
            OidcUtils.setSecurityIdentityConfigMetadata(builder, resolvedContext);
            final String userName;
            if (result.introspectionResult == null) {
                if (resolvedContext.oidcConfig.token.allowOpaqueTokenIntrospection &&
                        resolvedContext.oidcConfig.token.verifyAccessTokenWithUserInfo.orElse(false)) {
                    if (resolvedContext.oidcConfig.token.principalClaim.isPresent() && userInfo != null) {
                        userName = userInfo.getString(resolvedContext.oidcConfig.token.principalClaim.get());
                    } else {
                        userName = "";
                    }
                } else {
                    // we don't expect this to ever happen
                    LOG.debug("Illegal state - token introspection result is not available.");
                    return Uni.createFrom().failure(new AuthenticationFailedException());
                }
            } else {
                OidcUtils.setSecurityIdentityIntrospection(builder, result.introspectionResult);
                String principalName = result.introspectionResult.getUsername();
                if (principalName == null) {
                    principalName = result.introspectionResult.getSubject();
                }
                userName = principalName != null ? principalName : "";

                Set<String> scopes = result.introspectionResult.getScopes();
                if (scopes != null) {
                    builder.addRoles(scopes);
                    OidcUtils.addTokenScopesAsPermissions(builder, scopes);
                }
            }
            builder.setPrincipal(new Principal() {
                @Override
                public String getName() {
                    return userName != null ? userName : "";
                }
            });
            if (userInfo != null) {
                var rolesJson = new JsonObject(userInfo.getJsonObject().toString());
                OidcUtils.setSecurityIdentityRoles(builder, resolvedContext.oidcConfig, rolesJson);
                OidcUtils.setSecurityIdentityPermissions(builder, resolvedContext.oidcConfig, rolesJson);
            }
            OidcUtils.setTenantIdAttribute(builder, resolvedContext.oidcConfig);
            var vertxContext = getRoutingContextAttribute(request);
            OidcUtils.setBlockingApiAttribute(builder, vertxContext);
            OidcUtils.setRoutingContextAttribute(builder, vertxContext);
            SecurityIdentity identity = builder.build();
            // If the primary token is a bearer access token then there's no point of checking if
            // it should be refreshed as RT is only available for the code flow tokens
            if (isIdToken(request)
                    && tokenAutoRefreshPrepared(result, requestData, resolvedContext.oidcConfig)) {
                return Uni.createFrom().failure(new TokenAutoRefreshException(identity));
            }
            return Uni.createFrom().item(identity);
        }

    }

    private static boolean isInternalIdToken(TokenAuthenticationRequest request) {
        return isIdToken(request) && ((IdTokenCredential) request.getToken()).isInternal();
    }

    private static boolean isIdToken(TokenAuthenticationRequest request) {
        return request.getToken() instanceof IdTokenCredential;
    }

    private static boolean tokenAutoRefreshPrepared(TokenVerificationResult result, Map<String, Object> requestData,
            OidcTenantConfig oidcConfig) {
        if (result != null && oidcConfig.token.refreshExpired
                && oidcConfig.token.getRefreshTokenTimeSkew().isPresent()
                && requestData.get(REFRESH_TOKEN_GRANT_RESPONSE) != Boolean.TRUE
                && requestData.get(NEW_AUTHENTICATION) != Boolean.TRUE) {
            Long expiry = null;
            if (result.localVerificationResult != null) {
                expiry = result.localVerificationResult.getLong(Claims.exp.name());
            } else if (result.introspectionResult != null) {
                expiry = result.introspectionResult.getLong(OidcConstants.INTROSPECTION_TOKEN_EXP);
            }
            if (expiry != null) {
                final long refreshTokenTimeSkew = oidcConfig.token.getRefreshTokenTimeSkew().get().getSeconds();
                final long now = System.currentTimeMillis() / 1000;
                return now + refreshTokenTimeSkew > expiry;
            }
        }
        return false;
    }

    private static JsonObject getRolesJson(Map<String, Object> requestData, TenantConfigContext resolvedContext,
            TokenCredential tokenCred,
            JsonObject tokenJson, UserInfo userInfo) {
        JsonObject rolesJson = tokenJson;
        if (resolvedContext.oidcConfig.roles.source.isPresent()) {
            if (resolvedContext.oidcConfig.roles.source.get() == Source.userinfo) {
                rolesJson = new JsonObject(userInfo.getJsonObject().toString());
            } else if (tokenCred instanceof IdTokenCredential
                    && resolvedContext.oidcConfig.roles.source.get() == Source.accesstoken) {
                rolesJson = ((TokenVerificationResult) requestData.get(CODE_ACCESS_TOKEN_RESULT)).localVerificationResult;
                if (rolesJson == null) {
                    // JSON token representation may be null not only if it is an opaque access token
                    // but also if it is JWT and no JWK with a matching kid is available, asynchronous
                    // JWK refresh has not finished yet, but the fallback introspection request has succeeded.
                    rolesJson = OidcUtils.decodeJwtContent((String) requestData.get(OidcConstants.ACCESS_TOKEN_VALUE));
                }
            }
        }
        return rolesJson;
    }

    private Uni<TokenVerificationResult> verifyCodeFlowAccessTokenUni(Map<String, Object> requestData,
            TokenAuthenticationRequest request,
            TenantConfigContext resolvedContext, UserInfo userInfo) {
        if (request.getToken() instanceof IdTokenCredential
                && (resolvedContext.oidcConfig.authentication.verifyAccessToken
                        || resolvedContext.oidcConfig.roles.source.orElse(null) == Source.accesstoken)) {
            final String codeAccessToken = (String) requestData.get(OidcConstants.ACCESS_TOKEN_VALUE);
            return verifyTokenUni(requestData, resolvedContext, new AccessTokenCredential(codeAccessToken), false, userInfo);
        } else {
            return NULL_CODE_ACCESS_TOKEN_UNI;
        }
    }

    private Uni<TokenVerificationResult> verifyTokenUni(Map<String, Object> requestData, TenantConfigContext resolvedContext,
            TokenCredential tokenCred, boolean enforceAudienceVerification, UserInfo userInfo) {
        final String token = tokenCred.getToken();
        if (OidcUtils.isOpaqueToken(token)) {
            if (!resolvedContext.oidcConfig.token.allowOpaqueTokenIntrospection) {
                LOG.debug("Token is opaque but the opaque token introspection is not allowed");
                throw new AuthenticationFailedException();
            }
            // verify opaque access token with UserInfo if enabled and introspection URI is absent
            if (resolvedContext.oidcConfig.token.verifyAccessTokenWithUserInfo.orElse(false)
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
            return introspectTokenUni(resolvedContext, token, false);
        } else if (resolvedContext.provider.getMetadata().getJsonWebKeySetUri() == null
                || resolvedContext.oidcConfig.token.requireJwtIntrospectionOnly) {
            // Verify JWT token with the remote introspection
            LOG.debug("Starting the JWT token introspection");
            return introspectTokenUni(resolvedContext, token, false);
        } else if (resolvedContext.oidcConfig.jwks.resolveEarly) {
            // Verify JWT token with the local JWK keys with a possible remote introspection fallback
            final String nonce = (String) requestData.get(OidcConstants.NONCE);
            try {
                LOG.debug("Verifying the JWT token with the local JWK keys");
                return Uni.createFrom()
                        .item(resolvedContext.provider.verifyJwtToken(token, enforceAudienceVerification,
                                resolvedContext.oidcConfig.token.isSubjectRequired(), nonce));
            } catch (Throwable t) {
                if (t.getCause() instanceof UnresolvableKeyException) {
                    LOG.debug("No matching JWK key is found, refreshing and repeating the verification");
                    return refreshJwksAndVerifyTokenUni(resolvedContext, token, enforceAudienceVerification,
                            resolvedContext.oidcConfig.token.isSubjectRequired(), nonce);
                } else {
                    LOG.debugf("Token verification has failed: %s", t.getMessage());
                    return Uni.createFrom().failure(t);
                }
            }
        } else {
            final String nonce = (String) requestData.get(OidcConstants.NONCE);
            return resolveJwksAndVerifyTokenUni(resolvedContext, tokenCred, enforceAudienceVerification,
                    resolvedContext.oidcConfig.token.isSubjectRequired(), nonce);
        }
    }

    private Uni<TokenVerificationResult> verifySelfSignedTokenUni(TenantConfigContext resolvedContext, String token) {
        try {
            return Uni.createFrom().item(resolvedContext.provider.verifySelfSignedJwtToken(token));
        } catch (Throwable t) {
            return Uni.createFrom().failure(t);
        }
    }

    private Uni<TokenVerificationResult> refreshJwksAndVerifyTokenUni(TenantConfigContext resolvedContext, String token,
            boolean enforceAudienceVerification, boolean subjectRequired, String nonce) {
        return resolvedContext.provider.refreshJwksAndVerifyJwtToken(token, enforceAudienceVerification, subjectRequired, nonce)
                .onFailure(f -> fallbackToIntrospectionIfNoMatchingKey(f, resolvedContext))
                .recoverWithUni(f -> introspectTokenUni(resolvedContext, token, true));
    }

    private Uni<TokenVerificationResult> resolveJwksAndVerifyTokenUni(TenantConfigContext resolvedContext,
            TokenCredential tokenCred,
            boolean enforceAudienceVerification, boolean subjectRequired, String nonce) {
        return resolvedContext.provider
                .getKeyResolverAndVerifyJwtToken(tokenCred, enforceAudienceVerification, subjectRequired, nonce)
                .onFailure(f -> fallbackToIntrospectionIfNoMatchingKey(f, resolvedContext))
                .recoverWithUni(f -> introspectTokenUni(resolvedContext, tokenCred.getToken(), true));
    }

    private static boolean fallbackToIntrospectionIfNoMatchingKey(Throwable f, TenantConfigContext resolvedContext) {
        if (!(f.getCause() instanceof UnresolvableKeyException)) {
            LOG.debug("Local JWT token verification has failed, skipping the token introspection");
            return false;
        } else if (!resolvedContext.oidcConfig.token.allowJwtIntrospection) {
            LOG.debug("JWT token does not have a matching verification key but JWT token introspection is disabled");
            return false;
        } else {
            LOG.debug("Local JWT token verification has failed, attempting the token introspection");
            return true;
        }

    }

    private Uni<TokenVerificationResult> introspectTokenUni(TenantConfigContext resolvedContext, final String token,
            boolean fallbackFromJwkMatch) {
        TokenIntrospectionCache tokenIntrospectionCache = tenantResolver.getTokenIntrospectionCache();
        Uni<TokenIntrospection> tokenIntrospectionUni = tokenIntrospectionCache == null ? null
                : tokenIntrospectionCache
                        .getIntrospection(token, resolvedContext.oidcConfig, getIntrospectionRequestContext);
        if (tokenIntrospectionUni == null) {
            tokenIntrospectionUni = newTokenIntrospectionUni(resolvedContext, token, fallbackFromJwkMatch);
        } else {
            tokenIntrospectionUni = tokenIntrospectionUni.onItem().ifNull()
                    .switchTo(new Supplier<Uni<? extends TokenIntrospection>>() {
                        @Override
                        public Uni<TokenIntrospection> get() {
                            return newTokenIntrospectionUni(resolvedContext, token, fallbackFromJwkMatch);
                        }
                    });
        }
        return tokenIntrospectionUni.onItem().transform(t -> new TokenVerificationResult(null, t));
    }

    private Uni<TokenIntrospection> newTokenIntrospectionUni(TenantConfigContext resolvedContext, String token,
            boolean fallbackFromJwkMatch) {
        Uni<TokenIntrospection> tokenIntrospectionUni = resolvedContext.provider.introspectToken(token, fallbackFromJwkMatch);
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
            TokenVerificationResult result = resolvedContext.provider.verifyJwtToken(request.getToken().getToken(), false,
                    false, null);
            return Uni.createFrom()
                    .item(validateAndCreateIdentity(Map.of(), request.getToken(), resolvedContext,
                            result.localVerificationResult, result.localVerificationResult, null, null, request));
        } catch (Throwable t) {
            return Uni.createFrom().failure(new AuthenticationFailedException(t));
        }
    }

    private Uni<UserInfo> getUserInfoUni(Map<String, Object> requestData, TokenAuthenticationRequest request,
            TenantConfigContext resolvedContext) {
        if (isInternalIdToken(request) && resolvedContext.oidcConfig.cacheUserInfoInIdtoken) {
            JsonObject userInfo = OidcUtils.decodeJwtContent(request.getToken().getToken())
                    .getJsonObject(OidcUtils.USER_INFO_ATTRIBUTE);
            if (userInfo != null) {
                return Uni.createFrom().item(new UserInfo(userInfo.encode()));
            }
        }

        LOG.debug("Requesting UserInfo");
        String contextAccessToken = (String) requestData.get(OidcConstants.ACCESS_TOKEN_VALUE);
        final String accessToken = contextAccessToken != null ? contextAccessToken : request.getToken().getToken();

        UserInfoCache userInfoCache = tenantResolver.getUserInfoCache();
        Uni<UserInfo> userInfoUni = userInfoCache == null ? null
                : userInfoCache.getUserInfo(accessToken, resolvedContext.oidcConfig, getUserInfoRequestContext);
        if (userInfoUni == null) {
            userInfoUni = newUserInfoUni(resolvedContext, accessToken);
        } else {
            userInfoUni = userInfoUni.onItem().ifNull()
                    .switchTo(new Supplier<Uni<? extends UserInfo>>() {
                        @Override
                        public Uni<UserInfo> get() {
                            return newUserInfoUni(resolvedContext, accessToken);
                        }
                    });
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
