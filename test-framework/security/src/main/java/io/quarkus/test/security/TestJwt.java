package io.quarkus.test.security;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.jwt.JsonWebToken;

public class TestJwt implements JsonWebToken {

    private final String name;
    private final TestJwtClaims claims;

    public TestJwt(String name, TestJwtClaims claims) {
        this.name = name;
        this.claims = claims;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<String> getClaimNames() {
        Set<String> claimNames = new HashSet(claims.getClaimNames());
        return claimNames;
    }

    @Override
    public String getClaim(String s) {
        return claims.getClaim(s);
    }
}
