package io.quarkus.oidc.runtime;

import static io.quarkus.oidc.runtime.OidcUtils.validateAndCreateIdentity;
import static io.quarkus.vertx.http.runtime.security.HttpSecurityUtils.getRoutingContextAttribute;

import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.jwt.Claims;
import org.jboss.logging.Logger;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;
import org.jose4j.lang.UnresolvableKeyException;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.AuthorizationCodeTokens;
import io.quarkus.oidc.IdTokenCredential;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TokenIntrospection;
import io.quarkus.oidc.TokenIntrospectionCache;
import io.quarkus.oidc.UserInfo;
import io.quarkus.oidc.UserInfoCache;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.oidc.runtime.OidcTenantConfig.Roles.Source;
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
        if (!isIdToken(request.getToken()) && resolvedContext.oidcConfig().token().decryptAccessToken()) {
            String decryptedToken = OidcUtils.decryptToken(resolvedContext, request.getToken().getToken());
            request = new TokenAuthenticationRequest(new AccessTokenCredential(decryptedToken));
        }
        if (resolvedContext.oidcConfig().authServerUrl().isPresent()) {
            return validateAllTokensWithOidcServer(requestData, request, resolvedContext);
        } else if (resolvedContext.oidcConfig().certificateChain().trustStoreFile().isPresent()) {
            LOG.debug("Performing token verification with a public key inlined in the certificate chain");
            return validateTokenWithoutOidcServer(request, resolvedContext);
        } else if (resolvedContext.oidcConfig().publicKey().isPresent()) {
            LOG.debug("Performing token verification with a configured public key");
            return validateTokenWithoutOidcServer(request, resolvedContext);
        } else {
            return Uni.createFrom().failure(new OIDCException("Unexpected authentication request"));
        }
    }

    private Uni<SecurityIdentity> validateAllTokensWithOidcServer(Map<String, Object> requestData,
            TokenAuthenticationRequest request, TenantConfigContext resolvedContext) {

        if (resolvedContext.oidcConfig().token().verifyAccessTokenWithUserInfo().orElse(false)
                && isOpaqueAccessToken(requestData, request, resolvedContext)) {
            // UserInfo has to be acquired first as a precondition for verifying opaque access tokens.
            // Typically it will be done for bearer access tokens therefore even if the access token has expired
            // the client will be able to refresh if needed, no refresh token is available to Quarkus during the
            // bearer access token verification
            if (resolvedContext.oidcConfig().authentication().userInfoRequired().orElse(false)) {
                return getUserInfoUni(requestData, request, resolvedContext).onItemOrFailure().transformToUni(
                        new BiFunction<UserInfo, Throwable, Uni<? extends SecurityIdentity>>() {
                            @Override
                            public Uni<SecurityIdentity> apply(UserInfo userInfo, Throwable t) {
                                if (t != null) {
                                    return Uni.createFrom().failure(t instanceof AuthenticationFailedException ? t
                                            : new AuthenticationFailedException(t));
                                }
                                return validateTokenWithUserInfoAndCreateIdentity(requestData, request, resolvedContext,
                                        userInfo);
                            }
                        });
            } else {
                return validateTokenWithUserInfoAndCreateIdentity(requestData, request, resolvedContext, null);
            }
        } else {
            final Uni<TokenVerificationResult> primaryTokenUni = verifyPrimaryTokenUni(requestData, request, resolvedContext,
                    null);

            return getUserInfoAndCreateIdentity(primaryTokenUni, requestData, request, resolvedContext);
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
                            requestData.put(OidcUtils.CODE_ACCESS_TOKEN_FAILURE, t);
                            return Uni.createFrom().failure(t instanceof AuthenticationFailedException ? t
                                    : new AuthenticationFailedException(t, codeAccessTokenMap(requestData)));
                        }

                        if (codeAccessToken != null) {
                            requestData.put(OidcUtils.CODE_ACCESS_TOKEN_RESULT, codeAccessToken);
                        }

                        Uni<TokenVerificationResult> primaryTokenUni = verifyPrimaryTokenUni(requestData, request,
                                resolvedContext, userInfo);

                        return primaryTokenUni.onItemOrFailure()
                                .transformToUni(
                                        new BiFunction<TokenVerificationResult, Throwable, Uni<? extends SecurityIdentity>>() {
                                            @Override
                                            public Uni<SecurityIdentity> apply(TokenVerificationResult result, Throwable t) {
                                                if (t != null) {
                                                    return Uni.createFrom()
                                                            .failure(t instanceof AuthenticationFailedException ? t
                                                                    : new AuthenticationFailedException(t,
                                                                            tokenMap(request.getToken())));
                                                }

                                                return createSecurityIdentityWithOidcServer(result, requestData, request,
                                                        resolvedContext, userInfo);
                                            }
                                        });

                    }
                });
    }

    private Uni<TokenVerificationResult> verifyPrimaryTokenUni(Map<String, Object> requestData,
            TokenAuthenticationRequest request, TenantConfigContext resolvedContext, UserInfo userInfo) {
        StepUpAuthenticationPolicy stepUpAuthPolicy = StepUpAuthenticationPolicy.getFromRequest(request);
        if (isInternalIdToken(request)) {
            if (stepUpAuthPolicy != null) {
                return Uni.createFrom().failure(new OIDCException(
                        "The @AuthenticationContext annotation cannot be used with an internal ID token"));
            }
            if (requestData.get(NEW_AUTHENTICATION) == Boolean.TRUE) {
                // No need to verify it in this case as 'CodeAuthenticationMechanism' has just created it
                return Uni.createFrom()
                        .item(new TokenVerificationResult(OidcCommonUtils.decodeJwtContent(request.getToken().getToken()),
                                null));
            } else {
                return verifySelfSignedTokenUni(resolvedContext, request.getToken().getToken());
            }
        } else {
            final boolean idToken = isIdToken(request);
            Uni<TokenVerificationResult> result = verifyTokenUni(requestData, resolvedContext, request.getToken(), idToken,
                    idToken, false, userInfo);
            if (!idToken) {
                if (resolvedContext.oidcConfig().token().binding().certificate()) {
                    result = result.onItem().transform(new Function<TokenVerificationResult, TokenVerificationResult>() {

                        @Override
                        public TokenVerificationResult apply(TokenVerificationResult t) {
                            String tokenCertificateThumbprint = getTokenCertThumbprint(requestData, t);
                            if (tokenCertificateThumbprint == null) {
                                LOG.warn(
                                        "Access token does not contain a confirmation 'cnf' claim with the certificate thumbprint");
                                throw new AuthenticationFailedException(tokenMap(request.getToken()));
                            }
                            String clientCertificateThumbprint = (String) requestData.get(OidcConstants.X509_SHA256_THUMBPRINT);
                            if (clientCertificateThumbprint == null) {
                                LOG.warn("Client certificate thumbprint is not available");
                                throw new AuthenticationFailedException(tokenMap(request.getToken()));
                            }
                            if (!clientCertificateThumbprint.equals(tokenCertificateThumbprint)) {
                                LOG.warn("Client certificate thumbprint does not match the token certificate thumbprint");
                                throw new AuthenticationFailedException(tokenMap(request.getToken()));
                            }
                            return t;
                        }

                    });
                }

                if (requestData.containsKey(OidcUtils.DPOP_PROOF_JWT_HEADERS)) {
                    result = result.onItem().transform(new Function<TokenVerificationResult, TokenVerificationResult>() {

                        @Override
                        public TokenVerificationResult apply(TokenVerificationResult t) {

                            String dpopJwkThumbprint = getDpopJwkThumbprint(requestData, t);
                            if (dpopJwkThumbprint == null) {
                                LOG.warn(
                                        "DPoP access token does not contain a confirmation 'cnf' claim with the JWK thumbprint");
                                throw new AuthenticationFailedException(tokenMap(request.getToken()));
                            }

                            JsonObject proofHeaders = (JsonObject) requestData.get(OidcUtils.DPOP_PROOF_JWT_HEADERS);

                            JsonObject jwkProof = proofHeaders.getJsonObject(OidcConstants.DPOP_JWK_HEADER);
                            if (jwkProof == null) {
                                LOG.warn("DPoP proof jwk header is missing");
                                throw new AuthenticationFailedException(tokenMap(request.getToken()));
                            }

                            PublicJsonWebKey publicJsonWebKey = null;
                            try {
                                publicJsonWebKey = PublicJsonWebKey.Factory.newPublicJwk(jwkProof.getMap());
                            } catch (JoseException ex) {
                                LOG.warn("DPoP proof jwk header does not represent a valid JWK key");
                                throw new AuthenticationFailedException(ex, tokenMap(request.getToken()));
                            }

                            if (publicJsonWebKey.getPrivateKey() != null) {
                                LOG.warn("DPoP proof JWK key is a private key but it must be a public key");
                                throw new AuthenticationFailedException(tokenMap(request.getToken()));
                            }

                            byte[] jwkProofDigest = publicJsonWebKey.calculateThumbprint("SHA-256");
                            String jwkProofThumbprint = OidcCommonUtils.base64UrlEncode(jwkProofDigest);

                            if (!dpopJwkThumbprint.equals(jwkProofThumbprint)) {
                                LOG.warn("DPoP access token JWK thumbprint does not match the DPoP proof JWK thumbprint");
                                throw new AuthenticationFailedException(tokenMap(request.getToken()));
                            }

                            try {
                                JsonWebSignature jws = new JsonWebSignature();
                                jws.setAlgorithmConstraints(OidcProvider.ASYMMETRIC_ALGORITHM_CONSTRAINTS);
                                jws.setCompactSerialization((String) requestData.get(OidcUtils.DPOP_PROOF));
                                jws.setKey(publicJsonWebKey.getPublicKey());
                                if (!jws.verifySignature()) {
                                    LOG.warn("DPoP proof token signature is invalid");
                                    throw new AuthenticationFailedException(tokenMap(request.getToken()));
                                }
                            } catch (JoseException ex) {
                                LOG.warn("DPoP proof token signature can not be verified");
                                throw new AuthenticationFailedException(ex, tokenMap(request.getToken()));
                            }

                            JsonObject proofClaims = (JsonObject) requestData.get(OidcUtils.DPOP_PROOF_JWT_CLAIMS);

                            // Calculate the access token thumprint and compare with the `ath` claim

                            String accessTokenProof = proofClaims.getString(OidcConstants.DPOP_ACCESS_TOKEN_THUMBPRINT);
                            if (accessTokenProof == null) {
                                LOG.warn("DPoP proof access token hash is missing");
                                throw new AuthenticationFailedException(tokenMap(request.getToken()));
                            }

                            String accessTokenHash = null;
                            try {
                                accessTokenHash = OidcCommonUtils.base64UrlEncode(
                                        OidcUtils.getSha256Digest(request.getToken().getToken()));
                            } catch (NoSuchAlgorithmException ex) {
                                // SHA256 is always supported
                            }

                            if (!accessTokenProof.equals(accessTokenHash)) {
                                LOG.warn("DPoP access token hash does not match the DPoP proof access token hash");
                                throw new AuthenticationFailedException(tokenMap(request.getToken()));
                            }

                            return t;
                        }

                    });
                }
            }

            if (stepUpAuthPolicy != null) {
                result = result.invoke(stepUpAuthPolicy);
            }

            return result;
        }
    }

    private static String getTokenCertThumbprint(Map<String, Object> requestData, TokenVerificationResult t) {
        JsonObject json = t.localVerificationResult != null ? t.localVerificationResult
                : new JsonObject(t.introspectionResult.getIntrospectionString());
        JsonObject cnf = json.getJsonObject(OidcConstants.CONFIRMATION_CLAIM);
        String thumbprint = cnf == null ? null : cnf.getString(OidcConstants.X509_SHA256_THUMBPRINT);
        if (thumbprint != null) {
            requestData.put((t.introspectionResult == null ? OidcUtils.JWT_THUMBPRINT : OidcUtils.INTROSPECTION_THUMBPRINT),
                    true);
        }
        return thumbprint;
    }

    private static String getDpopJwkThumbprint(Map<String, Object> requestData, TokenVerificationResult t) {
        JsonObject json = t.localVerificationResult != null ? t.localVerificationResult
                : new JsonObject(t.introspectionResult.getIntrospectionString());
        JsonObject cnf = json.getJsonObject(OidcConstants.CONFIRMATION_CLAIM);
        String thumbprint = cnf == null ? null : cnf.getString(OidcConstants.DPOP_JWK_SHA256_THUMBPRINT);
        if (thumbprint != null) {
            requestData.put(
                    (t.introspectionResult == null ? OidcUtils.DPOP_JWT_THUMBPRINT : OidcUtils.DPOP_INTROSPECTION_THUMBPRINT),
                    true);
        }
        return thumbprint;
    }

    private Uni<SecurityIdentity> getUserInfoAndCreateIdentity(Uni<TokenVerificationResult> tokenUni,
            Map<String, Object> requestData,
            TokenAuthenticationRequest request,
            TenantConfigContext resolvedContext) {

        return tokenUni.onItemOrFailure()
                .transformToUni(new BiFunction<TokenVerificationResult, Throwable, Uni<? extends SecurityIdentity>>() {
                    @Override
                    public Uni<SecurityIdentity> apply(TokenVerificationResult result, Throwable t) {
                        if (t != null) {
                            return Uni.createFrom().failure(t instanceof AuthenticationFailedException ? t
                                    : new AuthenticationFailedException(t, tokenMap(request.getToken())));
                        }

                        Uni<TokenVerificationResult> codeAccessTokenUni = verifyCodeFlowAccessTokenUni(requestData, request,
                                resolvedContext, null);
                        return codeAccessTokenUni.onItemOrFailure().transformToUni(
                                new BiFunction<TokenVerificationResult, Throwable, Uni<? extends SecurityIdentity>>() {
                                    @Override
                                    public Uni<SecurityIdentity> apply(TokenVerificationResult codeAccessTokenResult,
                                            Throwable t) {
                                        if (t != null) {
                                            requestData.put(OidcUtils.CODE_ACCESS_TOKEN_FAILURE, t);
                                            return Uni.createFrom().failure(t instanceof AuthenticationFailedException ? t
                                                    : new AuthenticationFailedException(t, codeAccessTokenMap(requestData)));
                                        }
                                        if (codeAccessTokenResult != null) {
                                            if (tokenAutoRefreshPrepared(codeAccessTokenResult, requestData,
                                                    resolvedContext.oidcConfig(), true)) {
                                                return Uni.createFrom().failure(new TokenAutoRefreshException(null));
                                            }
                                            requestData.put(OidcUtils.CODE_ACCESS_TOKEN_RESULT, codeAccessTokenResult);
                                        }

                                        if (resolvedContext.oidcConfig().authentication().userInfoRequired().orElse(false)) {
                                            return getUserInfoUni(requestData, request, resolvedContext).onItemOrFailure()
                                                    .transformToUni(
                                                            new BiFunction<UserInfo, Throwable, Uni<? extends SecurityIdentity>>() {
                                                                @Override
                                                                public Uni<SecurityIdentity> apply(UserInfo userInfo,
                                                                        Throwable t) {
                                                                    if (t != null) {
                                                                        return Uni.createFrom().failure(
                                                                                t instanceof AuthenticationFailedException ? t
                                                                                        : new AuthenticationFailedException(t));
                                                                    }
                                                                    return createSecurityIdentityWithOidcServer(result,
                                                                            requestData, request, resolvedContext, userInfo);
                                                                }
                                                            });
                                        } else {
                                            return createSecurityIdentityWithOidcServer(result, requestData, request,
                                                    resolvedContext, null);
                                        }
                                    }
                                });

                    }
                });

    }

    private boolean isOpaqueAccessToken(Map<String, Object> requestData, TokenAuthenticationRequest request,
            TenantConfigContext resolvedContext) {
        if (request.getToken() instanceof AccessTokenCredential) {
            return ((AccessTokenCredential) request.getToken()).isOpaque();
        } else if (request.getToken() instanceof IdTokenCredential
                && (resolvedContext.oidcConfig().authentication().verifyAccessToken()
                        || resolvedContext.oidcConfig().roles().source().orElse(null) == Source.accesstoken)) {
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
            tokenJson = OidcCommonUtils.decodeJwtContent(tokenCred.getToken());
        }
        if (tokenJson != null) {
            try {
                OidcUtils.validatePrimaryJwtTokenType(resolvedContext.oidcConfig().token(), tokenJson);
                if (userInfo != null && resolvedContext.oidcConfig().token().subjectRequired()
                        && !tokenJson.getString(Claims.sub.name()).equals(userInfo.getString(Claims.sub.name()))) {
                    String errorMessage = "Token and UserInfo do not have matching `sub` claims";
                    return Uni.createFrom().failure(new AuthenticationCompletionException(errorMessage));
                }
                final String principalClaim = resolvedContext.oidcConfig().token().principalClaim().orElse(null);
                if (principalClaim != null && !tokenJson.containsKey(principalClaim) && userInfo != null
                        && userInfo.contains(principalClaim)) {
                    tokenJson.put(principalClaim, userInfo.getString(principalClaim));
                }
                JsonObject rolesJson = getRolesJson(requestData, resolvedContext, tokenCred, tokenJson,
                        userInfo);
                SecurityIdentity securityIdentity = validateAndCreateIdentity(requestData, tokenCred,
                        resolvedContext, tokenJson, rolesJson, userInfo, result.introspectionResult, request);
                // If the primary token is a bearer access token then there's no point of checking if
                // it should be refreshed as RT is only available for the code flow tokens
                if (isIdToken(tokenCred)
                        && tokenAutoRefreshPrepared(result, requestData, resolvedContext.oidcConfig(), false)) {
                    return Uni.createFrom().failure(new TokenAutoRefreshException(securityIdentity));
                } else {
                    return Uni.createFrom().item(securityIdentity);
                }
            } catch (Throwable ex) {
                return Uni.createFrom().failure(ex instanceof AuthenticationFailedException ? ex
                        : new AuthenticationFailedException(ex, tokenMap(tokenCred)));
            }
        } else if (isIdToken(tokenCred)
                || tokenCred instanceof AccessTokenCredential
                        && !((AccessTokenCredential) tokenCred).isOpaque()) {
            return Uni.createFrom()
                    .failure(new AuthenticationFailedException("JWT token can not be converted to JSON", tokenMap(tokenCred)));
        } else {
            // ID Token or Bearer access token has been introspected or verified via Userinfo acquisition
            QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
            builder.addCredential(tokenCred);
            OidcUtils.setSecurityIdentityUserInfo(builder, userInfo);
            OidcUtils.setSecurityIdentityConfigMetadata(builder, resolvedContext);
            final String userName;
            if (result.introspectionResult == null) {
                if (resolvedContext.oidcConfig().token().allowOpaqueTokenIntrospection() &&
                        resolvedContext.oidcConfig().token().verifyAccessTokenWithUserInfo().orElse(false)) {
                    if (resolvedContext.oidcConfig().token().principalClaim().isPresent() && userInfo != null) {
                        userName = userInfo.getString(resolvedContext.oidcConfig().token().principalClaim().get());
                    } else {
                        userName = "";
                    }
                } else {
                    // we don't expect this to ever happen
                    LOG.debug("Illegal state - token introspection result is not available.");
                    return Uni.createFrom().failure(new AuthenticationFailedException(tokenMap(tokenCred)));
                }
            } else {
                OidcUtils.setSecurityIdentityIntrospection(builder, result.introspectionResult);
                String principalName = result.introspectionResult.getUsername();
                if (principalName == null) {
                    principalName = result.introspectionResult.getSubject();
                }
                userName = principalName != null ? principalName : "";
                OidcUtils.setIntrospectionScopes(builder, result.introspectionResult);
            }
            builder.setPrincipal(new Principal() {
                @Override
                public String getName() {
                    return userName != null ? userName : "";
                }
            });
            if (userInfo != null) {
                var rolesJson = new JsonObject(userInfo.getJsonObject().toString());
                OidcUtils.setSecurityIdentityRoles(builder, resolvedContext.oidcConfig(), rolesJson);
                OidcUtils.setSecurityIdentityPermissions(builder, resolvedContext.oidcConfig(), rolesJson);
            }
            OidcUtils.setTenantIdAttribute(builder, resolvedContext.oidcConfig());
            var vertxContext = getRoutingContextAttribute(request);
            OidcUtils.setBlockingApiAttribute(builder, vertxContext);
            OidcUtils.setRoutingContextAttribute(builder, vertxContext);
            OidcUtils.setOidcProviderClientAttribute(builder, resolvedContext.getOidcProviderClient());
            SecurityIdentity identity = builder.build();
            // If the primary token is a bearer access token then there's no point of checking if
            // it should be refreshed as RT is only available for the code flow tokens
            if (isIdToken(tokenCred)
                    && tokenAutoRefreshPrepared(result, requestData, resolvedContext.oidcConfig(), false)) {
                return Uni.createFrom().failure(new TokenAutoRefreshException(identity));
            }
            return Uni.createFrom().item(identity);
        }

    }

    private static Map<String, Object> codeAccessTokenMap(Map<String, Object> requestData) {
        final String codeAccessToken = (String) requestData.get(OidcConstants.ACCESS_TOKEN_VALUE);
        return Map.of(OidcConstants.ACCESS_TOKEN_VALUE, codeAccessToken);
    }

    private static Map<String, Object> tokenMap(TokenCredential tokenCred) {
        final String tokenType = isIdToken(tokenCred) ? OidcConstants.ID_TOKEN_VALUE : OidcConstants.ACCESS_TOKEN_VALUE;
        return Map.of(tokenType, tokenCred.getToken());
    }

    private static boolean isInternalIdToken(TokenAuthenticationRequest request) {
        return isIdToken(request) && ((IdTokenCredential) request.getToken()).isInternal();
    }

    private static boolean isIdToken(TokenCredential tokenCred) {
        return tokenCred instanceof IdTokenCredential;
    }

    private static boolean isIdToken(TokenAuthenticationRequest request) {
        return isIdToken(request.getToken());
    }

    private static boolean tokenAutoRefreshPrepared(TokenVerificationResult result, Map<String, Object> requestData,
            OidcTenantConfig oidcConfig, boolean codeFlowAccessToken) {
        if (result != null && oidcConfig.token().refreshExpired()
                && oidcConfig.token().refreshTokenTimeSkew().isPresent()
                && requestData.get(REFRESH_TOKEN_GRANT_RESPONSE) != Boolean.TRUE
                && requestData.get(NEW_AUTHENTICATION) != Boolean.TRUE) {
            Long expiry = null;
            if (result.localVerificationResult != null) {
                expiry = result.localVerificationResult.getLong(Claims.exp.name());
            } else if (result.introspectionResult != null) {
                expiry = result.introspectionResult.getLong(OidcConstants.INTROSPECTION_TOKEN_EXP);
            }
            final long now = System.currentTimeMillis() / 1000;
            if (expiry == null && codeFlowAccessToken) {
                // JWT or introspection response `exp` property has a number of seconds since epoch.
                // The code flow access token `expires_in` property is relative to the current time.
                Long expiresIn = ((AuthorizationCodeTokens) requestData.get(AuthorizationCodeTokens.class.getName()))
                        .getAccessTokenExpiresIn();
                if (expiresIn != null) {
                    expiry = now + expiresIn;
                }
            }
            if (expiry != null) {
                final long refreshTokenTimeSkew = oidcConfig.token().refreshTokenTimeSkew().get().getSeconds();
                return now + refreshTokenTimeSkew > expiry;
            }
        }
        return false;
    }

    private static JsonObject getRolesJson(Map<String, Object> requestData, TenantConfigContext resolvedContext,
            TokenCredential tokenCred,
            JsonObject tokenJson, UserInfo userInfo) {
        JsonObject rolesJson = tokenJson;
        if (resolvedContext.oidcConfig().roles().source().isPresent()) {
            if (resolvedContext.oidcConfig().roles().source().get() == Source.userinfo) {
                rolesJson = new JsonObject(userInfo.getJsonObject().toString());
            } else if (tokenCred instanceof IdTokenCredential
                    && resolvedContext.oidcConfig().roles().source().get() == Source.accesstoken) {
                rolesJson = ((TokenVerificationResult) requestData
                        .get(OidcUtils.CODE_ACCESS_TOKEN_RESULT)).localVerificationResult;
                if (rolesJson == null) {
                    // JSON token representation may be null not only if it is an opaque access token
                    // but also if it is JWT and no JWK with a matching kid is available, asynchronous
                    // JWK refresh has not finished yet, but the fallback introspection request has succeeded.
                    rolesJson = OidcCommonUtils.decodeJwtContent((String) requestData.get(OidcConstants.ACCESS_TOKEN_VALUE));
                }
            }
        }
        return rolesJson;
    }

    private Uni<TokenVerificationResult> verifyCodeFlowAccessTokenUni(Map<String, Object> requestData,
            TokenAuthenticationRequest request,
            TenantConfigContext resolvedContext, UserInfo userInfo) {
        if (isIdToken(request)
                && (resolvedContext.oidcConfig().authentication().verifyAccessToken()
                        || resolvedContext.oidcConfig().roles().source().orElse(null) == Source.accesstoken)) {
            String codeAccessToken = (String) requestData.get(OidcConstants.ACCESS_TOKEN_VALUE);
            if (resolvedContext.oidcConfig().token().decryptAccessToken()) {
                codeAccessToken = OidcUtils.decryptToken(resolvedContext, codeAccessToken);
                requestData.put(OidcConstants.ACCESS_TOKEN_VALUE, codeAccessToken);
            }
            return verifyTokenUni(requestData, resolvedContext, new AccessTokenCredential(codeAccessToken), false,
                    false, true, userInfo);
        } else {
            return NULL_CODE_ACCESS_TOKEN_UNI;
        }
    }

    private Uni<TokenVerificationResult> verifyTokenUni(Map<String, Object> requestData, TenantConfigContext resolvedContext,
            TokenCredential tokenCred, boolean idToken, boolean enforceAudienceVerification, boolean codeFlowAccessToken,
            UserInfo userInfo) {
        final String token = tokenCred.getToken();
        Long expiresIn = null;
        if (codeFlowAccessToken) {
            expiresIn = ((AuthorizationCodeTokens) requestData.get(AuthorizationCodeTokens.class.getName()))
                    .getAccessTokenExpiresIn();
        }
        if (OidcUtils.isOpaqueToken(token)) {
            if (!resolvedContext.oidcConfig().token().allowOpaqueTokenIntrospection()) {
                LOG.debug("Token is opaque but the opaque token introspection is not allowed");
                throw new AuthenticationFailedException(tokenMap(tokenCred));
            }
            // verify opaque access token with UserInfo if enabled and introspection URI is absent
            if (resolvedContext.oidcConfig().token().verifyAccessTokenWithUserInfo().orElse(false)
                    && resolvedContext.provider().getMetadata().getIntrospectionUri() == null) {
                if (userInfo == null) {
                    return Uni.createFrom().failure(
                            new AuthenticationFailedException("Opaque access token verification failed as user info is null.",
                                    tokenMap(tokenCred)));
                } else {
                    // valid token verification result
                    return Uni.createFrom().item(new TokenVerificationResult(null, null));
                }
            }
            LOG.debug("Starting the opaque token introspection");
            return introspectTokenUni(resolvedContext, token, idToken, expiresIn, false);
        } else if (resolvedContext.provider().getMetadata().getJsonWebKeySetUri() == null
                || resolvedContext.oidcConfig().token().requireJwtIntrospectionOnly()) {
            // Verify JWT token with the remote introspection
            LOG.debug("Starting the JWT token introspection");
            return introspectTokenUni(resolvedContext, token, idToken, expiresIn, false);
        } else if (resolvedContext.oidcConfig().jwks().resolveEarly()) {
            // Verify JWT token with the local JWK keys with a possible remote introspection fallback
            final String nonce = tokenCred instanceof IdTokenCredential ? (String) requestData.get(OidcConstants.NONCE) : null;
            try {
                LOG.debug("Verifying the JWT token with the local JWK keys");
                return Uni.createFrom()
                        .item(resolvedContext.provider().verifyJwtToken(token, enforceAudienceVerification,
                                resolvedContext.oidcConfig().token().subjectRequired(), nonce));
            } catch (Throwable t) {
                if (t.getCause() instanceof UnresolvableKeyException) {
                    LOG.debug("No matching JWK key is found, refreshing and repeating the token verification");
                    return refreshJwksAndVerifyTokenUni(resolvedContext, token, idToken, enforceAudienceVerification,
                            resolvedContext.oidcConfig().token().subjectRequired(), nonce, expiresIn);
                } else {
                    LOG.debugf("Token verification has failed: %s", t.getMessage());
                    return Uni.createFrom().failure(t);
                }
            }
        } else {
            final String nonce = (String) requestData.get(OidcConstants.NONCE);
            return resolveJwksAndVerifyTokenUni(resolvedContext, tokenCred, enforceAudienceVerification,
                    resolvedContext.oidcConfig().token().subjectRequired(), nonce, expiresIn);
        }
    }

    private Uni<TokenVerificationResult> verifySelfSignedTokenUni(TenantConfigContext resolvedContext, String token) {
        try {
            return Uni.createFrom().item(
                    resolvedContext.provider().verifySelfSignedJwtToken(token, resolvedContext.getInternalIdTokenSigningKey()));
        } catch (Throwable t) {
            return Uni.createFrom().failure(t);
        }
    }

    private Uni<TokenVerificationResult> refreshJwksAndVerifyTokenUni(TenantConfigContext resolvedContext, String token,
            boolean idToken,
            boolean enforceAudienceVerification, boolean subjectRequired, String nonce, Long expiresIn) {
        return resolvedContext.provider()
                .refreshJwksAndVerifyJwtToken(token, enforceAudienceVerification, subjectRequired, nonce)
                .onFailure(f -> fallbackToIntrospectionIfNoMatchingKey(f, resolvedContext))
                .recoverWithUni(f -> introspectTokenUni(resolvedContext, token, idToken, expiresIn, true));
    }

    private Uni<TokenVerificationResult> resolveJwksAndVerifyTokenUni(TenantConfigContext resolvedContext,
            TokenCredential tokenCred,
            boolean enforceAudienceVerification, boolean subjectRequired, String nonce, Long expiresIn) {
        return resolvedContext.provider()
                .getKeyResolverAndVerifyJwtToken(tokenCred, enforceAudienceVerification, subjectRequired, nonce,
                        (tokenCred instanceof IdTokenCredential))
                .onFailure(f -> fallbackToIntrospectionIfNoMatchingKey(f, resolvedContext))
                .recoverWithUni(f -> introspectTokenUni(resolvedContext, tokenCred.getToken(),
                        isIdToken(tokenCred), expiresIn, true));
    }

    private static boolean fallbackToIntrospectionIfNoMatchingKey(Throwable f, TenantConfigContext resolvedContext) {
        if (!(f.getCause() instanceof UnresolvableKeyException)) {
            LOG.debug("Local JWT token verification has failed, skipping the token introspection");
            return false;
        } else if (!resolvedContext.oidcConfig().token().allowJwtIntrospection()) {
            LOG.debug("JWT token does not have a matching verification key but JWT token introspection is disabled");
            return false;
        } else {
            LOG.debug("Local JWT token verification has failed, attempting the token introspection");
            return true;
        }

    }

    private Uni<TokenVerificationResult> introspectTokenUni(TenantConfigContext resolvedContext, final String token,
            boolean idToken, Long expiresIn, boolean fallbackFromJwkMatch) {
        TokenIntrospectionCache tokenIntrospectionCache = tenantResolver.getTokenIntrospectionCache();
        Uni<TokenIntrospection> tokenIntrospectionUni = tokenIntrospectionCache == null ? null
                : tokenIntrospectionCache
                        .getIntrospection(token, resolvedContext.oidcConfig(), getIntrospectionRequestContext);
        if (tokenIntrospectionUni == null) {
            tokenIntrospectionUni = newTokenIntrospectionUni(resolvedContext, token, idToken, expiresIn, fallbackFromJwkMatch);
        } else {
            tokenIntrospectionUni = tokenIntrospectionUni.onItem().ifNull()
                    .switchTo(new Supplier<Uni<? extends TokenIntrospection>>() {
                        @Override
                        public Uni<TokenIntrospection> get() {
                            return newTokenIntrospectionUni(resolvedContext, token, idToken, expiresIn, fallbackFromJwkMatch);
                        }
                    });
        }
        return tokenIntrospectionUni.onItem().transform(t -> new TokenVerificationResult(null, t));
    }

    private Uni<TokenIntrospection> newTokenIntrospectionUni(TenantConfigContext resolvedContext, String token, boolean idToken,
            Long expiresIn, boolean fallbackFromJwkMatch) {
        Uni<TokenIntrospection> tokenIntrospectionUni = resolvedContext.provider().introspectToken(token, idToken, expiresIn,
                fallbackFromJwkMatch);
        if (tenantResolver.getTokenIntrospectionCache() == null
                || !resolvedContext.oidcConfig().allowTokenIntrospectionCache()) {
            return tokenIntrospectionUni;
        } else {
            return tokenIntrospectionUni.call(new Function<TokenIntrospection, Uni<?>>() {

                @Override
                public Uni<?> apply(TokenIntrospection introspection) {
                    return tenantResolver.getTokenIntrospectionCache().addIntrospection(token, introspection,
                            resolvedContext.oidcConfig(), uniVoidOidcContext);
                }
            });
        }
    }

    private static Uni<SecurityIdentity> validateTokenWithoutOidcServer(TokenAuthenticationRequest request,
            TenantConfigContext resolvedContext) {

        try {
            TokenVerificationResult result = resolvedContext.provider().verifyJwtToken(request.getToken().getToken(),
                    resolvedContext.oidcConfig().token().subjectRequired(), false, null);
            StepUpAuthenticationPolicy stepUpAuthPolicy = StepUpAuthenticationPolicy.getFromRequest(request);
            if (stepUpAuthPolicy != null) {
                stepUpAuthPolicy.accept(result);
            }
            return Uni.createFrom()
                    .item(validateAndCreateIdentity(Map.of(), request.getToken(), resolvedContext,
                            result.localVerificationResult, result.localVerificationResult, null, null, request));
        } catch (Throwable t) {
            return Uni.createFrom().failure(t instanceof AuthenticationFailedException ? t
                    : new AuthenticationFailedException(t, tokenMap(request.getToken())));
        }
    }

    private Uni<UserInfo> getUserInfoUni(Map<String, Object> requestData, TokenAuthenticationRequest request,
            TenantConfigContext resolvedContext) {
        if (isInternalIdToken(request) && OidcUtils.cacheUserInfoInIdToken(tenantResolver, resolvedContext.oidcConfig())) {
            JsonObject userInfo = OidcCommonUtils.decodeJwtContent(request.getToken().getToken())
                    .getJsonObject(OidcUtils.USER_INFO_ATTRIBUTE);
            if (userInfo != null) {
                return Uni.createFrom().item(new UserInfo(userInfo.encode()));
            }
        }

        LOG.debug("Requesting UserInfo");
        String contextAccessToken = (String) requestData.get(OidcConstants.ACCESS_TOKEN_VALUE);
        if (contextAccessToken == null && isIdToken(request)) {
            throw new AuthenticationCompletionException(
                    "Authorization code flow access token which is required to get UserInfo is missing");
        }
        final String accessToken = contextAccessToken != null ? contextAccessToken : request.getToken().getToken();

        UserInfoCache userInfoCache = tenantResolver.getUserInfoCache();
        Uni<UserInfo> userInfoUni = userInfoCache == null ? null
                : userInfoCache.getUserInfo(accessToken, resolvedContext.oidcConfig(), getUserInfoRequestContext);
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
        Uni<UserInfo> userInfoUni = resolvedContext.provider().getUserInfo(accessToken);
        if (tenantResolver.getUserInfoCache() == null || !resolvedContext.oidcConfig().allowUserInfoCache()
                || OidcUtils.cacheUserInfoInIdToken(tenantResolver, resolvedContext.oidcConfig())) {
            return userInfoUni;
        } else {
            return userInfoUni.call(new Function<UserInfo, Uni<?>>() {

                @Override
                public Uni<?> apply(UserInfo userInfo) {
                    return tenantResolver.getUserInfoCache().addUserInfo(accessToken, userInfo,
                            resolvedContext.oidcConfig(), uniVoidOidcContext);
                }
            });
        }
    }
}
