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
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.ErrorCodeValidator;
import org.jose4j.jwt.consumer.ErrorCodeValidatorAdapter;
import org.jose4j.jwt.consumer.ErrorCodes;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwt.consumer.Validator;
import org.jose4j.jwx.HeaderParameterNames;
import org.jose4j.jwx.JsonWebStructure;
import org.jose4j.keys.resolvers.VerificationKeyResolver;
import org.jose4j.lang.InvalidAlgorithmException;
import org.jose4j.lang.UnresolvableKeyException;

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
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.util.KeyUtils;
import io.smallrye.mutiny.Uni;

public class OidcProvider implements Closeable {

    private static final Logger LOG = Logger.getLogger(OidcProvider.class);
    private static final String ANY_AUDIENCE = "any";
    private static final String[] ASYMMETRIC_SUPPORTED_ALGORITHMS = new String[] { SignatureAlgorithm.RS256.getAlgorithm(),
            SignatureAlgorithm.RS384.getAlgorithm(),
            SignatureAlgorithm.RS512.getAlgorithm(),
            SignatureAlgorithm.ES256.getAlgorithm(),
            SignatureAlgorithm.ES384.getAlgorithm(),
            SignatureAlgorithm.ES512.getAlgorithm(),
            SignatureAlgorithm.PS256.getAlgorithm(),
            SignatureAlgorithm.PS384.getAlgorithm(),
            SignatureAlgorithm.PS512.getAlgorithm(),
            SignatureAlgorithm.EDDSA.getAlgorithm() };
    private static final AlgorithmConstraints SYMMETRIC_ALGORITHM_CONSTRAINTS = new AlgorithmConstraints(
            AlgorithmConstraints.ConstraintType.PERMIT, SignatureAlgorithm.HS256.getAlgorithm());
    static final AlgorithmConstraints ASYMMETRIC_ALGORITHM_CONSTRAINTS = new AlgorithmConstraints(
            AlgorithmConstraints.ConstraintType.PERMIT, ASYMMETRIC_SUPPORTED_ALGORITHMS);
    static final String ANY_ISSUER = "any";

    private final List<Validator> customValidators;
    final OidcProviderClientImpl client;
    final RefreshableVerificationKeyResolver asymmetricKeyResolver;
    final DynamicVerificationKeyResolver keyResolverProvider;
    final OidcTenantConfig oidcConfig;
    final TokenCustomizer tokenCustomizer;
    final String issuer;
    final String[] audience;
    final Map<String, Set<String>> requiredClaims;
    final AlgorithmConstraints requiredAlgorithmConstraints;

    public OidcProvider(OidcProviderClientImpl client, OidcTenantConfig oidcConfig, JsonWebKeySet jwks) {
        this(client, oidcConfig, jwks, TenantFeatureFinder.find(oidcConfig),
                TenantFeatureFinder.find(oidcConfig, Validator.class));
    }

    public OidcProvider(OidcProviderClientImpl client, OidcTenantConfig oidcConfig, JsonWebKeySet jwks,
            TokenCustomizer tokenCustomizer, List<Validator> customValidators) {
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
        this.requiredAlgorithmConstraints = checkSignatureAlgorithm();
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
        this.requiredAlgorithmConstraints = checkSignatureAlgorithm();
        this.customValidators = TenantFeatureFinder.find(oidcConfig, Validator.class);
    }

