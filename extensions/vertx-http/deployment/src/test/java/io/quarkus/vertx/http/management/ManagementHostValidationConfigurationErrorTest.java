package io.quarkus.vertx.http.management;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

public class ManagementHostValidationConfigurationErrorTest {

    private static final String configuration = "quarkus.management.enabled=true\n"
            + "quarkus.management.host-validation.allowed-hosts=localhost,127.0.0.1\n"
            + "quarkus.management.host-validation.require-localhost=true\n";

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(configuration), "application.properties"))
            .assertException(t -> {
                Assertions.assertInstanceOf(ConfigurationException.class, t);
                Assertions.assertTrue(t.getMessage().contains("mutually exclusive properties"),
                        "Exception message should mention 'mutually exclusive properties', got: " + t.getMessage());
            });

    @Test
    public void test() {
        Assertions.fail();
    }
}
