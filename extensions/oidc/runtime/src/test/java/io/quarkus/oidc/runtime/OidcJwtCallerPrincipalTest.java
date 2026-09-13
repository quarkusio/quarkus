package io.quarkus.oidc.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import io.smallrye.jwt.common.JwtClaims;

public class OidcJwtCallerPrincipalTest {

    @Test
    public void testGroupsFromStringClaim() {
        OidcJwtCallerPrincipal principal = principal("{\"groups\": \"admin user\"}");
        assertEquals(Set.of("admin", "user"), principal.getGroups());
    }

    @Test
    public void testGroupsFromStringClaimWithExtraWhitespace() {
        OidcJwtCallerPrincipal principal = principal("{\"groups\": \" admin  user \"}");
        assertEquals(Set.of("admin", "user"), principal.getGroups());
    }

    @Test
    public void testGroupsFromBlankStringClaim() {
        OidcJwtCallerPrincipal principal = principal("{\"groups\": \" \"}");
        assertTrue(principal.getGroups().isEmpty());
    }

    @Test
    public void testGroupsFromArrayClaim() {
        OidcJwtCallerPrincipal principal = principal("{\"groups\": [\"admin\", \"user\"]}");
        assertEquals(Set.of("admin", "user"), principal.getGroups());
    }

    @Test
    public void testMissingGroupsClaim() {
        OidcJwtCallerPrincipal principal = principal("{\"sub\": \"alice\"}");
        assertTrue(principal.getGroups().isEmpty());
    }

    @Test
    public void testGroupsClaimValueMatchesGetGroups() {
        OidcJwtCallerPrincipal principal = principal("{\"groups\": \"admin user\"}");
        assertEquals(principal.getGroups(), principal.getClaim("groups"));
    }

    private static OidcJwtCallerPrincipal principal(String json) {
        return new OidcJwtCallerPrincipal(JwtClaims.parse(json), null);
    }
}
