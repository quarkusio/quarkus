package io.quarkus.test.security;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TestJwtClaims {

    private Map<String, String> claims = new HashMap<>();

    public String getClaim(String claim) {
        return claims.get(claim);
    }

    public void setClaim(String claim, String value) {
        claims.put(claim, value);
    }

    public Collection<String> getClaimNames() {
        return claims.keySet();
    }
}
