package io.quarkus.vertx.http.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.event.Observes;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.tls.BaseTlsConfiguration;

public class FluentApiTlsConfigValidationFailureTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClass(AuthMechanismConfig.class)
            .addAsResource(new StringAsset("""
                    quarkus.http.tls-configuration-name=your-tls-config
                    """), "application.properties"))
            .assertException(throwable -> {
                // TLS configuration name is set up with the fluent API
                // but the 'quarkus.http.tls-configuration-name' has already been set,
                // and we currently do not allow to combine configuration in the 'application.properties' file
                // and fluent API, therefore expect validation failure
                assertThat(throwable)
                        .hasMessageContaining("Cannot configure TLS configuration name programmatically")
                        .hasMessageContaining("quarkus.http.tls-configuration-name");
            });

    @Test
    public void runTest() {
        Assertions.fail("This test should not run");
    }

    public static class AuthMechanismConfig {

        void configure(@Observes HttpSecurity httpSecurity) {
            httpSecurity.mTLS("my-tls-config", new BaseTlsConfiguration() {
            });
        }

    }

}
