package io.quarkus.oidc.runtime;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import jakarta.json.JsonObject;

import org.eclipse.microprofile.jwt.Claims;
import org.jboss.logging.Logger;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.ErrorCodeValidator;
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

import io.quarkus.logging.Log;
import io.quarkus.oidc.AuthorizationCodeTokens;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcConfigurationMetadata;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.CertificateChain;
import io.quarkus.oidc.TokenCustomizer;
import io.quarkus.oidc.TokenIntrospection;
import io.quarkus.oidc.UserInfo;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.credential.TokenCredential;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.util.KeyUtils;
import io.smallrye.mutiny.Uni;

public class OidcProvider implements Closeable {

    private static final Logger LOG = Logger.getLogger(OidcProvider.class);
    private static final String ANY_ISSUER = "any";
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
    private static final AlgorithmConstraints ASYMMETRIC_ALGORITHM_CONSTRAINTS = new AlgorithmConstraints(
            AlgorithmConstraints.ConstraintType.PERMIT, ASYMMETRIC_SUPPORTED_ALGORITHMS);
    private static final AlgorithmConstraints SYMMETRIC_ALGORITHM_CONSTRAINTS = new AlgorithmConstraints(
            AlgorithmConstraints.ConstraintType.PERMIT, SignatureAlgorithm.HS256.getAlgorithm());

    final OidcProviderClient client;
    final RefreshableVerificationKeyResolver asymmetricKeyResolver;
    final DynamicVerificationKeyResolver keyResolverProvider;
    final OidcTenantConfig oidcConfig;
    final TokenCustomizer tokenCustomizer;
    final String issuer;
    final String[] audience;
    final Map<String, String> requiredClaims;
    final Key tokenDecryptionKey;
    final AlgorithmConstraints requiredAlgorithmConstraints;

    public OidcProvider(OidcProviderClient client, OidcTenantConfig oidcConfig, JsonWebKeySet jwks, Key tokenDecryptionKey) {
        this(client, oidcConfig, jwks, TokenCustomizerFinder.find(oidcConfig), tokenDecryptionKey);
    }

    public OidcProvider(OidcProviderClient client, OidcTenantConfig oidcConfig, JsonWebKeySet jwks,
            TokenCustomizer tokenCustomizer, Key tokenDecryptionKey) {
        this.client = client;
        this.oidcConfig = oidcConfig;
        this.tokenCustomizer = tokenCustomizer;
        this.asymmetricKeyResolver = jwks == null ? null
                : new JsonWebKeyResolver(jwks, oidcConfig.token.forcedJwkRefreshInterval, oidcConfig.certificateChain);
        if (client != null && oidcConfig != null && !oidcConfig.jwks.resolveEarly) {
            this.keyResolverProvider = new DynamicVerificationKeyResolver(client, oidcConfig);
        } else {
            this.keyResolverProvider = null;
        }
        this.issuer = checkIssuerProp();
        this.audience = checkAudienceProp();
        this.requiredClaims = checkRequiredClaimsProp();
        this.tokenDecryptionKey = tokenDecryptionKey;
        this.requiredAlgorithmConstraints = checkSignatureAlgorithm();
    }

    public OidcProvider(String publicKeyEnc, OidcTenantConfig oidcConfig, Key tokenDecryptionKey) {
        this.client = null;
        this.oidcConfig = oidcConfig;
        this.tokenCustomizer = TokenCustomizerFinder.find(oidcConfig);
        if (publicKeyEnc != null) {
            this.asymmetricKeyResolver = new LocalPublicKeyResolver(publicKeyEnc);
        } else if (oidcConfig.certificateChain.trustStoreFile.isPresent()) {
            this.asymmetricKeyResolver = new CertChainPublicKeyResolver(oidcConfig.certificateChain);
        } else {
            throw new IllegalStateException("Neither public key nor certificate chain verification modes are enabled");
        }
        this.keyResolverProvider = null;
        this.issuer = checkIssuerProp();
        this.audience = checkAudienceProp();
        this.requiredClaims = checkRequiredClaimsProp();
        this.tokenDecryptionKey = tokenDecryptionKey;
        this.requiredAlgorithmConstraints = checkSignatureAlgorithm();
    }

