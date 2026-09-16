package io.quarkus.amazon.lambda.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.amazon.lambda.http.model.CognitoAuthorizerClaims;

public class CognitoPrincipalTest {

    private static CognitoPrincipal principal(String exp, String iat) {
        CognitoAuthorizerClaims claims = new CognitoAuthorizerClaims();
        claims.setUsername("bill");
        claims.setAudience("client-id");
        claims.setExpiration(exp);
        claims.setIssuedAt(iat);
        return new CognitoPrincipal(claims);
    }

    @Test
    public void expirationAndIssuedAtAreReadFromTheClaims() {
        CognitoPrincipal principal = principal("1700000000", "1690000000");

        assertEquals("bill", principal.getName());
        assertEquals(1700000000L, principal.getExpirationTime());
        assertEquals(1690000000L, principal.getIssuedAtTime());
        assertTrue(principal.getClaimNames().contains("exp"));
    }

    @Test
    public void standardClaimsAreReturnedWithTheirDocumentedTypes() {
        CognitoPrincipal principal = principal("1700000000", "1690000000");

        Object exp = principal.getClaim("exp");
        assertEquals(Long.valueOf(1700000000L), exp);
        Object iat = principal.getClaim("iat");
        assertEquals(Long.valueOf(1690000000L), iat);
        Object aud = principal.getClaim("aud");
        assertEquals(principal.getAudience(), aud);
    }

    @Test
    public void missingTimeClaimsAreZero() {
        CognitoPrincipal principal = principal(null, null);

        assertEquals(0L, principal.getExpirationTime());
        assertEquals(0L, principal.getIssuedAtTime());
    }
}
