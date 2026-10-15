package io.quarkus.amazon.lambda.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;

public class CognitoPrincipalTest {

    private static CognitoPrincipal principal(Map<String, String> claims) {
        APIGatewayV2HTTPEvent.RequestContext.Authorizer.JWT jwt = new APIGatewayV2HTTPEvent.RequestContext.Authorizer.JWT();
        jwt.setClaims(claims);
        return new CognitoPrincipal(jwt);
    }

    @Test
    public void expirationAndIssuedAtAreReadFromTheClaims() {
        CognitoPrincipal principal = principal(Map.of("cognito:username", "bill", "exp", "1700000000",
                "iat", "1690000000"));

        assertEquals("bill", principal.getName());
        assertEquals(1700000000L, principal.getExpirationTime());
        assertEquals(1690000000L, principal.getIssuedAtTime());
        assertTrue(principal.getClaimNames().contains("exp"));
    }

    @Test
    public void standardClaimsAreReturnedWithTheirDocumentedTypes() {
        CognitoPrincipal principal = principal(Map.of("cognito:username", "bill", "exp", "1700000000",
                "iat", "1690000000"));

        Object exp = principal.getClaim("exp");
        assertEquals(Long.valueOf(1700000000L), exp);
        Object iat = principal.getClaim("iat");
        assertEquals(Long.valueOf(1690000000L), iat);
        Object aud = principal.getClaim("aud");
        assertEquals(principal.getAudience(), aud);
    }

    @Test
    public void missingTimeClaimsAreZero() {
        CognitoPrincipal principal = principal(Map.of("cognito:username", "bill"));

        assertEquals(0L, principal.getExpirationTime());
        assertEquals(0L, principal.getIssuedAtTime());
    }
}
