package io.quarkus.oidc.runtime;

import static io.quarkus.oidc.common.runtime.OidcConstants.ACR;
import static io.quarkus.oidc.runtime.StepUpAuthenticationPolicy.throwAuthenticationFailedException;
import static java.util.Objects.requireNonNull;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.PrivateKey;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

import org.eclipse.microprofile.jwt.Claims;
import org.jboss.logging.Logger;

import io.quarkus.oidc.AuthorizationCodeTokens;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcConfigurationMetadata;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TokenCustomizer;
import io.quarkus.oidc.TokenIntrospection;
import io.quarkus.oidc.UserInfo;
import io.quarkus.oidc.common.runtime.AbstractJsonObject;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.credential.TokenCredential;
import io.smallrye.jws.JsonWebSignature;
import io.smallrye.jws.JwsHeaders;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.auth.ClaimsValidator;
import io.smallrye.jwt.auth.InvalidJWTException;
import io.smallrye.jwt.auth.JwtVerifier;
import io.smallrye.jwt.auth.TokenExpiredException;
import io.smallrye.jwt.auth.UnresolvableKeyException;
import io.smallrye.jwt.auth.VerificationKeyResolver;
import io.smallrye.jwt.common.JwtClaims;
import io.smallrye.jwt.util.KeyUtils;
import io.smallrye.mutiny.Uni;

public class OidcProvider implements Closeable {

    private static final Logger LOG = Logger.getLogger(OidcProvider.class);
    private static final String ANY_AUDIENCE = "any";
    private static final Set<SignatureAlgorithm> SYMMETRIC_ALLOWED_ALGORITHMS = Set.of(SignatureAlgorithm.HS256);
    static final Set<SignatureAlgorithm> ASYMMETRIC_ALLOWED_ALGORITHMS = Set.of(SignatureAlgorithm.RS256,
            SignatureAlgorithm.RS384,
            SignatureAlgorithm.RS512,
            SignatureAlgorithm.ES256,
            SignatureAlgorithm.ES384,
            SignatureAlgorithm.ES512,
            SignatureAlgorithm.PS256,
            SignatureAlgorithm.PS384,
            SignatureAlgorithm.PS512,
            SignatureAlgorithm.EDDSA);
    static final String ANY_ISSUER = "any";

    private final List<ClaimsValidator> customValidators;
    final OidcProviderClientImpl client;
    final RefreshableVerificationKeyResolver asymmetricKeyResolver;
    final DynamicVerificationKeyResolver keyResolverProvider;
    final OidcTenantConfig oidcConfig;
    final TokenCustomizer tokenCustomizer;
    final String issuer;
    final String[] audience;
    final Map<String, Set<String>> requiredClaims;
    final Set<SignatureAlgorithm> requiredAllowedAlgorithms;

    public OidcProvider(OidcProviderClientImpl client, OidcTenantConfig oidcConfig, JsonWebKeySet jwks) {
        this(client, oidcConfig, jwks, TenantFeatureFinder.find(oidcConfig),
                TenantFeatureFinder.find(oidcConfig, ClaimsValidator.class));
    }

    public OidcProvider(OidcProviderClientImpl client, OidcTenantConfig oidcConfig, JsonWebKeySet jwks,
            TokenCustomizer tokenCustomizer, List<ClaimsValidator> customValidators) {
        this.client = client;
        this.oidcConfig = oidcConfig;
        this.tokenCustomizer = tokenCustomizer;
        if (jwks != null) {
            this.asymmetricKeyResolver = new JsonWebKeyResolver(jwks, oidcConfig.token().forcedJwkRefreshInterval());
        } else if (oidcConfig != null && oidcConfig.certificateChain().trustStoreFile().isPresent()) {
            this.asymmetricKeyResolver = new CertChainPublicKeyResolver(oidcConfig);
        } else {
            this.asymmetricKeyResolver = null;
        }

        if (client != null && oidcConfig != null && !oidcConfig.jwks().resolveEarly()) {
            this.keyResolverProvider = new DynamicVerificationKeyResolver(client, oidcConfig);
        } else {
            this.keyResolverProvider = null;
        }
        this.issuer = checkIssuerProp();
        this.audience = checkAudienceProp();
        this.requiredClaims = checkRequiredClaimsProp();
        this.requiredAllowedAlgorithms = checkSignatureAlgorithm();
        this.customValidators = customValidators == null ? List.of() : customValidators;
        if (client != null) {
            this.client.setOidcProvider(this);
        }
    }

