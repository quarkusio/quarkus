package io.quarkus.reactive.db2.client.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class DataSourceReactiveDB2Config {

    /**
     * Whether prepared statements should be cached on the client side.
     *
     * @deprecated use {@code datasource.reactive.cache-prepared-statements} instead.
     */
    @ConfigItem
    @Deprecated
    public Optional<Boolean> cachePreparedStatements = Optional.empty();

    /**
     * Whether SSL/TLS is enabled.
     */
    @ConfigItem(defaultValue = "false")
    public boolean ssl = false;

}
