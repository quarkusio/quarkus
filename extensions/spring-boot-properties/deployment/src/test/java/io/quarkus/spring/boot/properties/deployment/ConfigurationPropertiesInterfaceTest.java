package io.quarkus.spring.boot.properties.deployment;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.context.properties.ConfigurationProperties;

import io.quarkus.test.QuarkusExtensionTest;

class ConfigurationPropertiesInterfaceTest {
    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .assertException(t -> {
                for (Throwable cause = t; cause != null; cause = cause.getCause()) {
                    if (cause instanceof IllegalArgumentException
                            && cause.getMessage().contains(InterfaceProperties.class.getName())) {
                        assertTrue(cause.getMessage().contains("only classes are supported"), cause.getMessage());
                        return;
                    }
                }
                fail("Expected the build to reject the interface", t);
            });

    @Test
    void interfaceIsRejected() {
        fail("Build should have failed");
    }

    @ConfigurationProperties(prefix = "iface")
    public interface InterfaceProperties {
        String getValue();
    }
}