    public OidcProvider(String publicKeyEnc, OidcTenantConfig oidcConfig) {
        this.client = null;
        this.oidcConfig = oidcConfig;
        this.tokenCustomizer = TenantFeatureFinder.find(oidcConfig);
        if (publicKeyEnc != null) {
            this.asymmetricKeyResolver = new LocalPublicKeyResolver(publicKeyEnc);
        } else if (oidcConfig.certificateChain().trustStoreFile().isPresent()) {
            this.asymmetricKeyResolver = new CertChainPublicKeyResolver(oidcConfig);
        } else {
            throw new IllegalStateException("Neither public key nor certificate chain verification modes are enabled");
        }
        this.keyResolverProvider = null;
        this.issuer = checkIssuerProp();
        this.audience = checkAudienceProp();
        this.requiredClaims = checkRequiredClaimsProp();
        this.requiredAllowedAlgorithms = checkSignatureAlgorithm();
        this.customValidators = TenantFeatureFinder.find(oidcConfig, ClaimsValidator.class);
    }

    private Set<SignatureAlgorithm> checkSignatureAlgorithm() {
        if (oidcConfig != null && oidcConfig.token().signatureAlgorithm().isPresent()) {
            String configuredAlg = oidcConfig.token().signatureAlgorithm().get().getAlgorithm();
            return Set.of(SignatureAlgorithm.fromAlgorithm(configuredAlg));
        } else {
            return null;
        }
    }

    private String checkIssuerProp() {
        String issuerProp = null;
        if (oidcConfig != null) {
            issuerProp = oidcConfig.token().issuer().orElse(null);
            if (issuerProp == null && client != null) {
                issuerProp = client.getMetadata().getIssuer();
            }
        }
        return ANY_ISSUER.equals(issuerProp) ? null : issuerProp;
    }

    private String[] checkAudienceProp() {
        List<String> audienceProp = oidcConfig != null ? oidcConfig.token().audience().orElse(null) : null;
        return audienceProp != null ? audienceProp.toArray(new String[] {}) : null;
    }

    private Map<String, Set<String>> checkRequiredClaimsProp() {
        return oidcConfig != null && !oidcConfig.token().requiredClaims().isEmpty() ? oidcConfig.token().requiredClaims()
                : null;
    }

    public TokenVerificationResult verifySelfSignedJwtToken(String token, Key generatedInternalSignatureKey)
            throws InvalidJWTException {
        return verifyJwtTokenInternal(token, true, false, null, SYMMETRIC_ALLOWED_ALGORITHMS,
                new InternalSignatureKeyResolver(generatedInternalSignatureKey),
                true, oidcConfig.token().issuedAtRequired());
    }

    public TokenVerificationResult verifyJwtToken(String token, boolean enforceAudienceVerification, boolean subjectRequired,
            String nonce)
            throws InvalidJWTException {
        return verifyJwtTokenInternal(customizeJwtToken(token), enforceAudienceVerification, subjectRequired, nonce,
                (requiredAllowedAlgorithms != null ? requiredAllowedAlgorithms : ASYMMETRIC_ALLOWED_ALGORITHMS),
                asymmetricKeyResolver, true, oidcConfig.token().issuedAtRequired());
    }

    public TokenVerificationResult verifyJwtToken(String token, boolean enforceAudienceVerification, boolean subjectRequired,
            String nonce, boolean enforceExpReq)
            throws InvalidJWTException {
        return verifyJwtTokenInternal(customizeJwtToken(token), enforceAudienceVerification, subjectRequired, nonce,
                (requiredAllowedAlgorithms != null ? requiredAllowedAlgorithms : ASYMMETRIC_ALLOWED_ALGORITHMS),
                asymmetricKeyResolver, enforceExpReq, oidcConfig.token().issuedAtRequired());
    }

