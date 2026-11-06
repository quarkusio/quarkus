package io.quarkus.spring.boot.properties.deployment;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.context.properties.ConfigurationProperties;

import io.quarkus.arc.Unremovable;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.config.ConfigValidationException;

class ConfigurationPropertiesIgnoreUnknownFieldsTest {
    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset(
                            """
                                    strict.host=localhost
                                    strict.unmapped=value
                                    """), "application.properties"))
            .assertException(t -> {
                for (Throwable cause = t; cause != null; cause = cause.getCause()) {
                    // the exception is loaded by the runtime classloader, so it cannot be matched with instanceof
                    if (cause.getClass().getName().equals(ConfigValidationException.class.getName())) {
                        assertTrue(cause.getMessage().contains("strict.unmapped"), cause.getMessage());
                        return;
                    }
                }
                fail("Expected a ConfigValidationException for strict.unmapped", t);
            });

    @Test
    void unknownPropertyFailsStartup() {
        fail("Startup should have failed");
    }

    // not injected anywhere, so it must be unremovable to be registered with the config
    @Unremovable
    @ConfigurationProperties(prefix = "strict", ignoreUnknownFields = false)
    public static class StrictProperties {
        public String host;
    }
}