    private AlgorithmConstraints checkSignatureAlgorithm() {
        if (oidcConfig != null && oidcConfig.token.signatureAlgorithm.isPresent()) {
            String configuredAlg = oidcConfig.token.signatureAlgorithm.get().getAlgorithm();
            return new AlgorithmConstraints(AlgorithmConstraints.ConstraintType.PERMIT, configuredAlg);
        } else {
            return null;
        }
    }

    private String checkIssuerProp() {
        String issuerProp = null;
        if (oidcConfig != null) {
            issuerProp = oidcConfig.token.issuer.orElse(null);
            if (issuerProp == null && client != null) {
                issuerProp = client.getMetadata().getIssuer();
            }
        }
        return ANY_ISSUER.equals(issuerProp) ? null : issuerProp;
    }

    private String[] checkAudienceProp() {
        List<String> audienceProp = oidcConfig != null ? oidcConfig.token.audience.orElse(null) : null;
        return audienceProp != null ? audienceProp.toArray(new String[] {}) : null;
    }

    private Map<String, String> checkRequiredClaimsProp() {
        return oidcConfig != null ? oidcConfig.token.requiredClaims : null;
    }

    public TokenVerificationResult verifySelfSignedJwtToken(String token) throws InvalidJwtException {
        return verifyJwtTokenInternal(token, true, false, null, SYMMETRIC_ALGORITHM_CONSTRAINTS, new SymmetricKeyResolver(),
                true);
    }

    public TokenVerificationResult verifyJwtToken(String token, boolean enforceAudienceVerification, boolean subjectRequired,
            String nonce)
            throws InvalidJwtException {
        return verifyJwtTokenInternal(customizeJwtToken(token), enforceAudienceVerification, subjectRequired, nonce,
                (requiredAlgorithmConstraints != null ? requiredAlgorithmConstraints : ASYMMETRIC_ALGORITHM_CONSTRAINTS),
                asymmetricKeyResolver, true);
    }

    public TokenVerificationResult verifyLogoutJwtToken(String token) throws InvalidJwtException {
        final boolean enforceExpReq = !oidcConfig.token.age.isPresent();
        TokenVerificationResult result = verifyJwtTokenInternal(token, true, false, null, ASYMMETRIC_ALGORITHM_CONSTRAINTS,
                asymmetricKeyResolver,
                enforceExpReq);
        if (!enforceExpReq) {
            // Expiry check was skipped during the initial verification but if the logout token contains the exp claim
            // then it must be verified
            if (isTokenExpired(result.localVerificationResult.getLong(Claims.exp.name()))) {
                String error = String.format("Logout token for client %s has expired", oidcConfig.clientId.get());
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
            VerificationKeyResolver verificationKeyResolver, boolean enforceExpReq) throws InvalidJwtException {
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
            builder.registerValidator(new CustomClaimsValidator(Map.of(OidcConstants.NONCE, nonce)));
        }

        builder.setRequireIssuedAt();

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
            builder.setExpectedAudience(oidcConfig.clientId.get());
        } else {
            builder.setSkipDefaultAudienceValidation();
        }
        if (requiredClaims != null && !requiredClaims.isEmpty()) {
            builder.registerValidator(new CustomClaimsValidator(requiredClaims));
        }

        if (oidcConfig.token.lifespanGrace.isPresent()) {
            final int lifespanGrace = oidcConfig.token.lifespanGrace.getAsInt();
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
            if (oidcConfig.clientId.isPresent()) {
                LOG.debugf("Verification of the token issued to client %s has failed: %s", oidcConfig.clientId.get(), detail);
            } else {
                LOG.debugf("Token verification has failed: %s", detail);
            }
            throw ex;
        }
        TokenVerificationResult result = new TokenVerificationResult(OidcUtils.decodeJwtContent(token), null);

        verifyTokenAge(result.localVerificationResult.getLong(Claims.iat.name()));
        return result;
    }