    private AlgorithmConstraints checkSignatureAlgorithm() {
        if (oidcConfig != null && oidcConfig.token().signatureAlgorithm().isPresent()) {
            String configuredAlg = oidcConfig.token().signatureAlgorithm().get().getAlgorithm();
            return new AlgorithmConstraints(AlgorithmConstraints.ConstraintType.PERMIT, configuredAlg);
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
            throws InvalidJwtException {
        return verifyJwtTokenInternal(token, true, false, null, SYMMETRIC_ALGORITHM_CONSTRAINTS,
                new InternalSignatureKeyResolver(generatedInternalSignatureKey),
                true, oidcConfig.token().issuedAtRequired());
    }

    public TokenVerificationResult verifyJwtToken(String token, boolean enforceAudienceVerification, boolean subjectRequired,
            String nonce)
            throws InvalidJwtException {
        return verifyJwtTokenInternal(customizeJwtToken(token), enforceAudienceVerification, subjectRequired, nonce,
                (requiredAlgorithmConstraints != null ? requiredAlgorithmConstraints : ASYMMETRIC_ALGORITHM_CONSTRAINTS),
                asymmetricKeyResolver, true, oidcConfig.token().issuedAtRequired());
    }

    public TokenVerificationResult verifyLogoutJwtToken(String token) throws InvalidJwtException {
        final boolean enforceExpReq = !oidcConfig.token().age().isPresent();
        TokenVerificationResult result = verifyJwtTokenInternal(token, true, false, null, ASYMMETRIC_ALGORITHM_CONSTRAINTS,
                asymmetricKeyResolver, enforceExpReq, oidcConfig.token().issuedAtRequired());
        if (!enforceExpReq) {
            // Expiry check was skipped during the initial verification but if the logout token contains the exp claim
            // then it must be verified
            if (isTokenExpired(result.localVerificationResult.getLong(Claims.exp.name()))) {
                String error = String.format("Logout token for client %s has expired", oidcConfig.clientId().get());
                LOG.debugf(error);
                throw new InvalidJwtException(error, List.of(new ErrorCodeValidator.Error(ErrorCodes.EXPIRED, error)), null);
            }
        }
        return result;
    }

    private TokenVerificationResult verifyJwtTokenInternal(String token,
            boolean enforceAudienceVerification,
            boolean subjectRequired,
            String nonce,
            AlgorithmConstraints algConstraints,
            VerificationKeyResolver verificationKeyResolver, boolean enforceExpReq, boolean issuedAtRequired)
            throws InvalidJwtException {
        JwtConsumerBuilder builder = new JwtConsumerBuilder();

        builder.setVerificationKeyResolver(verificationKeyResolver);

        builder.setJwsAlgorithmConstraints(algConstraints);

        if (enforceExpReq) {
            builder.setRequireExpirationTime();
        }
        if (subjectRequired) {
            builder.setRequireSubject();
        }

        if (nonce != null) {
            builder.registerValidator(new CustomClaimsValidator(Map.of(OidcConstants.NONCE, Set.of(nonce))));
        }

        final List<CatchingErrorCodeValidator> validators;
        if (!customValidators.isEmpty() || requiredClaims != null) {
            validators = new ArrayList<>();
            for (Validator customValidator : customValidators) {
                validators.add(new CatchingErrorCodeValidator(customValidator));
            }
            if (requiredClaims != null) {
                validators.add(new CatchingErrorCodeValidator(new CustomClaimsValidator(requiredClaims)));
            }
            for (var validator : validators) {
                builder.registerValidator(validator);
            }
        } else {
            validators = null;
        }

        if (issuedAtRequired) {
            builder.setRequireIssuedAt();
        }

        if (issuer != null) {
            builder.setExpectedIssuer(issuer);
        }
        if (audience != null) {
            if (audience.length == 1 && audience[0].equals(ANY_AUDIENCE)) {
                builder.setSkipDefaultAudienceValidation();
            } else {
                builder.setExpectedAudience(audience);
            }
        } else if (enforceAudienceVerification) {
            builder.setExpectedAudience(oidcConfig.clientId().get());
        } else {
            builder.setSkipDefaultAudienceValidation();
        }

        if (oidcConfig.token().lifespanGrace().isPresent()) {
            final int lifespanGrace = oidcConfig.token().lifespanGrace().getAsInt();
            builder.setAllowedClockSkewInSeconds(lifespanGrace);
        }

        builder.setRelaxVerificationKeyValidation();

        try {
            JwtConsumer jwtConsumer = builder.build();
            jwtConsumer.processToClaims(token);
        } catch (InvalidJwtException ex) {
            String detail = "";
            List<ErrorCodeValidator.Error> details = ex.getErrorDetails();
            if (!details.isEmpty()) {
                detail = details.get(0).getErrorMessage();
            }
            if (oidcConfig.clientId().isPresent()) {
                LOG.debugf("Verification of the token issued to client %s has failed: %s.", oidcConfig.clientId().get(),
                        detail);
                if (oidcConfig.clientName().isPresent()) {
                    LOG.debugf(" Client name: %s", oidcConfig.clientName().get());
                }
            } else {
                LOG.debugf("Token verification has failed: %s", detail);
            }
            throw ex;
        }
        if (validators != null) {
            // this is workaround for we want to give custom validators option to fail authentication over 'acr' values
            for (CatchingErrorCodeValidator validator : validators) {
                if (validator.authenticationFailure != null) {
                    throw validator.authenticationFailure;
                }
            }
        }
        TokenVerificationResult result = new TokenVerificationResult(OidcCommonUtils.decodeJwtContent(token), null);

        verifyTokenAge(result.localVerificationResult.getLong(Claims.iat.name()));
        return result;
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

    private void verifyTokenAge(Long iat) throws InvalidJwtException {
        if (oidcConfig.token().age().isPresent() && iat != null) {
            final long now = now() / 1000;

            if (now - iat > oidcConfig.token().age().get().toSeconds() + getLifespanGrace()) {
                final String errorMessage = "Token age exceeds the configured token age property";
                LOG.debugf(errorMessage);
                throw new InvalidJwtException(errorMessage,
                        List.of(new ErrorCodeValidator.Error(ErrorCodes.ISSUED_AT_INVALID_PAST, errorMessage)), null);
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
                                            (requiredAlgorithmConstraints != null ? requiredAlgorithmConstraints
                                                    : ASYMMETRIC_ALGORITHM_CONSTRAINTS),
                                            resolver, true, issuedAtRequired));
                        } catch (Throwable t) {
                            return Uni.createFrom().failure(t);
                        }
                    }

                });
    }

    public Uni<TokenIntrospection> introspectToken(String token, boolean idToken, Long expiresIn,
            boolean fallbackFromJwkMatch) {
        if (client.getMetadata().getIntrospectionUri() == null) {
            String errorMessage = String.format("Token issued to client %s "
                    + (fallbackFromJwkMatch ? "does not have a matching verification key and it " : "")
                    + "can not be introspected because the introspection endpoint address is unknown - "
                    + "please check if your OpenId Connect Provider supports the token introspection",
                    oidcConfig.clientId().get());

            throw new AuthenticationFailedException(errorMessage, tokenMap(token, idToken));
        }
        return client.introspectAccessToken(token).onItemOrFailure()
                .transform(new BiFunction<TokenIntrospection, Throwable, TokenIntrospection>() {

                    @Override
                    public TokenIntrospection apply(TokenIntrospection introspectionResult, Throwable t) {
                        if (t != null) {
                            throw new AuthenticationFailedException(t, tokenMap(token, idToken));
                        }
                        Long introspectionExpiresIn = introspectionResult.getLong(OidcConstants.INTROSPECTION_TOKEN_EXP);
                        if (introspectionExpiresIn == null && expiresIn != null) {
                            // expires_in is relative to the current time
                            introspectionExpiresIn = now() + expiresIn;
                        }
                        if (!introspectionResult.isActive()) {
                            verifyTokenExpiry(token, idToken, introspectionExpiresIn);
                            throw new AuthenticationFailedException(
                                    String.format("Token issued to client %s is not active", oidcConfig.clientId().get()),
                                    tokenMap(token, idToken));
                        }
                        verifyTokenExpiry(token, idToken, introspectionExpiresIn);
                        try {
                            verifyTokenAge(introspectionResult.getLong(OidcConstants.INTROSPECTION_TOKEN_IAT));
                        } catch (InvalidJwtException ex) {
                            throw new AuthenticationFailedException(ex, tokenMap(token, idToken));
                        }

                        if (requiredClaims != null) {
                            for (Map.Entry<String, Set<String>> requiredClaim : requiredClaims.entrySet()) {
                                final String requiredClaimName = requiredClaim.getKey();
                                if (!introspectionResult.contains(requiredClaimName)) {
                                    LOG.debugf("Introspection claim %s is missing", requiredClaimName);
                                    throw new AuthenticationFailedException(tokenMap(token, idToken));
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
                                    LOG.debugf("Introspection claim %s is neither string or array", requiredClaimName);
                                    throw new AuthenticationFailedException(tokenMap(token, idToken));
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
                                    throw new AuthenticationFailedException(tokenMap(token, idToken));
                                }
                            }
                        }

                        return introspectionResult;
                    }

                });
    }

    private void verifyTokenExpiry(String token, boolean idToken, Long exp) {
        if (isTokenExpired(exp)) {
            String error = String.format("Token issued to client %s has expired", oidcConfig.clientId().get());
            LOG.debugf(error);
            throw new AuthenticationFailedException(
                    new InvalidJwtException(error,
                            List.of(new ErrorCodeValidator.Error(ErrorCodes.EXPIRED, error)), null),
                    tokenMap(token, idToken));
        }
    }

    private boolean isTokenExpired(Long exp) {
        return exp != null && now() / 1000 > exp + getLifespanGrace();
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
        public Key resolveKey(JsonWebSignature jws, List<JsonWebStructure> nestingContext)
                throws UnresolvableKeyException {
            Key key = null;

            // Try 'kid' first
            String kid = jws.getKeyIdHeaderValue();
            if (kid != null) {
                key = getKeyWithId(kid);
                if (key == null) {
                    // if `kid` was set then the key must exist
                    throw new UnresolvableKeyException(String.format("JWK with kid '%s' is not available", kid));
                }
            }

            String thumbprint = null;
            if (key == null) {
                thumbprint = jws.getHeader(HeaderParameterNames.X509_CERTIFICATE_SHA256_THUMBPRINT);
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
                thumbprint = jws.getHeader(HeaderParameterNames.X509_CERTIFICATE_THUMBPRINT);
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
                    key = jwks.getKeyWithoutKeyIdAndThumbprint(jws.getKeyType());
                } catch (InvalidAlgorithmException ex) {
                    LOG.debug("Token 'alg'(algorithm) header value is invalid", ex);
                }
            }

            if (key == null && oidcConfig.jwks().tryAll() && kid == null && thumbprint == null) {
                LOG.debug("JWK is not available, neither 'kid' nor 'x5t#S256' nor 'x5t' token headers are set,"
                        + " falling back to trying all available keys");
                key = jwks.findKeyInAllKeys(jws);
            }

            if (key == null && chainResolverFallback != null) {
                LOG.debug("JWK is not available, neither 'kid' nor 'x5t#S256' nor 'x5t' token headers are set,"
                        + " falling back to the certificate chain resolver");
                key = chainResolverFallback.resolveKey(jws, nestingContext);
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
        public Key resolveKey(JsonWebSignature jws, List<JsonWebStructure> nestingContext)
                throws UnresolvableKeyException {
            return key;
        }

    }

    private class InternalSignatureKeyResolver implements VerificationKeyResolver {
        final Key internalSignatureKey;

        public InternalSignatureKeyResolver(Key generatedInternalSignatureKey) {
            this.internalSignatureKey = initKey(generatedInternalSignatureKey);
        }

        @Override
        public Key resolveKey(JsonWebSignature jws, List<JsonWebStructure> nestingContext)
                throws UnresolvableKeyException {
            return internalSignatureKey;
        }

        private Key initKey(Key generatedInternalSignatureKey) {
            String clientSecret = OidcCommonUtils.getClientOrJwtSecret(oidcConfig.credentials());
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

    private static final class CustomClaimsValidator implements Validator {

        private final Map<String, Set<String>> customClaims;

        private CustomClaimsValidator(Map<String, Set<String>> customClaims) {
            this.customClaims = customClaims;
        }

        @Override
        public String validate(JwtContext jwtContext) throws MalformedClaimException {
            var claims = jwtContext.getJwtClaims();
            for (var requiredClaim : customClaims.entrySet()) {
                String validationFailureMessage = validate(requiredClaim.getKey(), requiredClaim.getValue(), claims);
                if (validationFailureMessage != null) {
                    if (ACR.equals(requiredClaim.getKey())) {
                        throwAuthenticationFailedException(validationFailureMessage, requiredClaim.getValue());
                    }
                    return validationFailureMessage;
                }
            }
            return null;
        }

        private static String validate(String requiredClaimName, Set<String> requiredClaimValues, JwtClaims claims)
                throws MalformedClaimException {
            if (!claims.hasClaim(requiredClaimName)) {
                return "claim " + requiredClaimName + " is missing";
            }
            if (claims.isClaimValueString(requiredClaimName)) {
                if (requiredClaimValues.size() == 1) {
                    String actualClaimValue = claims.getStringClaimValue(requiredClaimName);
                    String requiredClaimValue = requiredClaimValues.iterator().next();
                    if (!requiredClaimValue.equals(actualClaimValue)) {
                        return "claim " + requiredClaimName + " does not match expected value of " + requiredClaimValues;
                    }
                } else {
                    throw new MalformedClaimException("expected claim " + requiredClaimName + " must be a list of strings");
                }
            } else {
                if (claims.isClaimValueStringList(requiredClaimName)) {
                    List<String> actualClaimValues = claims.getStringListClaimValue(requiredClaimName);
                    for (String requiredClaimValue : requiredClaimValues) {
                        if (!actualClaimValues.contains(requiredClaimValue)) {
                            return "claim " + requiredClaimName + " does not match expected value of " + requiredClaimValues;
                        }
                    }
                } else {
                    throw new MalformedClaimException(
                            "expected claim " + requiredClaimName + " must be a list of strings or a string");
                }
            }
            return null;
        }
    }

    private static Map<String, Object> tokenMap(String token, boolean idToken) {
        return Map.of(idToken ? OidcConstants.ID_TOKEN_VALUE : OidcConstants.ACCESS_TOKEN_VALUE, token);
    }

    private static final class CatchingErrorCodeValidator extends ErrorCodeValidatorAdapter {

        private AuthenticationFailedException authenticationFailure;

        private CatchingErrorCodeValidator(Validator validator) {
            super(validator);
        }

        @Override
        public Error validate(JwtContext jwtContext) throws MalformedClaimException {
            try {
                return super.validate(jwtContext);
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
}
