package io.quarkus.extest.runtime.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "unremoveable")
public interface UnremovableMappingFromBuildItem {
    String value();
}
