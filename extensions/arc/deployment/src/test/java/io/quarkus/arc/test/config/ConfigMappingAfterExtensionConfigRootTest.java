package io.quarkus.arc.test.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * Extension config roots that end up in the index (here through {@code quarkus.index-dependency}) must be skipped
 * individually, without stopping the discovery of application mappings indexed after them.
 */
public class ConfigMappingAfterExtensionConfigRootTest {
    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            // additional application archives are indexed after the index-dependency archives
            .withAdditionalDependency(jar -> jar.addClass(DependencyMapping.class))
            .overrideConfigKey("quarkus.index-dependency.core.group-id", "io.quarkus")
            .overrideConfigKey("quarkus.index-dependency.core.artifact-id", "quarkus-core")
            .overrideConfigKey("dependency.name", "foo");

    @Inject
    DependencyMapping mapping;

    @Test
    void mapping() {
        assertEquals("foo", mapping.name());
    }
}
