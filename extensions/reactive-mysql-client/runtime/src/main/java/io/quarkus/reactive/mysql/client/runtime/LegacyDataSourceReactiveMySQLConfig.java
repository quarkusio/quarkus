package io.quarkus.reactive.mysql.client.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "reactive-mysql-client", phase = ConfigPhase.RUN_TIME)
@Deprecated
public class LegacyDataSourceReactiveMySQLConfig {

    /**
     * @deprecated use quarkus.datasource.reactive.mysql.cache-prepared-statements instead.
     */
    @ConfigItem
    @Deprecated
    public Optional<Boolean> cachePreparedStatements;

    /**
     * @deprecated use quarkus.datasource.reactive.mysql.charset instead.
     */
    @ConfigItem
    @Deprecated
    public Optional<String> charset;

    /**
     * @deprecated use quarkus.datasource.reactive.mysql.collation instead.
     */
    @ConfigItem
    @Deprecated
    public Optional<String> collation;
}
