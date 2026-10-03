package io.quarkus.oidc.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.junit.jupiter.api.Test;

public class OidcJwtCallerPrincipalTest {

    @Test
    public void testGroupsFromStringClaim() throws InvalidJwtException {
        OidcJwtCallerPrincipal principal = principal("{\"groups\": \"admin user\"}");
        assertEquals(Set.of("admin", "user"), principal.getGroups());
    }

    @Test
    public void testGroupsFromStringClaimWithExtraWhitespace() throws InvalidJwtException {
        OidcJwtCallerPrincipal principal = principal("{\"groups\": \" admin  user \"}");
        assertEquals(Set.of("admin", "user"), principal.getGroups());
    }

    @Test
    public void testGroupsFromBlankStringClaim() throws InvalidJwtException {
        OidcJwtCallerPrincipal principal = principal("{\"groups\": \" \"}");
        assertTrue(principal.getGroups().isEmpty());
    }

    @Test
    public void testGroupsFromArrayClaim() throws InvalidJwtException {
        OidcJwtCallerPrincipal principal = principal("{\"groups\": [\"admin\", \"user\"]}");
        assertEquals(Set.of("admin", "user"), principal.getGroups());
    }

    @Test
    public void testMissingGroupsClaim() throws InvalidJwtException {
        OidcJwtCallerPrincipal principal = principal("{\"sub\": \"alice\"}");
        assertTrue(principal.getGroups().isEmpty());
    }

    @Test
    public void testGroupsClaimValueMatchesGetGroups() throws InvalidJwtException {
        OidcJwtCallerPrincipal principal = principal("{\"groups\": \"admin user\"}");
        assertEquals(principal.getGroups(), principal.getClaim("groups"));
    }

    private static OidcJwtCallerPrincipal principal(String json) throws InvalidJwtException {
        return new OidcJwtCallerPrincipal(JwtClaims.parse(json), null);
    }
}
