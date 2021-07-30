package io.quarkus.oidc.runtime;

import java.security.Key;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jose4j.jwk.JsonWebKey;
import org.jose4j.lang.JoseException;

import io.quarkus.oidc.OIDCException;

public class JsonWebKeySet {
    private static final String RSA_KEY_TYPE = "RSA";
    private static final String EC_KEY_TYPE = "EC";
    private static final String SIGNATURE_USE = "sig";

    private Map<String, Key> keys = new HashMap<>();

    public JsonWebKeySet(String json) {
        initKeys(json);
    }

    private void initKeys(String json) {
        try {
            org.jose4j.jwk.JsonWebKeySet jwkSet = new org.jose4j.jwk.JsonWebKeySet(json);
            for (JsonWebKey jwkKey : jwkSet.getJsonWebKeys()) {
                if ((RSA_KEY_TYPE.equals(jwkKey.getKeyType()) || EC_KEY_TYPE.equals(jwkKey.getKeyType())
                        || jwkKey.getKeyType() == null)
                        && (SIGNATURE_USE.equals(jwkKey.getUse()) || jwkKey.getUse() == null)
                        && jwkKey.getKeyId() != null) {
                    keys.put(jwkKey.getKeyId(), jwkKey.getKey());
                }
            }
        } catch (JoseException ex) {
            throw new OIDCException(ex);
        }
    }

    public Key getKey(String kid) {
        return keys.get(kid);
    }

    Map<String, Key> getKeys() {
        return Collections.unmodifiableMap(keys);
    }
}
