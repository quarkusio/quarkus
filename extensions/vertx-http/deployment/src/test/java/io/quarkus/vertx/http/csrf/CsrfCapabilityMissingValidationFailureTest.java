package io.quarkus.vertx.http.csrf;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.event.Observes;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.security.CSRF;
import io.quarkus.vertx.http.security.HttpSecurity;

public class CsrfCapabilityMissingValidationFailureTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class).addClass(CsrfConfig.class))
            .assertException(throwable -> assertThat(throwable)
                    .hasMessageContaining("Please add an extension that provides a CSRF prevention feature"));

    @Test
    public void runTest() {
        Assertions.fail("This test should not run");
    }

    public static class CsrfConfig {

        void configure(@Observes HttpSecurity httpSecurity) {
            // if an extension that supports the CSRF is not present, expect failure:
            CSRF.builder();
        }

    }

}