    private String customizeJwtToken(String token) {
        if (tokenCustomizer != null) {
            JsonObject headers = AbstractJsonObjectResponse.toJsonObject(
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
        if (oidcConfig.token.age.isPresent() && iat != null) {
            final long now = now() / 1000;

            if (now - iat > oidcConfig.token.age.get().toSeconds() + getLifespanGrace()) {
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
            boolean subjectRequired, String nonce) {
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
                                            resolver, true));
                        } catch (Throwable t) {
                            return Uni.createFrom().failure(t);
                        }
                    }

                });
    }

    public Uni<TokenIntrospection> introspectToken(String token, boolean fallbackFromJwkMatch) {
        if (client.getMetadata().getIntrospectionUri() == null) {
            String errorMessage = String.format("Token issued to client %s "
                    + (fallbackFromJwkMatch ? "does not have a matching verification key and it " : "")
                    + "can not be introspected because the introspection endpoint address is unknown - "
                    + "please check if your OpenId Connect Provider supports the token introspection",
                    oidcConfig.clientId.get());

            throw new AuthenticationFailedException(errorMessage);
        }
        return client.introspectToken(token).onItemOrFailure()
                .transform(new BiFunction<TokenIntrospection, Throwable, TokenIntrospection>() {

                    @Override
                    public TokenIntrospection apply(TokenIntrospection introspectionResult, Throwable t) {
                        if (t != null) {
                            throw new AuthenticationFailedException(t);
                        }
                        if (!introspectionResult.isActive()) {
                            verifyTokenExpiry(introspectionResult.getLong(OidcConstants.INTROSPECTION_TOKEN_EXP));
                            throw new AuthenticationFailedException(
                                    String.format("Token issued to client %s is not active", oidcConfig.clientId.get()));
                        }
                        verifyTokenExpiry(introspectionResult.getLong(OidcConstants.INTROSPECTION_TOKEN_EXP));
                        try {
                            verifyTokenAge(introspectionResult.getLong(OidcConstants.INTROSPECTION_TOKEN_IAT));
                        } catch (InvalidJwtException ex) {
                            throw new AuthenticationFailedException(ex);
                        }

                        return introspectionResult;
                    }

                    private void verifyTokenExpiry(Long exp) {
                        if (isTokenExpired(exp)) {
                            String error = String.format("Token issued to client %s has expired",
                                    oidcConfig.clientId.get());
                            LOG.debugf(error);
                            throw new AuthenticationFailedException(
                                    new InvalidJwtException(error,
                                            List.of(new ErrorCodeValidator.Error(ErrorCodes.EXPIRED, error)), null));
                        }
                    }

                });
    }

    private boolean isTokenExpired(Long exp) {
        return exp != null && now() / 1000 > exp + getLifespanGrace();
    }

    private int getLifespanGrace() {
        return client.getOidcConfig().token.lifespanGrace.isPresent()
                ? client.getOidcConfig().token.lifespanGrace.getAsInt()
                : 0;
    }

    private static final long now() {
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

        JsonWebKeyResolver(JsonWebKeySet jwks, Duration forcedJwksRefreshInterval, CertificateChain chain) {
            this.jwks = jwks;
            this.forcedJwksRefreshIntervalMilliSecs = forcedJwksRefreshInterval.toMillis();
            if (chain.trustStoreFile.isPresent()) {
                chainResolverFallback = new CertChainPublicKeyResolver(chain);
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
                    Log.debug("Token 'alg'(algorithm) header value is invalid", ex);
                }
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

    private class SymmetricKeyResolver implements VerificationKeyResolver {
        @Override
        public Key resolveKey(JsonWebSignature jws, List<JsonWebStructure> nestingContext)
                throws UnresolvableKeyException {
            return KeyUtils.createSecretKeyFromSecret(OidcCommonUtils.clientSecret(oidcConfig.credentials));
        }
    }

    public OidcConfigurationMetadata getMetadata() {
        return client.getMetadata();
    }

    private static class CustomClaimsValidator implements Validator {

        private final Map<String, String> customClaims;

        public CustomClaimsValidator(Map<String, String> customClaims) {
            this.customClaims = customClaims;
        }

        @Override
        public String validate(JwtContext jwtContext) throws MalformedClaimException {
            var claims = jwtContext.getJwtClaims();
            for (var targetClaim : customClaims.entrySet()) {
                var claimName = targetClaim.getKey();
                if (!claims.hasClaim(claimName)) {
                    return "claim " + claimName + " is missing";
                }
                if (!claims.isClaimValueString(claimName)) {
                    throw new MalformedClaimException("expected claim " + claimName + " to be a string");
                }
                var claimValue = claims.getStringClaimValue(claimName);
                var targetValue = targetClaim.getValue();
                if (!claimValue.equals(targetValue)) {
                    return "claim " + claimName + "does not match expected value of " + targetValue;
                }
            }
            return null;
        }
    }
}
