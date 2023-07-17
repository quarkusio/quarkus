package io.quarkus.oidc.runtime;

import java.security.Key;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.lang.JoseException;

import io.quarkus.oidc.OIDCException;

public class JsonWebKeySet {

    private static final String RSA_KEY_TYPE = "RSA";
    private static final String ELLIPTIC_CURVE_KEY_TYPE = "EC";
    // This key type is used when EdDSA algorithm is used
    private static final String OCTET_KEY_PAIR_TYPE = "OKP";
    private static final Set<String> KEY_TYPES = Set.of(RSA_KEY_TYPE, ELLIPTIC_CURVE_KEY_TYPE, OCTET_KEY_PAIR_TYPE);

    private static final String SIGNATURE_USE = "sig";

    private Map<String, Key> keysWithKeyId = new HashMap<>();
    private Map<String, Key> keysWithThumbprints = new HashMap<>();
    private Key keyWithoutKeyIdAndThumbprint;

    public JsonWebKeySet(String json) {
        initKeys(json);
    }

    private void initKeys(String json) {
        try {
            org.jose4j.jwk.JsonWebKeySet jwkSet = new org.jose4j.jwk.JsonWebKeySet(json);
            for (JsonWebKey jwkKey : jwkSet.getJsonWebKeys()) {
                if (isSupportedJwkKey(jwkKey)) {
                    if (jwkKey.getKeyId() != null) {
                        keysWithKeyId.put(jwkKey.getKeyId(), jwkKey.getKey());
                    }
                    // 'x5t' may not be available but the certificate `x5c` may be so 'x5t' can be calculated early
                    boolean calculateThumbprintIfMissing = true;
                    String x5t = ((PublicJsonWebKey) jwkKey).getX509CertificateSha1Thumbprint(calculateThumbprintIfMissing);
                    if (x5t != null && jwkKey.getKey() != null) {
                        keysWithThumbprints.put(x5t, jwkKey.getKey());
                    }
                }
            }
            if (keysWithKeyId.isEmpty() && keysWithThumbprints.isEmpty() && jwkSet.getJsonWebKeys().size() == 1
                    && isSupportedJwkKey(jwkSet.getJsonWebKeys().get(0))) {
                keyWithoutKeyIdAndThumbprint = jwkSet.getJsonWebKeys().get(0).getKey();
            }
        } catch (JoseException ex) {
            throw new OIDCException(ex);
        }
    }

    private static boolean isSupportedJwkKey(JsonWebKey jwkKey) {
        return (jwkKey.getKeyType() == null || KEY_TYPES.contains(jwkKey.getKeyType()))
                && (SIGNATURE_USE.equals(jwkKey.getUse()) || jwkKey.getUse() == null);
    }

    public Key getKeyWithId(String kid) {
        return keysWithKeyId.get(kid);
    }

    public Key getKeyWithThumbprint(String x5t) {
        return keysWithThumbprints.get(x5t);
    }

    public Key getKeyWithoutKeyIdAndThumbprint() {
        return keyWithoutKeyIdAndThumbprint;
    }
}
