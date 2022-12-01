package io.quarkus.config.yaml.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.runtime.configuration.QuarkusConfigFactory;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * Test the YAML config provider (plan JUnit). We aren't re-testing the whole config source
 * (that's done in SmallRye Config) but we do make sure that both the file system and in-JAR
 * properties are being picked up.
 */
public class ApplicationYamlTest {

    static volatile SmallRyeConfig config;

    @BeforeAll
    public static void doBefore() {
        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
        builder.addDefaultSources().addDiscoveredConverters().addDiscoveredSources();
        QuarkusConfigFactory.setConfig(config = builder.build());
        Config conf = ConfigProvider.getConfig();
        if (conf != config) {
            ConfigProviderResolver cpr = ConfigProviderResolver.instance();
            cpr.releaseConfig(conf);
            ConfigProvider.getConfig();
        }
        System.out.println(System.getProperty("user.dir"));
    }

    @Test
    public void testBasicApplicationYaml() {
        assertEquals("something", ConfigProvider.getConfig().getValue("foo.bar", String.class));
        assertEquals("somethingElse", ConfigProvider.getConfig().getValue("foo2.bar", String.class));
        assertEquals("other", ConfigProvider.getConfig().getValue("foo.baz", String.class));
        assertTrue(ConfigProvider.getConfig().getValue("file.system", Boolean.class).booleanValue());
    }

    @AfterAll
    public static void doAfter() {
        ConfigProviderResolver cpr = ConfigProviderResolver.instance();
        cpr.releaseConfig(config);
        config = null;
    }
}
