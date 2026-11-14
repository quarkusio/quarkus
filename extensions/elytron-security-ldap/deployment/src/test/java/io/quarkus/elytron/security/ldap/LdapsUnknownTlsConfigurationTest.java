package io.quarkus.elytron.security.ldap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * The application fails to start when the TLS configuration name does not match a TLS configuration.
 */
public class LdapsUnknownTlsConfigurationTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("""
                            quarkus.security.ldap.enabled=true
                            quarkus.security.ldap.identity-mapping.search-base-dn=ou=Users,dc=quarkus,dc=io
                            quarkus.security.ldap.dir-context.tls-configuration-name=missing
                            """), "application.properties"))
            .overrideRuntimeConfigKey("quarkus.security.ldap.dir-context.url", "ldaps://localhost:636")
            .assertException(t -> assertThat(t).hasStackTraceContaining("Unable to find the TLS configuration 'missing'")
                    .hasStackTraceContaining("quarkus.tls.missing.*"));

    @Test
    public void shouldNotBeCalled() {
        fail("The application should fail to start");
    }
}
