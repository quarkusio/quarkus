package io.quarkus.config;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.ConfigMapping;

public class StaticInitConfigMappingInvalidTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(RuntimeInitConfigSource.class)
                    .addAsServiceProvider("org.eclipse.microprofile.config.spi.ConfigSource",
                            RuntimeInitConfigSource.class.getName()))
            .assertException(throwable -> {
            });

    @Inject
    StaticInitConfigMapping mapping;

    @Test
    void fail() {
        Assertions.fail();
    }

    @StaticInitSafe
    @ConfigMapping(prefix = "config.static.init")
    public interface StaticInitConfigMapping {
        // The configuration does not exist at static init, so the startup will fail
        String myProp();
    }
}
