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

public class FluentApiBasicAuthValidationFailureTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
            .addAsResource(new StringAsset("quarkus.http.auth.realm=whatever"), "application.properties")
            .addClasses(TestIdentityProvider.class, TestTrustedIdentityProvider.class, TestIdentityController.class,
                    HttpSecurityConfigurator.class))
            // forbid programmatic setup when the authentication realm (which is the only basic auth property) is
            // already configured in the application.properties
            .setExpectedException(IllegalArgumentException.class);

    @Test
    void runValidation() {
        Assertions.fail("This code should be unreachable");
    }

    public static class HttpSecurityConfigurator {

        void configureFormAuthentication(@Observes HttpSecurity httpSecurity) {
            httpSecurity.basic("OtherRealm");
        }
    }
}
