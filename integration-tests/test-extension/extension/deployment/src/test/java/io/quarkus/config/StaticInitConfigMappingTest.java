package io.quarkus.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.SmallRyeConfig;

public class StaticInitConfigMappingTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(StaticInitConfigSourceFactory.class)
                    .addAsServiceProvider("io.smallrye.config.ConfigSourceFactory",
                            StaticInitConfigSourceFactory.class.getName()));

    // This does not come from the static init Config, but it is registered in both static and runtime.
    // If it doesn't fail, it means that the static mapping was done correctly.
    @Inject
    StaticInitConfigMapping mapping;
    @Inject
    SmallRyeConfig config;

    @Test
    void staticInitMapping() {
        assertEquals("1234", mapping.myProp());
        assertTrue(config.getConfigSource("StaticInitConfigSource").isPresent());
    }

    @ConfigMapping(prefix = "config.static.init")
    public interface StaticInitConfigMapping {
        String myProp();
    }
}
