package io.quarkus.arc.test.config;

import io.smallrye.config.ConfigMapping;

/**
 * Not nested in {@link ConfigMappingAfterExtensionConfigRootTest}, so that it is only part of the additional
 * dependency and not of the application root.
 */
@ConfigMapping(prefix = "dependency")
public interface DependencyMapping {
    String name();
}
