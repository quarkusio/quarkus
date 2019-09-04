package io.quarkus.reactive.mysql.client.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "reactive-mysql-client", phase = ConfigPhase.RUN_TIME)
public class MySQLPoolConfig {

    /**
     * Whether prepared statements should be cached on the client side.
     */
    @ConfigItem
    public Optional<Boolean> cachePreparedStatements;

    /**
     * Charset for connections.
     */
    @ConfigItem
    public Optional<String> charset;

    /**
     * Collation for connections.
     */
    @ConfigItem
    public Optional<String> collation;
}
