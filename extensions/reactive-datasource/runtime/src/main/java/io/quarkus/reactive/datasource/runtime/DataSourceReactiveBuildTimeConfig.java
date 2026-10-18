package io.quarkus.reactive.datasource.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

@ConfigGroup
public interface DataSourceReactiveBuildTimeConfig {

    /**
     * If we create a Reactive datasource for this datasource.
     */
    @WithDefault("true")
    @WithParentName
    boolean enabled();
}
