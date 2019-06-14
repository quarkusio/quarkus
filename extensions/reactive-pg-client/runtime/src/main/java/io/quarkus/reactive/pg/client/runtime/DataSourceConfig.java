package io.quarkus.reactive.pg.client.runtime;

import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "datasource", phase = ConfigPhase.RUN_TIME)
public class DataSourceConfig {

    /**
     * The datasource URL.
     */
    @ConfigItem
    public Optional<String> url;

    /**
     * The datasource username.
     */
    @ConfigItem
    public Optional<String> username;

    /**
     * The datasource password.
     */
    @ConfigItem
    public Optional<String> password;

    /**
     * The datasource pool maximum size.
     */
    @ConfigItem
    public OptionalInt maxSize;
}
