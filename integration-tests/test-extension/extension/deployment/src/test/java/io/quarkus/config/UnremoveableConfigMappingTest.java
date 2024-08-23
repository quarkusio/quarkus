package io.quarkus.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.extest.runtime.config.UnremovableMappingFromBuildItem;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.SmallRyeConfig;

public class UnremoveableConfigMappingTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(UnremovableMappingFromBuildItem.class));

    @Inject
    SmallRyeConfig config;

    @Test
    void unremoveableMapping() {
        UnremovableMappingFromBuildItem mapping = config.getConfigMapping(UnremovableMappingFromBuildItem.class);
        assertEquals("1234", mapping.value());
    }
}