    public TokenVerificationResult verifyLogoutJwtToken(String token) throws InvalidJWTException {
        final boolean enforceExpReq = !oidcConfig.token().age().isPresent();
        TokenVerificationResult result = verifyJwtTokenInternal(token, true, false, null, ASYMMETRIC_ALLOWED_ALGORITHMS,
                asymmetricKeyResolver, enforceExpReq, oidcConfig.token().issuedAtRequired());
        if (!enforceExpReq) {
            // Expiry check was skipped during the initial verification but if the logout token contains the exp claim
            // then it must be verified
            final Long exp = result.localVerificationResult().getLong(Claims.exp.name());
            if (exp != null) {
                final long secondsAfterExpiry = now() / 1000 - (exp + getLifespanGrace());
                if (secondsAfterExpiry > 0) {
                    String error = "Logout token issued to client %s expired %d seconds ago".formatted(
                            oidcConfig.clientId().get(),
                            secondsAfterExpiry);
                    LOG.warn(error);
                    throw new TokenExpiredException(error);
                }
            }
        }
        return result;
    }

    private TokenVerificationResult verifyJwtTokenInternal(String token,
            boolean enforceAudienceVerification,
            boolean subjectRequired,
            String nonce,
            Set<SignatureAlgorithm> allowedAlgorithms,
            VerificationKeyResolver verificationKeyResolver, boolean enforceExpReq, boolean issuedAtRequired)
            throws InvalidJWTException {

        JwtVerifier.Builder builder = JwtVerifier.builder()
                .verificationKeyResolver(verificationKeyResolver)
                .allowedAlgorithms(allowedAlgorithms)
                .requireExpirationTime(enforceExpReq)
                .requireIssuedAt(issuedAtRequired)
                .requireSubject(subjectRequired)
                .clockSkewSeconds(getLifespanGrace())
                .relaxKeyValidation();

        if (issuer != null) {
            builder.expectedIssuer(issuer);
        }

        if (audience != null) {
            if (!(audience.length == 1 && audience[0].equals(ANY_AUDIENCE))) {
                builder.expectedAudience(audience);
            }
        } else if (enforceAudienceVerification) {
            builder.expectedAudience(oidcConfig.clientId().get());
        }

        if (nonce != null) {
            builder.claimsValidator(new CustomClaimsValidator(Map.of(OidcConstants.NONCE, Set.of(nonce))));
        }

        final List<CatchingClaimsValidator> validators;
        if (!customValidators.isEmpty() || requiredClaims != null) {
            validators = new ArrayList<>();
            for (ClaimsValidator customValidator : customValidators) {
                validators.add(new CatchingClaimsValidator(customValidator));
            }
            if (requiredClaims != null) {
                validators.add(new CatchingClaimsValidator(new CustomClaimsValidator(requiredClaims)));
            }
            builder.claimsValidators(new ArrayList<>(validators));
        } else {
            validators = null;
        }

        try {
            builder.build().verify(token);
        } catch (UnresolvableKeyException ex) {
            logVerificationFailure(ex.getMessage());
            throw new InvalidJWTException(ex.getMessage(), ex);
        } catch (TokenExpiredException ex) {
            logVerificationFailure(ex.getMessage());
            throw ex;
        } catch (InvalidJWTException ex) {
            logVerificationFailure(ex.getMessage());
            throw ex;
        }

        if (validators != null) {
            // this is workaround for we want to give custom validators option to fail authentication over 'acr' values
            for (CatchingClaimsValidator validator : validators) {
                if (validator.authenticationFailure != null) {
                    throw validator.authenticationFailure;
                }
            }
        }

        TokenVerificationResult result = new TokenVerificationResult(OidcCommonUtils.decodeJwtContent(token), null);

        verifyTokenAge(result.localVerificationResult().getLong(Claims.iat.name()));
        return result;
    }

    private void logVerificationFailure(String detail) {
        if (oidcConfig.clientId().isPresent()) {
            LOG.warnf("Verification of the token issued to client %s has failed: %s.", oidcConfig.clientId().get(),
                    detail);
            if (oidcConfig.clientName().isPresent()) {
                LOG.warnf(" Client name: %s", oidcConfig.clientName().get());
            }
        } else {
            LOG.warnf("Token verification has failed: %s", detail);
        }
    }

    private String customizeJwtToken(String token) {
        if (tokenCustomizer != null) {
            JsonObject headers = AbstractJsonObject.toJsonObject(
                    OidcUtils.decodeJwtHeadersAsString(token));
            headers = tokenCustomizer.customizeHeaders(headers);
            if (headers != null) {
                String newHeaders = new String(
                        Base64.getUrlEncoder().withoutPadding().encode(headers.toString().getBytes()),
                        StandardCharsets.UTF_8);
                int dotIndex = token.indexOf('.');
                String newToken = newHeaders + token.substring(dotIndex);
                return newToken;
            }
        }
        return token;
    }

