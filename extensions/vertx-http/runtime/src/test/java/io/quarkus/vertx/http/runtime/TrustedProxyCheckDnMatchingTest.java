package io.quarkus.vertx.http.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;

import org.junit.jupiter.api.Test;

class TrustedProxyCheckDnMatchingTest {

    @Test
    void cnOnlyMatchesCnOnly() {
        assertThat(matches("CN=proxy", "CN=proxy")).isTrue();
    }

    @Test
    void cnOnlyMatchesFullDn() {
        assertThat(matches("CN=proxy,O=MyOrg,C=US", "CN=proxy")).isTrue();
    }

    @Test
    void cnAndOrgMatchesFullDn() {
        assertThat(matches("CN=proxy,O=MyOrg,C=US", "CN=proxy,O=MyOrg")).isTrue();
    }

    @Test
    void fullDnMatchesFullDn() {
        assertThat(matches("CN=proxy,O=MyOrg,C=US", "CN=proxy,O=MyOrg,C=US")).isTrue();
    }

    @Test
    void wrongCnDoesNotMatch() {
        assertThat(matches("CN=proxy,O=MyOrg", "CN=other")).isFalse();
    }

    @Test
    void wrongOrgDoesNotMatch() {
        assertThat(matches("CN=proxy,O=MyOrg", "CN=proxy,O=OtherOrg")).isFalse();
    }

    @Test
    void extraRequiredComponentDoesNotMatch() {
        assertThat(matches("CN=proxy", "CN=proxy,O=MyOrg")).isFalse();
    }

    @Test
    void matchingIsCaseInsensitive() {
        assertThat(matches("CN=Proxy,O=MyOrg", "cn=proxy")).isTrue();
    }

    @Test
    void matchesRegardlessOfRdnOrder() {
        assertThat(matches("CN=proxy,O=MyOrg,C=US", "O=MyOrg,C=US,CN=proxy")).isTrue();
    }

    @Test
    void matchesSecondTrustedDn() {
        assertThat(TrustedProxyCheck.matchesAnyTrustedDn(
                new X500Principal("CN=proxy"),
                List.of(toRdns("CN=other"), toRdns("CN=proxy")))).isTrue();
    }

    @Test
    void matchesNoneTrustedDn() {
        assertThat(TrustedProxyCheck.matchesAnyTrustedDn(
                new X500Principal("CN=proxy"),
                List.of(toRdns("CN=other"), toRdns("CN=another")))).isFalse();
    }

    private static boolean matches(String certDn, String requiredDn) {
        return TrustedProxyCheck.matchesAnyTrustedDn(
                new X500Principal(certDn),
                List.of(toRdns(requiredDn)));
    }

    private static List<Rdn> toRdns(String dn) {
        try {
            return new LdapName(dn).getRdns();
        } catch (InvalidNameException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
