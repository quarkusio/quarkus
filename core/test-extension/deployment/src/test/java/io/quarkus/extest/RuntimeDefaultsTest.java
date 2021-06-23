package io.quarkus.extest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class RuntimeDefaultsTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    // Don't change this to types, because of classloader class cast exception.
                    .addAsServiceProvider("org.eclipse.microprofile.config.spi.ConfigSource",
                            "io.quarkus.extest.runtime.config.EnvBuildTimeConfigSource")
                    .addAsResource("application.properties"));

    @Inject
    Config config;

    @Test
    void doNotRecordEnvRuntimeDefaults() {
        ConfigSource defaultValues = null;
        for (ConfigSource configSource : config.getConfigSources()) {
            if (configSource.getName().contains("PropertiesConfigSource[source=Specified default values]")) {
                defaultValues = configSource;
                break;
            }
        }
        assertNotNull(defaultValues);

        assertEquals("properties", defaultValues.getValue("bt.do.not.record"));
    }
}
