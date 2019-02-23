package io.quarkus.smallrye.jwt.runtime.auth;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jose4j.jwt.JwtClaims;
import org.wildfly.security.authz.Attributes;
import org.wildfly.security.authz.SimpleAttributesEntry;

/**
 * An implementation of Elytron attributes interface that builds on the jose4j {@linkplain JwtClaims}.
 *
 */
public class ClaimAttributes implements Attributes {
    private final JwtClaims claimsSet;
    private HashMap<String, Entry> entries = new HashMap<>();

    public ClaimAttributes(JwtClaims claimsSet) {
        this.claimsSet = claimsSet;
        populateEntries();
    }

    @Override
    public Collection<Entry> entries() {
        return entries.values();
    }

    @Override
    public int size(String key) {
        int size = 0;
        try {
            Object objectValue = claimsSet.getClaimValue(key);
            if (objectValue instanceof List) {
                size = ((List) objectValue).size();
            } else {
                size = 1;
            }
        } catch (Exception e) {

        }

        return size;
    }

    @Override
    public Entry get(String key) {
        return entries.get(key);
    }

    @Override
    public String get(String key, int idx) {
        String value = null;
        try {
            if (claimsSet.isClaimValueStringList(key)) {
                List<String> values = claimsSet.getStringListClaimValue(key);
                value = values.get(idx);
            } else if (claimsSet.isClaimValueString(key) && idx == 0) {
                value = claimsSet.getClaimValue(key, String.class);
            } else {
                Object objectValue = claimsSet.getClaimValue(key);
                if (objectValue instanceof List) {
                    value = ((List) objectValue).get(idx).toString();
                } else {
                    value = objectValue.toString();
                }
            }
        } catch (Exception e) {

        }
        return value;
    }

    @Override
    public int size() {
        return entries.size();
    }

    public JwtClaims getClaimsSet() {
        return claimsSet;
    }

    /**
     *
     */
    private void populateEntries() {
        Map<String, List<Object>> claims = claimsSet.flattenClaims();
        for (Map.Entry<String, List<Object>> entry : claims.entrySet()) {
            String key = entry.getKey();
            SimpleAttributesEntry attributesEntry = new SimpleAttributesEntry(this, key);
            entries.put(key, attributesEntry);
        }
    }
}