    private void verifyTokenAge(Long iat) throws TokenExpiredException {
        if (oidcConfig.token().age().isPresent() && iat != null) {
            final long nowSecs = now() / 1000;

            if (nowSecs - iat > oidcConfig.token().age().get().toSeconds() + getLifespanGrace()) {
                final String errorMessage = "Token age exceeds the configured token age property";
                LOG.warn(errorMessage);
                throw new TokenExpiredException(errorMessage);
            }
        }
    }

    public Uni<TokenVerificationResult> refreshJwksAndVerifyJwtToken(String token, boolean enforceAudienceVerification,
            boolean subjectRequired, String nonce) {
        return asymmetricKeyResolver.refresh().onItem()
                .transformToUni(new Function<Void, Uni<? extends TokenVerificationResult>>() {

                    @Override
                    public Uni<? extends TokenVerificationResult> apply(Void v) {
                        try {
                            return Uni.createFrom()
                                    .item(verifyJwtToken(token, enforceAudienceVerification, subjectRequired, nonce));
                        } catch (Throwable t) {
                            return Uni.createFrom().failure(t);
                        }
                    }

                });
    }

    public Uni<TokenVerificationResult> getKeyResolverAndVerifyJwtToken(TokenCredential tokenCred,
            boolean enforceAudienceVerification,
            boolean subjectRequired, String nonce, boolean issuedAtRequired) {
        return keyResolverProvider.resolve(tokenCred).onItem()
                .transformToUni(new Function<VerificationKeyResolver, Uni<? extends TokenVerificationResult>>() {

                    @Override
                    public Uni<? extends TokenVerificationResult> apply(VerificationKeyResolver resolver) {
                        try {
                            return Uni.createFrom()
                                    .item(verifyJwtTokenInternal(customizeJwtToken(tokenCred.getToken()),
                                            enforceAudienceVerification,
                                            subjectRequired, nonce,
                                            (requiredAllowedAlgorithms != null ? requiredAllowedAlgorithms
                                                    : ASYMMETRIC_ALLOWED_ALGORITHMS),
                                            resolver, true, issuedAtRequired));
                        } catch (Throwable t) {
                            return Uni.createFrom().failure(t);
                        }
                    }

                });
    }

