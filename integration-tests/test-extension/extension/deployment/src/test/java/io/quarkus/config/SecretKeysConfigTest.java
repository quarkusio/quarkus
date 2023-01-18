package io.quarkus.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.SmallRyeConfig;

public class SecretKeysConfigTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(SecretKeysConfigInterceptorFactory.class)
                    .addAsServiceProvider("io.smallrye.config.ConfigSourceInterceptorFactory",
                            SecretKeysConfigInterceptorFactory.class.getName()))
            .overrideConfigKey("secrets.my.secret", "secret");

    @Inject
    SmallRyeConfig config;
    @Inject
    @ConfigProperty(name = "secrets.my.secret")
    String secret;
    @Inject
    MappingSecret mappingSecret;

    @Test
    void secrets() {
        assertThrows(SecurityException.class, () -> config.getConfigValue("secrets.my.secret"));

        assertEquals("secret", secret);
        assertEquals("secret", mappingSecret.secret());
    }

    @ConfigMapping(prefix = "secrets.my")
    interface MappingSecret {
        String secret();
    }
}
