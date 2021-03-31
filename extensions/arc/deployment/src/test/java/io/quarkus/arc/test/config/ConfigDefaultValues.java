package io.quarkus.arc.test.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ConfigDefaultValues {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset("config_ordinal=1000\n" +
                            "my.prop=1234\n"), "application.properties"));
    @Inject
    Config config;

    @Test
    void configDefaultValues() {
        ConfigSource defaultValues = getConfigSourceByName("PropertiesConfigSource[source=Specified default values]");
        assertNotNull(defaultValues);
        assertEquals(Integer.MIN_VALUE + 100, defaultValues.getOrdinal());

        // Should be the first
        ConfigSource applicationProperties = config.getConfigSources().iterator().next();
        assertNotNull(applicationProperties);
        assertEquals(1000, applicationProperties.getOrdinal());

        assertEquals("1234", defaultValues.getValue("my.prop"));
        assertEquals("1234", applicationProperties.getValue("my.prop"));
    }

    private ConfigSource getConfigSourceByName(String name) {
        for (ConfigSource configSource : config.getConfigSources()) {
            if (configSource.getName().contains(name)) {
                return configSource;
            }
        }
        return null;
    }
}
