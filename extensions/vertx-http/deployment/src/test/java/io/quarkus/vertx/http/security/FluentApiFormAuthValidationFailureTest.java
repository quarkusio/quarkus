package io.quarkus.vertx.http.security;

import jakarta.enterprise.event.Observes;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;

public class FluentApiFormAuthValidationFailureTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
            .addAsResource(new StringAsset("quarkus.http.auth.form.landing-page=landing"), "application.properties")
            .addClasses(TestIdentityProvider.class, TestTrustedIdentityProvider.class, TestIdentityController.class,
                    HttpSecurityConfigurator.class))
            // forbid programmatic setup when at least one form config property was set
            .setExpectedException(IllegalArgumentException.class);

    @Test
    void runValidation() {
        Assertions.fail("This code should be unreachable");
    }

    public static class HttpSecurityConfigurator {

        void configureFormAuthentication(@Observes HttpSecurity httpSecurity) {
            HttpAuthenticationMechanism formMechanism = Form.builder().loginPage("login").build();
            httpSecurity.mechanism(formMechanism);
        }
    }
}
