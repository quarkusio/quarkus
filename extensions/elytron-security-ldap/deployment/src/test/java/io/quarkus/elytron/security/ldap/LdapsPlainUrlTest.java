package io.quarkus.elytron.security.ldap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * The application fails to start when a TLS configuration name is set with a plain {@code ldap://} URL.
 */
public class LdapsPlainUrlTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("""
                            quarkus.security.ldap.enabled=true
                            quarkus.security.ldap.identity-mapping.search-base-dn=ou=Users,dc=quarkus,dc=io
                            quarkus.security.ldap.dir-context.tls-configuration-name=ldap
                            quarkus.tls.ldap.trust-all=true
                            """), "application.properties"))
            .overrideRuntimeConfigKey("quarkus.security.ldap.dir-context.url", "ldap://localhost:389")
            .assertException(t -> assertThat(t).hasStackTraceContaining("'ldap://localhost:389'")
                    .hasStackTraceContaining("does not use the 'ldaps' scheme"));

    @Test
    public void shouldNotBeCalled() {
        fail("The application should fail to start");
    }
}
