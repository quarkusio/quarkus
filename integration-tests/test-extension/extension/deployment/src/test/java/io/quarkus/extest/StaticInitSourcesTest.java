package io.quarkus.extest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.extest.runtime.config.StaticInitNotSafeConfigBuilderCustomizer;
import io.quarkus.extest.runtime.config.StaticInitNotSafeConfigSource;
import io.quarkus.extest.runtime.config.StaticInitSafeConfigBuilderCustomizer;
import io.quarkus.extest.runtime.config.StaticInitSafeConfigSource;
import io.quarkus.test.QuarkusUnitTest;

public class StaticInitSourcesTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    // Don't change this to types, because of classloader class cast exception.
                    .addAsServiceProvider("io.smallrye.config.SmallRyeConfigBuilderCustomizer",
                            StaticInitSafeConfigBuilderCustomizer.class.getName(),
                            StaticInitNotSafeConfigBuilderCustomizer.class.getName())
                    .addAsResource("application.properties"))
            .failOnUnknownProperties(false); // quarkus.build.unknown.prop from UnknownBuildPropertyConfigSourceFactory

    @Test
    void staticInitSources() {
        assertEquals(2, StaticInitSafeConfigSource.counter.get());
        assertEquals(1, StaticInitNotSafeConfigSource.counter.get());
    }
}
