package io.quarkus.security.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.Permission;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.security.credential.Credential;
import io.quarkus.security.credential.PasswordCredential;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;

public class QuarkusSecurityIdentityTest {

    @Test
    public void testCopyIdentity() throws Exception {
        SecurityIdentity identity1 = QuarkusSecurityIdentity.builder()
                .setPrincipal(new QuarkusPrincipal("alice"))
                .addRole("admin")
                .addCredential(new PasswordCredential("password".toCharArray()))
                .addAttribute("key", "value")
                .build();

        assertFalse(identity1.isAnonymous());

        SecurityIdentity identity2 = QuarkusSecurityIdentity.builder(identity1).build();
        assertFalse(identity1.isAnonymous());

        assertEquals(identity1.getAttributes(), identity2.getAttributes());
        assertEquals(identity1.getPrincipal(), identity2.getPrincipal());
        assertEquals(identity1.getCredentials(), identity2.getCredentials());
        assertEquals(identity1.getRoles(), identity2.getRoles());
    }

    @Test
    public void testAnonymousPrincipalWithCustomIdentity() throws Exception {
        SecurityIdentity identity1 = new TestSecurityIdentityAnonymousPrincipal();
        assertTrue(identity1.isAnonymous());
        assertEquals("anonymous-principal", identity1.getPrincipal().getName());

        SecurityIdentity identity2 = QuarkusSecurityIdentity.builder(identity1).build();
        assertTrue(identity2.isAnonymous());
        assertEquals("anonymous-principal", identity2.getPrincipal().getName());
    }

    @Test
    public void testPrincipalNullAnonymousFalseWithBuilder() throws Exception {
        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder()
                .addRole("admin")
                .addCredential(new PasswordCredential("password".toCharArray()))
                .addAttribute("key", "value");
        ;

        assertThrows(IllegalStateException.class, () -> builder.build());
    }

    @Test
    public void testPrincipalNullAnonymousFalseWithCustomIdentity() throws Exception {
        SecurityIdentity identity1 = new TestSecurityIdentityPrincipalNullAnonymousFalse();
        assertFalse(identity1.isAnonymous());
        assertNull(identity1.getPrincipal());

        assertThrows(IllegalStateException.class, () -> QuarkusSecurityIdentity.builder(identity1).build());
    }

    @Test
    public void testPrincipalNullAnonymousFalseWithCustomIdentityFixed() throws Exception {
        SecurityIdentity identity1 = new TestSecurityIdentityPrincipalNullAnonymousFalse();
        assertFalse(identity1.isAnonymous());
        assertNull(identity1.getPrincipal());

        SecurityIdentity identity2 = QuarkusSecurityIdentity.builder(identity1).setAnonymous(true).build();
        assertTrue(identity2.isAnonymous());
        assertNull(identity2.getPrincipal());
    }

    static class TestSecurityIdentityAnonymousPrincipal extends AbstractTestSecurityIdentity {

        @Override
        public Principal getPrincipal() {
            return new Principal() {
                @Override
                public String getName() {
                    return "anonymous-principal";
                }
            };
        }

        @Override
        public boolean isAnonymous() {
            return true;
        }

    }

    static class TestSecurityIdentityPrincipalNullAnonymousFalse extends AbstractTestSecurityIdentity {

        @Override
        public Principal getPrincipal() {
            return null;
        }

        @Override
        public boolean isAnonymous() {
            return false;
        }

    }

    static abstract class AbstractTestSecurityIdentity implements SecurityIdentity {

        @Override
        public Set<String> getRoles() {
            return Collections.emptySet();
        }

        @Override
        public boolean hasRole(String role) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public <T extends Credential> T getCredential(Class<T> credentialType) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Set<Credential> getCredentials() {
            return Collections.emptySet();
        }

        @Override
        public <T> T getAttribute(String name) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return Collections.emptyMap();
        }

        @Override
        public Uni<Boolean> checkPermission(Permission permission) {
            // TODO Auto-generated method stub
            return null;
        }

    }
}
