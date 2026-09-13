package io.quarkus.elytron.security.ldap;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Checks that a TLS configuration name is accepted only with {@code ldaps} URLs.
 */
public class LdapsUrlCheckTest {

    @Test
    public void testLdapsUrlsAreAccepted() {
        assertThatCode(() -> LdapRecorder.checkLdapsUrls("ldaps://one.local:636 LDAPS://two.local", "ldap"))
                .doesNotThrowAnyException();
    }

    @Test
    public void testPlainLdapUrlIsRejected() {
        assertThatThrownBy(() -> LdapRecorder.checkLdapsUrls("ldaps://one.local ldap://two.local:389", "ldap"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'ldap://two.local:389'")
                .hasMessageContaining("'ldaps://'");
    }
}
