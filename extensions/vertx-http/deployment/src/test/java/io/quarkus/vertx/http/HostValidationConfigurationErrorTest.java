package io.quarkus.vertx.http;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class HostValidationConfigurationErrorTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("""
                            quarkus.http.host-validation.allowed-hosts=localhost,127.0.0.1
                            quarkus.http.host-validation.require-localhost=true
                            """), "application.properties"))
            .assertException(t -> {
                Assertions.assertTrue(t instanceof ConfigurationException,
                        "Expected ConfigurationException but got: " + t.getClass().getName());
                Assertions.assertTrue(t.getMessage().contains("mutually exclusive properties"),
                        "Exception message should mention 'mutually exclusive properties', got: " + t.getMessage());
            });

    @Test
    public void test() {
        Assertions.fail();
    }
}