    public Uni<TokenIntrospection> introspectToken(String token, TokenType tokenType, Long expiresIn,
            boolean fallbackFromJwkMatch) {
        if (client.getMetadata().getIntrospectionUri() == null) {
            String errorMessage = String.format("Token issued to client %s "
                    + (fallbackFromJwkMatch ? "does not have a matching verification key and it " : "")
                    + "can not be introspected because the introspection endpoint address is unknown - "
                    + "please check if your OpenId Connect Provider supports the token introspection",
                    oidcConfig.clientId().get());

            throw new AuthenticationFailedException(errorMessage, tokenMap(token, tokenType));
        }
        return client.introspectAccessToken(token).onItemOrFailure()
                .transform(new BiFunction<TokenIntrospection, Throwable, TokenIntrospection>() {

                    @Override
                    public TokenIntrospection apply(TokenIntrospection introspectionResult, Throwable t) {
                        if (t != null) {
                            throw new AuthenticationFailedException(t, tokenMap(token, tokenType));
                        }
                        Long introspectionExpiresIn = introspectionResult.getLong(OidcConstants.INTROSPECTION_TOKEN_EXP);
                        if (introspectionExpiresIn == null && expiresIn != null) {
                            // expires_in is relative to the current time
                            introspectionExpiresIn = now() + expiresIn;
                        }
                        if (!introspectionResult.isActive()) {
                            verifyTokenExpiry(token, tokenType, introspectionExpiresIn);
                            throw new AuthenticationFailedException(
                                    String.format("Token issued to client %s is not active", oidcConfig.clientId().get()),
                                    tokenMap(token, tokenType));
                        }
                        verifyTokenExpiry(token, tokenType, introspectionExpiresIn);
                        try {
                            verifyTokenAge(introspectionResult.getLong(OidcConstants.INTROSPECTION_TOKEN_IAT));
                        } catch (TokenExpiredException ex) {
                            throw new AuthenticationFailedException(ex, tokenMap(token, tokenType));
                        }

                        if (requiredClaims != null) {
                            for (Map.Entry<String, Set<String>> requiredClaim : requiredClaims.entrySet()) {
                                final String requiredClaimName = requiredClaim.getKey();
                                if (!introspectionResult.contains(requiredClaimName)) {
                                    LOG.debugf("Introspection claim %s is missing", requiredClaimName);
                                    throw new AuthenticationFailedException(tokenMap(token, tokenType));
                                }
                                final Set<String> requiredClaimValues = requiredClaim.getValue();
                                if (requiredClaimValues.size() == 1) {
                                    String introspectionClaimValue = null;
                                    try {
                                        introspectionClaimValue = introspectionResult.getString(requiredClaimName);
                                    } catch (ClassCastException ex) {
                                        LOG.debugf("Introspection claim %s is not String", requiredClaimName);
                                    }
                                    String requiredClaimValue = requiredClaimValues.iterator().next();
                                    if (requiredClaimValue.equals(introspectionClaimValue)) {
                                        continue;
                                    }
                                }
                                final JsonArray actualClaimValueArray;
                                try {
                                    actualClaimValueArray = requireNonNull(introspectionResult.getArray(requiredClaimName));
                                } catch (Exception ignored) {
                                    LOG.debugf("Introspection claim %s is neither string nor array", requiredClaimName);
                                    throw new AuthenticationFailedException(tokenMap(token, tokenType));
                                }
                                requiredClaimValuesLoop: for (String requiredClaimValue : requiredClaimValues) {
                                    for (int i = 0; i < actualClaimValueArray.size(); i++) {
                                        try {
                                            String actualClaimValue = actualClaimValueArray.getString(i);
                                            if (requiredClaimValue.equals(actualClaimValue)) {
                                                continue requiredClaimValuesLoop;
                                            }
                                        } catch (Exception ignored) {
                                            // try next actual claim value
                                        }
                                    }
                                    LOG.debugf("Value of the introspection claim %s does not match required value of %s",
                                            requiredClaimName, requiredClaimValue);
                                    throw new AuthenticationFailedException(tokenMap(token, tokenType));
                                }
                            }
                        }

                        return introspectionResult;
                    }

                });
    }

    private void verifyTokenExpiry(String token, TokenType tokenType, Long exp) {
        if (exp == null) {
            return;
        }
        final long secondsAfterExpiry = now() / 1000 - (exp + getLifespanGrace());
        if (secondsAfterExpiry > 0) {
            String error = "Token issued to client %s expired %d seconds ago".formatted(oidcConfig.clientId().get(),
                    secondsAfterExpiry);
            if (tokenType == TokenType.BEARER_ACCESS_TOKEN) {
                LOG.warn(error);
            } else {
                LOG.debug(error);
            }
            throw new AuthenticationFailedException(
                    new TokenExpiredException(error),
                    tokenMap(token, tokenType));
        }
    }

