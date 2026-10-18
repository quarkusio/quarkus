package io.quarkus.config.yaml.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * Test the YAML config provider (plan JUnit). We aren't re-testing the whole config source
 * (that's done in SmallRye Config) but we do make sure that both the file system and in-JAR
 * properties are being picked up.
 */
public class ApplicationYamlTest {

    private static SmallRyeConfig config;

    @BeforeAll
    public static void doBefore() {
        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
        builder.addDefaultSources().addDiscoveredConverters().addDiscoveredSources();
        config = builder.build();
        System.out.println(System.getProperty("user.dir"));
    }

    @Test
    public void testBasicApplicationYaml() {
        assertEquals("something", config.getValue("foo.bar", String.class));
        assertEquals("somethingElse", config.getValue("foo2.bar", String.class));
        assertEquals("other", config.getValue("foo.baz", String.class));
        assertTrue(config.getValue("file.system", Boolean.class).booleanValue());
    }

    @AfterAll
    public static void doAfter() {
        config = null;
    }
}
