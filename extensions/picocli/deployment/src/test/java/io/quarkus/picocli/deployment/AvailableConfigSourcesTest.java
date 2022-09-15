package io.quarkus.picocli.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;

public class AvailableConfigSourcesTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withApplicationRoot(
            (jar) -> jar.addAsResource(new StringAsset("quarkus.config.sources.system-only=true\n" +
                    "my.prop=1234\n"), "application.properties"));

    @Inject
    SmallRyeConfig config;

    @Test
    void sources() {
        ConfigValue value = config.getConfigValue("my.prop");
        assertEquals("1234", value.getValue());
        assertEquals("RunTime Defaults", value.getConfigSourceName());

        for (final ConfigSource configSource : config.getConfigSources()) {
            if (configSource.getName().contains("application.properties")) {
                Assertions.fail();
            }
        }
    }
}
