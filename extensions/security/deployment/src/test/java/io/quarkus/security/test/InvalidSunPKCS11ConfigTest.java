package io.quarkus.security.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

public class InvalidSunPKCS11ConfigTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .assertException(throwable -> {
                Assertions.assertTrue(throwable instanceof ConfigurationException);
                Assertions.assertTrue(throwable.getMessage().contains("SunPKCS11"));
            })
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-invalid-sunpkcs11-config.properties", "application.properties"));

    @Test
    void shouldThrow() {
        Assertions.fail("A ConfigurationException should have been thrown due to invalid SunPKCS11 configuration");
    }
}
