package io.quarkus.csrf.reactive;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.security.CSRF;

public class ProgrammaticCsrfValidationFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withEmptyApplication();

    @Test
    public void testQuarkusSecurityExtensionRequired() {
        var exception = assertThrows(IllegalStateException.class, CSRF::builder);
        assertTrue(exception.getMessage().contains("Please add the `quarkus-security` extension"));
    }

}