    private int getLifespanGrace() {
        return oidcConfig.token().lifespanGrace().isPresent()
                ? oidcConfig.token().lifespanGrace().getAsInt()
                : 0;
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    public Uni<UserInfo> getUserInfo(String accessToken) {
        return client.getUserInfo(accessToken);
    }

    public Uni<AuthorizationCodeTokens> getCodeFlowTokens(String code, String redirectUri, String codeVerifier) {
        return client.getAuthorizationCodeTokens(code, redirectUri, codeVerifier);
    }

    public Uni<AuthorizationCodeTokens> refreshTokens(String refreshToken) {
        return client.refreshAuthorizationCodeTokens(refreshToken);
    }

    @Override
    public void close() {
        if (client != null) {
            client.close();
        }
    }

    private class JsonWebKeyResolver implements RefreshableVerificationKeyResolver {
        volatile JsonWebKeySet jwks;
        volatile long lastForcedRefreshTime;
        volatile long forcedJwksRefreshIntervalMilliSecs;
        final CertChainPublicKeyResolver chainResolverFallback;

        JsonWebKeyResolver(JsonWebKeySet jwks, Duration forcedJwksRefreshInterval) {
            this.jwks = jwks;
            this.forcedJwksRefreshIntervalMilliSecs = forcedJwksRefreshInterval.toMillis();
            if (oidcConfig.certificateChain().trustStoreFile().isPresent()) {
                chainResolverFallback = new CertChainPublicKeyResolver(oidcConfig);
            } else {
                chainResolverFallback = null;
            }
        }

        @Override
        public Key resolveKey(JsonWebSignature jws) throws UnresolvableKeyException {
            Key key = null;

            // Try 'kid' first
            JwsHeaders header = jws.headers();
            String kid = header.keyId();
            if (kid != null) {
                key = getKeyWithId(kid);
                if (key == null) {
                    // if `kid` was set then the key must exist
                    throw new UnresolvableKeyException(String.format("JWK with kid '%s' is not available", kid));
                }
            }

            String thumbprint = null;
            if (key == null) {
                thumbprint = header.x509CertificateSha256Thumbprint();
                if (thumbprint != null) {
                    key = getKeyWithS256Thumbprint(thumbprint);
                    if (key == null) {
                        // if only `x5tS256` was set then the key must exist
                        throw new UnresolvableKeyException(
                                String.format("JWK with the SHA256 certificate thumbprint '%s' is not available", thumbprint));
                    }
                }
            }

            if (key == null) {
                thumbprint = header.x509CertificateThumbprint();
                if (thumbprint != null) {
                    key = getKeyWithThumbprint(thumbprint);
                    if (key == null) {
                        // if only `x5t` was set then the key must exist
                        throw new UnresolvableKeyException(
                                String.format("JWK with the certificate thumbprint '%s' is not available", thumbprint));
                    }
                }
            }

            if (key == null && kid == null && thumbprint == null) {
                try {
                    String keyType = JsonWebKeySet.getKeyTypeFromAlgorithm(header.algorithm());
                    key = jwks.getKeyWithoutKeyIdAndThumbprint(keyType);
                } catch (Exception ex) {
                    LOG.debug("Token 'alg'(algorithm) header value is invalid", ex);
                }
            }

            if (key == null && oidcConfig.jwks().tryAll() && kid == null && thumbprint == null) {
                LOG.debug("JWK is not available, neither 'kid' nor 'x5t#S256' nor 'x5t' token headers are set,"
                        + " falling back to trying all available keys");
                key = jwks.findKeyInAllKeys(jws, ASYMMETRIC_ALLOWED_ALGORITHMS);
            }

            if (key == null && chainResolverFallback != null) {
                LOG.debug("JWK is not available, neither 'kid' nor 'x5t#S256' nor 'x5t' token headers are set,"
                        + " falling back to the certificate chain resolver");
                key = chainResolverFallback.resolveKey(jws);
            }

            if (key == null) {
                throw new UnresolvableKeyException(
                        "JWK is not available, neither 'kid' nor 'x5t#S256' nor 'x5t' token headers are set");
            } else {
                return key;
            }
        }

        private Key getKeyWithId(String kid) {
            if (kid != null) {
                return jwks.getKeyWithId(kid);
            } else {
                LOG.debug("Token 'kid' header is not set");
                return null;
            }
        }

        private Key getKeyWithThumbprint(String thumbprint) {
            if (thumbprint != null) {
                return jwks.getKeyWithThumbprint(thumbprint);
            } else {
                LOG.debug("Token 'x5t' header is not set");
                return null;
            }
        }

        private Key getKeyWithS256Thumbprint(String thumbprint) {
            if (thumbprint != null) {
                return jwks.getKeyWithS256Thumbprint(thumbprint);
            } else {
                LOG.debug("Token 'x5tS256' header is not set");
                return null;
            }
        }

        public Uni<Void> refresh() {
            final long now = now();
            if (now > lastForcedRefreshTime + forcedJwksRefreshIntervalMilliSecs) {
                lastForcedRefreshTime = now;
                return client.getJsonWebKeySet(null).onItem()
                        .transformToUni(new Function<JsonWebKeySet, Uni<? extends Void>>() {

                            @Override
                            public Uni<? extends Void> apply(JsonWebKeySet t) {
                                jwks = t;
                                return Uni.createFrom().voidItem();
                            }

                        });
            } else {
                return Uni.createFrom().voidItem();
            }
        }

    }

    private static class LocalPublicKeyResolver implements RefreshableVerificationKeyResolver {
        Key key;

        LocalPublicKeyResolver(String publicKeyEnc) {
            try {
                key = KeyUtils.decodePublicKey(publicKeyEnc);
            } catch (Exception ex) {
                throw new OIDCException(ex);
            }
        }

        @Override
        public Key resolveKey(JsonWebSignature jws) throws UnresolvableKeyException {
            return key;
        }

    }

    private class InternalSignatureKeyResolver implements VerificationKeyResolver {
        final Key internalSignatureKey;

        public InternalSignatureKeyResolver(Key generatedInternalSignatureKey) {
            this.internalSignatureKey = initKey(generatedInternalSignatureKey);
        }

        @Override
        public Key resolveKey(JsonWebSignature jws) throws UnresolvableKeyException {
            return internalSignatureKey;
        }

        private Key initKey(Key generatedInternalSignatureKey) {
            String clientSecret = client.getClientSecret();
            if (clientSecret != null) {
                LOG.debug("Verifying internal ID token with a configured client secret");
                return KeyUtils.createSecretKeyFromSecret(clientSecret);
            } else if (client.getClientJwtKey() instanceof PrivateKey) {
                LOG.debug("Verifying internal ID token with a configured JWT private key");
                return OidcUtils.createSecretKeyFromDigest(((PrivateKey) client.getClientJwtKey()).getEncoded());
            } else {
                LOG.debug("Verifying internal ID token with a generated secret key");
                return generatedInternalSignatureKey;
            }
        }
    }

    public OidcConfigurationMetadata getMetadata() {
        return client == null ? null : client.getMetadata();
    }

    private static final class CustomClaimsValidator implements ClaimsValidator {

        private final Map<String, Set<String>> customClaims;

        private CustomClaimsValidator(Map<String, Set<String>> customClaims) {
            this.customClaims = customClaims;
        }

        @Override
        public String validate(VerificationContext context) {
            JwtClaims claims = context.claims();
            for (var requiredClaim : customClaims.entrySet()) {
                String validationFailureMessage = validateClaim(requiredClaim.getKey(), requiredClaim.getValue(), claims);
                if (validationFailureMessage != null) {
                    if (ACR.equals(requiredClaim.getKey())) {
                        throwAuthenticationFailedException(validationFailureMessage, requiredClaim.getValue());
                    }
                    return validationFailureMessage;
                }
            }
            return null;
        }

        private static String validateClaim(String requiredClaimName, Set<String> requiredClaimValues, JwtClaims claims) {
            if (!claims.hasClaim(requiredClaimName)) {
                return "claim " + requiredClaimName + " is missing";
            }
            Object claimValue = claims.getClaim(requiredClaimName);
            if (claimValue instanceof String) {
                if (requiredClaimValues.size() == 1) {
                    String actualClaimValue = (String) claimValue;
                    String requiredClaimValue = requiredClaimValues.iterator().next();
                    if (!requiredClaimValue.equals(actualClaimValue)) {
                        return "claim " + requiredClaimName + " does not match expected value of " + requiredClaimValues;
                    }
                } else {
                    return "expected claim " + requiredClaimName + " must be a list of strings";
                }
            } else if (claimValue instanceof List<?> actualClaimValues
                    && actualClaimValues.stream().allMatch(String.class::isInstance)) {
                for (String requiredClaimValue : requiredClaimValues) {
                    if (!actualClaimValues.contains(requiredClaimValue)) {
                        return "claim " + requiredClaimName + " does not match expected value of " + requiredClaimValues;
                    }
                }
            } else {
                return "expected claim " + requiredClaimName + " must be a list of strings or a string";
            }
            return null;
        }
    }

    private static Map<String, Object> tokenMap(String token, TokenType tokenType) {
        return Map.of(tokenType == TokenType.ID_TOKEN ? OidcConstants.ID_TOKEN_VALUE : OidcConstants.ACCESS_TOKEN_VALUE,
                token);
    }

    private static final class CatchingClaimsValidator implements ClaimsValidator {

        private AuthenticationFailedException authenticationFailure;
        private final ClaimsValidator validator;

        private CatchingClaimsValidator(ClaimsValidator validator) {
            this.validator = validator;
        }

        @Override
        public String validate(VerificationContext context) {
            try {
                return validator.validate(context);
            } catch (AuthenticationFailedException e) {
                if (e.getAttribute(OidcConstants.ACR_VALUES) != null) {
                    authenticationFailure = e;
                    return null;
                } else {
                    throw e;
                }
            }
        }
    }

    public static boolean isTokenExpired(Throwable t) {
        if (t == null) {
            return false;
        }
        return t instanceof TokenExpiredException || t.getCause() instanceof TokenExpiredException;
    }
}
