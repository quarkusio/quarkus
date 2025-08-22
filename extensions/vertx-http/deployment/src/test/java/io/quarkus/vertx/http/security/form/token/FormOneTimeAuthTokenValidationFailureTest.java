package io.quarkus.vertx.http.security.form.token;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class FormOneTimeAuthTokenValidationFailureTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addAsResource(new StringAsset("""
                    quarkus.http.auth.form.enabled=true
                    quarkus.http.auth.form.authentication-token.enabled=true
                    """),
                    "application.properties"))
            .assertException(t -> {
                String exceptionMessage = t.getMessage();
                Assertions.assertTrue(exceptionMessage.contains("One-time authentication token feature is enabled"));
                Assertions.assertTrue(exceptionMessage.contains(
                        "no 'io.quarkus.vertx.http.security.token.OneTimeAuthenticationTokenSender' interface has been found"));
            });

    @Test
    public void test() {
        // must be here to run validation
        Assertions.fail("Expected startup failure did not occur");
    }

}
