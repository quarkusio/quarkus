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

public class ConfigDefaultValuesTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset(
                            "config_ordinal=1000\n" +
                                    "my.prop=1234\n" +
                                    "%prod.my.prop=1234\n" +
                                    "%dev.my.prop=5678\n" +
                                    "%test.my.prop=1234"),
                            "application.properties"));
    @Inject
    Config config;

    @Test
    void configDefaultValues() {
        ConfigSource defaultValues = getConfigSourceByName("PropertiesConfigSource[source=Specified default values]");
        assertNotNull(defaultValues);
        assertEquals(Integer.MIN_VALUE + 100, defaultValues.getOrdinal());

        ConfigSource applicationProperties = getConfigSourceByName("PropertiesConfigSource[source=application.properties]");
        assertNotNull(applicationProperties);
        assertEquals(1000, applicationProperties.getOrdinal());

        assertEquals("1234", defaultValues.getValue("my.prop"));
        assertEquals("1234", applicationProperties.getValue("my.prop"));
    }

    @Test
    void profileDefaultValues() {
        ConfigSource defaultValues = getConfigSourceByName("PropertiesConfigSource[source=Specified default values]");
        assertNotNull(defaultValues);
        assertEquals("1234", defaultValues.getValue("my.prop"));
        assertEquals("1234", defaultValues.getValue("%prod.my.prop"));
        assertEquals("5678", defaultValues.getValue("%dev.my.prop"));
        assertEquals("1234", defaultValues.getValue("%test.my.prop"));
        assertEquals("1234", config.getValue("my.prop", String.class));
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
