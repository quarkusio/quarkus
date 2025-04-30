package io.quarkus.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;

public class RecordedBuildProfileInRuntimeTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .withConfigurationResource("application.properties");

    @Inject
    SmallRyeConfig config;

    @Test
    void recordedBuildProfileInRuntime() {
        ConfigValue profile = config.getConfigValue("quarkus.test.profile");
        assertEquals("test", profile.getValue());
        // Recorded by ProfileBuildStep
        assertEquals("DefaultValuesConfigSource", profile.getConfigSourceName());

        Optional<ConfigSource> defaultValuesConfigSource = config.getConfigSource("DefaultValuesConfigSource");
        assertTrue(defaultValuesConfigSource.isPresent());
        // The default set in ConfigUtils, based on the LaunchMode
        assertEquals("test", defaultValuesConfigSource.get().getValue("quarkus.test.profile"));
    }
}
