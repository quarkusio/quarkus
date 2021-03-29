package io.quarkus.oidc.runtime;

import java.security.Key;
import java.time.Duration;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jboss.logging.Logger;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.consumer.ErrorCodeValidator;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwx.JsonWebStructure;
import org.jose4j.keys.resolvers.VerificationKeyResolver;
import org.jose4j.lang.UnresolvableKeyException;

import io.quarkus.oidc.AuthorizationCodeTokens;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.OidcConfigurationMetadata;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.util.KeyUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class OidcProvider {

    private static final Logger LOG = Logger.getLogger(OidcProvider.class);

    final OidcProviderClient client;
    final RefreshableVerificationKeyResolver keyResolver;
    final OidcTenantConfig oidcConfig;

    public OidcProvider(OidcProviderClient client, OidcTenantConfig oidcConfig, JsonWebKeyCache jwks) {
        this.client = client;
        this.oidcConfig = oidcConfig;
        this.keyResolver = jwks == null ? null : new JsonWebKeyResolver(jwks, oidcConfig.token.forcedJwkRefreshInterval);
    }

    public OidcProvider(String publicKeyEnc, OidcTenantConfig oidcConfig) {
        this.client = null;
        this.oidcConfig = oidcConfig;
        this.keyResolver = new LocalPublicKeyResolver(publicKeyEnc);
    }

    public TokenVerificationResult verifyJwtToken(String token) throws InvalidJwtException {
        JwtConsumerBuilder builder = new JwtConsumerBuilder();

        builder.setVerificationKeyResolver(keyResolver);

        builder.setJwsAlgorithmConstraints(
                new AlgorithmConstraints(AlgorithmConstraints.ConstraintType.PERMIT, SignatureAlgorithm.RS256.getAlgorithm()));

        builder.setRequireExpirationTime();
        builder.setRequireIssuedAt();

        String issuer = oidcConfig.token.issuer.orElse(null);
        if (issuer == null && client != null) {
            issuer = client.getMetadata().getIssuer();
        }
        if (issuer != null) {
            builder.setExpectedIssuer(issuer);
        }
        if (oidcConfig.token.audience.isPresent()) {
            builder.setExpectedAudience(oidcConfig.token.audience.get().toArray(new String[] {}));
        } else {
            builder.setSkipDefaultAudienceValidation();
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
            LOG.debugf("Token verification has failed: %s", detail);
            throw ex;
        }
        return new TokenVerificationResult(OidcUtils.decodeJwtContent(token), null);
    }

    public Uni<TokenVerificationResult> refreshJwksAndVerifyJwtToken(String token) {
        return keyResolver.refresh().onItem().transformToUni(new Function<Void, Uni<? extends TokenVerificationResult>>() {

            @Override
            public Uni<? extends TokenVerificationResult> apply(Void v) {
                try {
                    return Uni.createFrom().item(verifyJwtToken(token));
                } catch (Throwable t) {
                    return Uni.createFrom().failure(t);
                }
            }

        });
    }

    public Uni<TokenVerificationResult> introspectToken(String token) {
        return client.introspectToken(token).onItemOrFailure()
                .transform(new BiFunction<JsonObject, Throwable, TokenVerificationResult>() {

                    @Override
                    public TokenVerificationResult apply(JsonObject jsonObject, Throwable t) {
                        if (t != null) {
                            throw new AuthenticationFailedException(t);
                        }
                        if (!Boolean.TRUE.equals(jsonObject.getBoolean(OidcConstants.INTROSPECTION_TOKEN_ACTIVE))) {
                            throw new AuthenticationFailedException();
                        }
                        Long exp = jsonObject.getLong(OidcConstants.INTROSPECTION_TOKEN_EXP);
                        if (exp != null) {
                            final int lifespanGrace = client.getOidcConfig().token.lifespanGrace.isPresent()
                                    ? client.getOidcConfig().token.lifespanGrace.getAsInt()
                                    : 0;
                            if (System.currentTimeMillis() / 1000 > exp + lifespanGrace) {
                                throw new AuthenticationFailedException();
                            }
                        }

                        return new TokenVerificationResult(null, jsonObject);
                    }

                });
    }

    public Uni<JsonObject> getUserInfo(RoutingContext vertxContext, TokenAuthenticationRequest request) {
        String accessToken = vertxContext.get(OidcConstants.ACCESS_TOKEN_VALUE);
        if (accessToken == null) {
            accessToken = request.getToken().getToken();
        }
        return client.getUserInfo(accessToken);
    }

    public Uni<AuthorizationCodeTokens> getCodeFlowTokens(String code, String redirectUri) {
        return client.getAuthorizationCodeTokens(code, redirectUri);
    }

    public Uni<AuthorizationCodeTokens> refreshTokens(String refreshToken) {
        return client.refreshAuthorizationCodeTokens(refreshToken);
    }

    private class JsonWebKeyResolver implements RefreshableVerificationKeyResolver {
        volatile JsonWebKeyCache jwks;
        volatile long lastForcedRefreshTime;
        volatile long forcedJwksRefreshIntervalMilliSecs;

        JsonWebKeyResolver(JsonWebKeyCache jwks, Duration forcedJwksRefreshInterval) {
            this.jwks = jwks;
            this.forcedJwksRefreshIntervalMilliSecs = forcedJwksRefreshInterval.toMillis();
        }

        @Override
        public Key resolveKey(JsonWebSignature jws, List<JsonWebStructure> nestingContext)
                throws UnresolvableKeyException {
            String kid = jws.getKeyIdHeaderValue();
            if (kid == null) {
                throw new UnresolvableKeyException("Token 'kid' header is not set");
            }
            Key key = jwks.getKey(kid);
            if (key == null) {
                throw new UnresolvableKeyException(String.format("JWK with kid '%s' is not available", kid));
            }
            return key;
        }

        public Uni<Void> refresh() {
            final long now = System.currentTimeMillis();
            if (now > lastForcedRefreshTime + forcedJwksRefreshIntervalMilliSecs) {
                lastForcedRefreshTime = now;
                return client.getJsonWebKeySet().onItem().transformToUni(new Function<JsonWebKeyCache, Uni<? extends Void>>() {

                    @Override
                    public Uni<? extends Void> apply(JsonWebKeyCache t) {
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

    public OidcConfigurationMetadata getMetadata() {
        return client.getMetadata();
    }

    private static interface RefreshableVerificationKeyResolver extends VerificationKeyResolver {
        default Uni<Void> refresh() {
            return Uni.createFrom().voidItem();
        }
    }
}
