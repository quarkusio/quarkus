package io.quarkus.oidc.runtime;

import java.security.Key;
import java.util.HashMap;
import java.util.Map;

import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.lang.JoseException;

import io.quarkus.oidc.OIDCException;

public class JsonWebKeySet {
    private static final String RSA_KEY_TYPE = "RSA";
    private static final String EC_KEY_TYPE = "EC";
    private static final String SIGNATURE_USE = "sig";

    private Map<String, Key> keysWithKeyId = new HashMap<>();
    private Map<String, Key> keysWithThumbprints = new HashMap<>();

    public JsonWebKeySet(String json) {
        initKeys(json);
    }

    private void initKeys(String json) {
        try {
            org.jose4j.jwk.JsonWebKeySet jwkSet = new org.jose4j.jwk.JsonWebKeySet(json);
            for (JsonWebKey jwkKey : jwkSet.getJsonWebKeys()) {
                if ((RSA_KEY_TYPE.equals(jwkKey.getKeyType()) || EC_KEY_TYPE.equals(jwkKey.getKeyType())
                        || jwkKey.getKeyType() == null)
                        && (SIGNATURE_USE.equals(jwkKey.getUse()) || jwkKey.getUse() == null)) {
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
        } catch (JoseException ex) {
            throw new OIDCException(ex);
        }
    }

    public Key getKeyWithId(String kid) {
        return keysWithKeyId.get(kid);
    }

    public Key getKeyWithThumbprint(String x5t) {
        return keysWithThumbprints.get(x5t);
    }
}
