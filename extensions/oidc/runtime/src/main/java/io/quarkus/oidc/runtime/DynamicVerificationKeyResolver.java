package io.quarkus.oidc.runtime;

import java.security.Key;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import jakarta.enterprise.event.Observes;

import org.jboss.logging.Logger;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.common.OidcRequestContextProperties;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.credential.TokenCredential;
import io.smallrye.jwt.auth.SingleKeyVerificationKeyResolver;
import io.smallrye.jwt.auth.UnresolvableKeyException;
import io.smallrye.jwt.auth.VerificationKeyResolver;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class DynamicVerificationKeyResolver {
    private static final Logger LOG = Logger.getLogger(DynamicVerificationKeyResolver.class);
    private static final Set<String> KEY_HEADERS = Set.of(JoseConstants.KEY_ID,
            JoseConstants.X_509_CERT_SHA_256_THUMBPRINT, JoseConstants.X_509_CERT_SHA_1_THUMBPRINT);

    private final OidcProviderClientImpl client;
    private final MemoryCache<Key> cache;
    final CertChainPublicKeyResolver chainResolverFallback;

    public DynamicVerificationKeyResolver(OidcProviderClientImpl client, OidcTenantConfig config) {
        this.client = client;
        this.cache = new MemoryCache<Key>(client.getVertx(), config.jwks().cleanUpTimerInterval(),
                config.jwks().cacheTimeToLive(), config.jwks().cacheSize());
        if (config.certificateChain().trustStoreFile().isPresent()) {
            chainResolverFallback = new CertChainPublicKeyResolver(config);
        } else {
            chainResolverFallback = null;
        }
    }

    public Uni<VerificationKeyResolver> resolve(TokenCredential tokenCred) {
        JsonObject headers = OidcUtils.decodeJwtHeaders(tokenCred.getToken());
        if (headers == null) {
            LOG.debug("Invalid JWT token format");
            return Uni.createFrom().failure(new AuthenticationFailedException());
        }
        Key key = findKeyInTheCache(headers);
        if (key != null) {
            return Uni.createFrom().item(new SingleKeyVerificationKeyResolver(key));
        }
        if (chainResolverFallback != null && headers.containsKey(JoseConstants.X_509_CERT_CHAIN)
                && Collections.disjoint(KEY_HEADERS, headers.fieldNames())) {
            // If none of the key headers is available which can be used to resolve JWK then do
            // not try to get another JWK set but delegate to the chain resolver fallback if it is available
            return getChainResolver();
        }

        return client.getJsonWebKeySet(new OidcRequestContextProperties(
                Map.of(OidcRequestContextProperties.TOKEN, tokenCred.getToken(),
                        OidcRequestContextProperties.TOKEN_CREDENTIAL, tokenCred)))
                .onItem().transformToUni(new Function<JsonWebKeySet, Uni<? extends VerificationKeyResolver>>() {

                    @Override
                    public Uni<? extends VerificationKeyResolver> apply(JsonWebKeySet jwks) {
                        Key newKey = null;
                        // Try 'kid' first
                        String kid = headers.getString(JoseConstants.KEY_ID);
                        if (kid != null) {
                            newKey = getKeyWithId(jwks, kid);
                            if (newKey == null) {
                                // if `kid` was set then the key must exist
                                return Uni.createFrom().failure(
                                        new UnresolvableKeyException(
                                                String.format("JWK with kid '%s' is not available", kid)));
                            } else {
                                cache.add(kid, newKey);
                            }
                        }

                        String thumbprint = null;
                        if (newKey == null) {
                            thumbprint = headers.getString(JoseConstants.X_509_CERT_SHA_256_THUMBPRINT);
                            if (thumbprint != null) {
                                newKey = getKeyWithS256Thumbprint(jwks, thumbprint);
                                if (newKey == null) {
                                    // if only `x5tS256` was set then the key must exist
                                    return Uni.createFrom().failure(
                                            new UnresolvableKeyException(String.format(
                                                    "JWK with the SHA256 certificate thumbprint '%s' is not available",
                                                    thumbprint)));
                                } else {
                                    cache.add(thumbprint, newKey);
                                }
                            }
                        }

                        if (newKey == null) {
                            thumbprint = headers.getString(JoseConstants.X_509_CERT_SHA_1_THUMBPRINT);
                            if (thumbprint != null) {
                                newKey = getKeyWithThumbprint(jwks, thumbprint);
                                if (newKey == null) {
                                    // if only `x5t` was set then the key must exist
                                    return Uni.createFrom().failure(new UnresolvableKeyException(
                                            String.format("JWK with the certificate thumbprint '%s' is not available",
                                                    thumbprint)));
                                } else {
                                    cache.add(thumbprint, newKey);
                                }
                            }
                        }

                        if (newKey == null && kid == null && thumbprint == null) {
                            newKey = jwks.getKeyWithoutKeyIdAndThumbprint("RSA");
                        }

                        if (newKey == null && chainResolverFallback != null) {
                            return getChainResolver();
                        }

                        if (newKey == null) {
                            return Uni.createFrom().failure(new UnresolvableKeyException(
                                    "JWK is not available, neither 'kid' nor 'x5t#S256' nor 'x5t' token headers are set"));
                        } else {
                            return Uni.createFrom().item(new SingleKeyVerificationKeyResolver(newKey));
                        }
                    }

                });
    }

    private Uni<VerificationKeyResolver> getChainResolver() {
        LOG.debug("JWK is not available, neither 'kid' nor 'x5t#S256' nor 'x5t' token headers are set,"
                + " falling back to the certificate chain resolver");
        return Uni.createFrom().item(chainResolverFallback);
    }

    private static Key getKeyWithId(JsonWebKeySet jwks, String kid) {
        if (kid != null) {
            return jwks.getKeyWithId(kid);
        } else {
            LOG.debug("Token 'kid' header is not set");
            return null;
        }
    }

    private Key getKeyWithThumbprint(JsonWebKeySet jwks, String thumbprint) {
        if (thumbprint != null) {
            return jwks.getKeyWithThumbprint(thumbprint);
        } else {
            LOG.debug("Token 'x5t' header is not set");
            return null;
        }
    }

    private Key getKeyWithS256Thumbprint(JsonWebKeySet jwks, String thumbprint) {
        if (thumbprint != null) {
            return jwks.getKeyWithS256Thumbprint(thumbprint);
        } else {
            LOG.debug("Token 'x5tS256' header is not set");
            return null;
        }
    }

    private Key findKeyInTheCache(JsonObject headers) {
        String kid = headers.getString(JoseConstants.KEY_ID);
        if (kid != null && cache.containsKey(kid)) {
            return cache.get(kid);
        }
        String thumbprint = headers.getString(JoseConstants.X_509_CERT_SHA_256_THUMBPRINT);
        if (thumbprint != null && cache.containsKey(thumbprint)) {
            return cache.get(thumbprint);
        }

        thumbprint = headers.getString(JoseConstants.X_509_CERT_SHA_1_THUMBPRINT);
        if (thumbprint != null && cache.containsKey(thumbprint)) {
            return cache.get(thumbprint);
        }

        return null;
    }

    void shutdown(@Observes ShutdownEvent event, Vertx vertx) {
        cache.stopTimer(vertx);
    }
}
