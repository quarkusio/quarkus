package io.quarkus.test.security.oidc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.security.Principal;
import java.util.Optional;
import java.util.Set;

import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;

public class OidcTestSecurityIdentityAugmentorTest {

    @Test
    @OidcSecurity(claims = {
            @Claim(key = "exp", value = "123456789"),
            @Claim(key = "iat", value = "123456788"),
            @Claim(key = "nbf", value = "123456787"),
            @Claim(key = "auth_time", value = "123456786"),
            @Claim(key = "customlong", value = "123456785", type = ClaimType.LONG),
            @Claim(key = "email", value = "user@gmail.com"),
            @Claim(key = "email_verified", value = "true"),
            @Claim(key = "email_checked", value = "false", type = ClaimType.BOOLEAN),
            @Claim(key = "jsonarray_claim", value = "[\"1\", \"2\"]", type = ClaimType.JSON_ARRAY),
            @Claim(key = "jsonobject_claim", value = "{\"a\":\"1\", \"b\":\"2\"}", type = ClaimType.JSON_OBJECT)
    })
    public void testClaimValues() throws Exception {
        SecurityIdentity identity = QuarkusSecurityIdentity.builder()
                .setPrincipal(new Principal() {
                    @Override
                    public String getName() {
                        return "alice";
                    }

                })
                .addRole("user")
                .build();

        OidcTestSecurityIdentityAugmentor augmentor = new OidcTestSecurityIdentityAugmentor(Optional.of("https://issuer.org"));

        Annotation[] annotations = OidcTestSecurityIdentityAugmentorTest.class.getMethod("testClaimValues").getAnnotations();
        JsonWebToken jwt = (JsonWebToken) augmentor.augment(identity, annotations).getPrincipal();

        assertEquals("alice", jwt.getName());
        assertEquals(Set.of("user"), jwt.getGroups());

        assertEquals(123456789, jwt.getExpirationTime());
        assertEquals(123456788, jwt.getIssuedAtTime());
        assertEquals(123456787, (Long) jwt.getClaim(Claims.nbf.name()));
        assertEquals(123456786, (Long) jwt.getClaim(Claims.auth_time.name()));
        assertEquals(123456785, ((JsonNumber) jwt.getClaim("customlong")).longValue());
        assertEquals("user@gmail.com", jwt.getClaim(Claims.email));
        assertTrue((Boolean) jwt.getClaim(Claims.email_verified.name()));
        assertEquals(JsonValue.FALSE, jwt.getClaim("email_checked"));

        JsonArray array = jwt.getClaim("jsonarray_claim");
        assertEquals("1", array.getString(0));
        assertEquals("2", array.getString(1));

        JsonObject map = jwt.getClaim("jsonobject_claim");
        assertEquals("1", map.getString("a"));
        assertEquals("2", map.getString("b"));
    }

    @Test
    @OidcSecurity(userinfo = {
            @UserInfo(key = "sub", value = "subject")
    })
    public void testUserInfo() throws Exception {
        SecurityIdentity identity = QuarkusSecurityIdentity.builder()
                .setPrincipal(new Principal() {
                    @Override
                    public String getName() {
                        return "alice";
                    }

                })
                .build();

        OidcTestSecurityIdentityAugmentor augmentor = new OidcTestSecurityIdentityAugmentor(Optional.of("https://issuer.org"));

        Annotation[] annotations = OidcTestSecurityIdentityAugmentorTest.class.getMethod("testUserInfo").getAnnotations();
        SecurityIdentity augmentedIdentity = augmentor.augment(identity, annotations);
        JsonWebToken jwt = (JsonWebToken) augmentedIdentity.getPrincipal();
        assertEquals("alice", jwt.getName());

        io.quarkus.oidc.UserInfo userInfo = augmentedIdentity.getAttribute("userinfo");
        assertNotNull(userInfo);
        assertEquals("subject", userInfo.getSubject());
    }

}
