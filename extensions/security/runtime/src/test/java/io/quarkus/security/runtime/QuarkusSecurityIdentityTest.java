package io.quarkus.security.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.security.credential.PasswordCredential;
import io.quarkus.security.identity.SecurityIdentity;

public class QuarkusSecurityIdentityTest {

    @Test
    public void testCopyIdentity() throws Exception {
        SecurityIdentity identity1 = QuarkusSecurityIdentity.builder()
                .setPrincipal(new QuarkusPrincipal("alice"))
                .addRole("admin")
                .addCredential(new PasswordCredential("password".toCharArray()))
                .addAttribute("key", "value")
                .build();

        SecurityIdentity identity2 = QuarkusSecurityIdentity.builder(identity1).build();

        assertEquals(identity1.getAttributes(), identity2.getAttributes());
        assertEquals(identity1.getPrincipal(), identity2.getPrincipal());
        assertEquals(identity1.getCredentials(), identity2.getCredentials());
        assertEquals(identity1.getRoles(), identity2.getRoles());
    }
}
