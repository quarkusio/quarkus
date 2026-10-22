package io.quarkus.oidc.runtime;

import java.security.Key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;

import io.quarkus.oidc.OIDCException;
import io.smallrye.jwk.AsymmetricJsonWebKey;
import io.smallrye.jwk.JsonWebKey;
import io.smallrye.jwk.JsonWebKeyException;
import io.smallrye.jws.JsonWebSignature;
import io.smallrye.jws.JwsException;
import io.smallrye.jws.JwsVerifier;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;

public class JsonWebKeySet {

    private static final Logger LOG = Logger.getLogger(JsonWebKeySet.class);
    private static final Set<String> KEY_TYPES = Set.of(JoseConstants.RSA_KEY_TYPE,
            JoseConstants.ELLIPTIC_CURVE_KEY_TYPE, JoseConstants.OCTET_KEY_PAIR_KEY_TYPE);

    private Map<String, Key> keysWithKeyId = new HashMap<>();
    private Map<String, Key> keysWithThumbprints = new HashMap<>();
    private Map<String, Key> keysWithS256Thumbprints = new HashMap<>();
    private Map<String, List<Key>> keysWithoutKeyIdAndThumbprint = new HashMap<>();
    private Map<String, List<Key>> allKeys = new HashMap<>();

    public JsonWebKeySet(String json) {
        initKeys(json);
    }

    private void initKeys(String json) {
        io.smallrye.jwk.JsonWebKeySet jwkSet;
        try {
            jwkSet = io.smallrye.jwk.JsonWebKeySet.parse(json);
        } catch (JsonWebKeyException ex) {
            throw new OIDCException(ex);
        }

        for (JsonWebKey jwkKey : jwkSet.keys()) {
            if (isSupportedJwkKey(jwkKey)) {
                Key key;
                try {
                    key = extractKey(jwkKey);
                } catch (JsonWebKeyException ex) {
                    logKeyExtractionFailure(jwkKey, ex);
                    continue;
                }
                final String keyType = jwkKey.keyType();

                addKeyToListInMap(keyType, key, allKeys);

                if (jwkKey.keyId() != null) {
                    keysWithKeyId.put(jwkKey.keyId(), key);
                }

                // The thumbprints are calculated from the certificate chain if they are not set
                String x5t = jwkKey.x509CertificateThumbprint();
                String x5tS256 = jwkKey.x509CertificateS256Thumbprint();

                if (x5t != null) {
                    keysWithThumbprints.put(x5t, key);
                }
                if (x5tS256 != null) {
                    keysWithS256Thumbprints.put(x5tS256, key);
                }

                if (jwkKey.keyId() == null && x5t == null && x5tS256 == null && keyType != null) {
                    addKeyToListInMap(keyType, key, keysWithoutKeyIdAndThumbprint);
                }
            }
        }
    }

    private static Key extractKey(JsonWebKey jwkKey) throws JsonWebKeyException {
        // only the asymmetric key types are supported, see isSupportedJwkKey()
        return ((AsymmetricJsonWebKey) jwkKey).publicKey();
    }

    private static void logKeyExtractionFailure(JsonWebKey jwkKey, JsonWebKeyException ex) {
        LOG.warnf(ex, "Supported JWK of type '%s' with key id '%s' can not be converted to a key and will be ignored",
                jwkKey.keyType(), jwkKey.keyId());
    }

    private static boolean isSupportedJwkKey(JsonWebKey jwkKey) {
        String keyType = jwkKey.keyType();
        String use = jwkKey.keyUse();
        return (keyType == null || KEY_TYPES.contains(keyType))
                && (JoseConstants.SIGNATURE_USE.equals(use) || use == null);
    }

    private void addKeyToListInMap(String keyType, Key key, Map<String, List<Key>> map) {
        List<Key> keys = map.get(keyType);

        if (keys == null) {
            keys = new ArrayList<>();
            map.put(keyType, keys);
        }

        keys.add(key);
    }

    public Key findKeyInAllKeys(JsonWebSignature jws, Set<SignatureAlgorithm> allowedAlgorithms) {
        LOG.debug("Evaluating all keys to find a matching one");

        String alg = jws.headers().algorithm();
        if (allowedAlgorithms.stream().noneMatch(a -> a.getAlgorithm().equals(alg))) {
            LOG.debugf("Token algorithm %s is not allowed", alg);
            return null;
        }
        String keyType = getKeyTypeFromAlgorithm(alg);
        if (keyType == null) {
            LOG.debug("No key type available, cannot determine keys to check");
            return null;
        }

        for (Key key : allKeys.getOrDefault(keyType, List.of())) {
            try {
                JwsVerifier.builder(key).algorithms(allowedAlgorithms).build().verify(jws);
                LOG.debugf("Found matching key %s", key.toString());
                return key;
            } catch (JwsException e) {
                LOG.debugf(e, "Verifying signature with key %s failed.", key.toString());
            }
        }

        LOG.debug("No matching key found");
        return null;
    }

    static String getKeyTypeFromAlgorithm(String alg) {
        if (alg.startsWith("RS") || alg.startsWith("PS")) {
            return JoseConstants.RSA_KEY_TYPE;
        }
        if (alg.startsWith("ES")) {
            return JoseConstants.ELLIPTIC_CURVE_KEY_TYPE;
        }
        if (alg.equals("EdDSA")) {
            return JoseConstants.OCTET_KEY_PAIR_KEY_TYPE;
        }
        return null;
    }

    public Key getKeyWithId(String kid) {
        return keysWithKeyId.get(kid);
    }

    public Key getKeyWithThumbprint(String x5t) {
        return keysWithThumbprints.get(x5t);
    }

    public Key getKeyWithS256Thumbprint(String x5tS256) {
        return keysWithS256Thumbprints.get(x5tS256);
    }

    public Key getKeyWithoutKeyIdAndThumbprint(String keyType) {
        List<Key> keys = keysWithoutKeyIdAndThumbprint.get(keyType);
        return keys == null || keys.size() != 1 ? null : keys.get(0);
    }
}
