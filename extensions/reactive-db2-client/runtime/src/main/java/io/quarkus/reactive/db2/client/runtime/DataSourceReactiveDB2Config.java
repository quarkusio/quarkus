package io.quarkus.reactive.db2.client.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "datasource.reactive.db2", phase = ConfigPhase.RUN_TIME)
public class DataSourceReactiveDB2Config {

    /**
     * Whether prepared statements should be cached on the client side.
     * 
     * @deprecated use {@code datasource.reactive.cache-prepared-statements} instead.
     */
    @ConfigItem
    @Deprecated
    public Optional<Boolean> cachePreparedStatements;

    /**
     * Whether SSL/TLS is enabled.
     */
    @ConfigItem(defaultValue = "false")
    public boolean ssl;

}
