package io.quarkus.extest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.extest.runtime.AdditionalStaticInitConfigSourceProvider;
import io.quarkus.extest.runtime.config.StaticInitNotSafeConfigSource;
import io.quarkus.extest.runtime.config.StaticInitSafeConfigSource;
import io.quarkus.test.QuarkusUnitTest;

public class StaticInitSourcesTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ConfiguredBean.class)
                    // Don't change this to types, because of classloader class cast exception.
                    .addAsServiceProvider("org.eclipse.microprofile.config.spi.ConfigSource",
                            StaticInitSafeConfigSource.class.getName(),
                            StaticInitNotSafeConfigSource.class.getName())
                    .addAsResource("application.properties"));

    @Test
    void staticInitSources() {
        assertEquals(2, StaticInitSafeConfigSource.counter.get());
        assertEquals(1, StaticInitNotSafeConfigSource.counter.get());
    }

    @Test
    void deprecatedStaticInitBuildItem() {
        assertEquals(1, AdditionalStaticInitConfigSourceProvider.AdditionalStaticInitConfigSource.counter.get());
    }
}
