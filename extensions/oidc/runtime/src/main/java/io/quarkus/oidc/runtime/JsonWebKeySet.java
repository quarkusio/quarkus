package io.quarkus.oidc.runtime;

import java.security.Key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.InvalidAlgorithmException;
import org.jose4j.lang.JoseException;

import io.quarkus.oidc.OIDCException;

public class JsonWebKeySet {

    private static final Logger LOG = Logger.getLogger(JsonWebKeySet.class);
    private static final String RSA_KEY_TYPE = "RSA";
    private static final String ELLIPTIC_CURVE_KEY_TYPE = "EC";
    // This key type is used when EdDSA algorithm is used
    private static final String OCTET_KEY_PAIR_TYPE = "OKP";
    private static final Set<String> KEY_TYPES = Set.of(RSA_KEY_TYPE, ELLIPTIC_CURVE_KEY_TYPE, OCTET_KEY_PAIR_TYPE);

    private static final String SIGNATURE_USE = "sig";

    private Map<String, Key> keysWithKeyId = new HashMap<>();
    private Map<String, Key> keysWithThumbprints = new HashMap<>();
    private Map<String, Key> keysWithS256Thumbprints = new HashMap<>();
    private Map<String, List<Key>> keysWithoutKeyIdAndThumbprint = new HashMap<>();
    private Map<String, List<Key>> allKeys = new HashMap<>();

    public JsonWebKeySet(String json) {
        initKeys(json);
    }

    private void initKeys(String json) {
        try {
            org.jose4j.jwk.JsonWebKeySet jwkSet = new org.jose4j.jwk.JsonWebKeySet(json);
            for (JsonWebKey jwkKey : jwkSet.getJsonWebKeys()) {
                if (isSupportedJwkKey(jwkKey)) {
                    addKeyToListInMap(jwkKey, allKeys);

                    if (jwkKey.getKeyId() != null) {
                        keysWithKeyId.put(jwkKey.getKeyId(), jwkKey.getKey());
                    }
                    // 'x5t' may not be available but the certificate `x5c` may be so 'x5t' can be calculated early
                    boolean calculateThumbprintIfMissing = true;
                    String x5t = ((PublicJsonWebKey) jwkKey).getX509CertificateSha1Thumbprint(calculateThumbprintIfMissing);
                    if (x5t != null && jwkKey.getKey() != null) {
                        keysWithThumbprints.put(x5t, jwkKey.getKey());
                    }
                    String x5tS256 = ((PublicJsonWebKey) jwkKey)
                            .getX509CertificateSha256Thumbprint(calculateThumbprintIfMissing);
                    if (x5tS256 != null && jwkKey.getKey() != null) {
                        keysWithS256Thumbprints.put(x5tS256, jwkKey.getKey());
                    }
                    if (jwkKey.getKeyId() == null && x5t == null && x5tS256 == null && jwkKey.getKeyType() != null) {
                        addKeyToListInMap(jwkKey, keysWithoutKeyIdAndThumbprint);
                    }
                }
            }
        } catch (JoseException ex) {
            throw new OIDCException(ex);
        }
    }

    private static boolean isSupportedJwkKey(JsonWebKey jwkKey) {
        return (jwkKey.getKeyType() == null || KEY_TYPES.contains(jwkKey.getKeyType()))
                && (SIGNATURE_USE.equals(jwkKey.getUse()) || jwkKey.getUse() == null);
    }

    private void addKeyToListInMap(JsonWebKey key, Map<String, List<Key>> map) {
        List<Key> keys = map.get(key.getKeyType());

        if (keys == null) {
            keys = new ArrayList<>();
            map.put(key.getKeyType(), keys);
        }

        keys.add(key.getKey());
    }

    public Key findKeyInAllKeys(JsonWebSignature jws) {
        LOG.debug("Evaluating all keys to find a matching one");
        final Key initialKey = jws.getKey();
        final String keyType;

        try {
            keyType = jws.getKeyType();
        } catch (InvalidAlgorithmException e) {
            LOG.debug("No key type available, cannot determine keys to check", e);
            return null;
        }

        for (Key key : allKeys.getOrDefault(keyType, List.of())) {
            jws.setKey(key);

            try {
                if (jws.verifySignature()) {
                    jws.setKey(initialKey);
                    LOG.debugf("Found matching key %s", key.toString());
                    return key;
                }
            } catch (JoseException e) {
                LOG.debugf(e, "Verifying signature with key %s failed.", key.toString());
            }
        }

        jws.setKey(initialKey);
        LOG.debug("No matching key found");
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
