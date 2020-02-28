package io.quarkus.datasource.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * This configuration class is here for compatibility reason and is planned for removal.
 */
@Deprecated
@ConfigGroup
public class LegacyDataSourceRuntimeConfig {

    /**
     * @deprecated use either quarkus.datasource.jdbc.url or quarkus.datasource.reactive.url.
     */
    @ConfigItem
    @Deprecated
    public Optional<String> url;

    /**
     * @deprecated use either quarkus.datasource.jdbc.max-size or quarkus.datasource.reactive.max-size.
     */
    @ConfigItem(defaultValue = "20")
    public int maxSize = 20;
}
