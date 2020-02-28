package io.quarkus.reactive.datasource.runtime;

import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * For now, the reactive extensions only support a default datasource.
 */
@ConfigRoot(name = "datasource.reactive", phase = ConfigPhase.RUN_TIME)
public class DataSourceReactiveRuntimeConfig {

    /**
     * The datasource URL.
     */
    @ConfigItem
    public Optional<String> url;

    /**
     * The datasource pool maximum size.
     */
    @ConfigItem
    public OptionalInt maxSize;
}
