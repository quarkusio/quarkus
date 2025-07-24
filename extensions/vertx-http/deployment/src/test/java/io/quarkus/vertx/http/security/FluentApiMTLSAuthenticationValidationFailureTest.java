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

public class FluentApiMTLSAuthenticationValidationFailureTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClass(AuthMechanismConfig.class)
            .addAsResource(new StringAsset("""
                    quarkus.http.ssl.client-auth=REQUEST
                    """), "application.properties"))
            .assertException(throwable -> {
                // Mutual TLS is required due to configuration in the fluent API
                // but the 'quarkus.http.ssl.client-auth' has already been set (to any value)
                // and we currently do not allow to combine configuration in the 'application.properties' file
                // and fluent API, therefore expect validation failure
                assertThat(throwable).hasMessageContaining("TLS client authentication has already been enabled");
            });

    @Test
    public void runTest() {
        Assertions.fail("This test should not run");
    }

    public static class AuthMechanismConfig {

        void configure(@Observes HttpSecurity httpSecurity) {
            httpSecurity.mTLS();
        }

    }

}
