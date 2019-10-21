package io.quarkus.neo4j.runtime;

import java.time.Duration;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class Neo4jConfiguration {

    static final String DEFAULT_SERVER_URI = "bolt://localhost:7687";
    static final String DEFAULT_USERNAME = "neo4j";
    static final String DEFAULT_PASSWORD = "neo4j";

    /**
     * The uri this driver should connect to. The driver supports bolt, bolt+routing or neo4j as schemes.
     */
    @ConfigItem(defaultValue = DEFAULT_SERVER_URI)
    public String uri;

    /**
     * Authentication.
     */
    @ConfigItem
    @ConfigDocSection
    public Authentication authentication;

    /**
     * Connection pool.
     */
    @ConfigItem
    @ConfigDocSection
    public Pool pool;

    @ConfigGroup
    static class Authentication {

        /**
         * The login of the user connecting to the database.
         */
        @ConfigItem(defaultValue = DEFAULT_USERNAME)
        public String username;

        /**
         * The password of the user connecting to the database.
         */
        @ConfigItem(defaultValue = DEFAULT_PASSWORD)
        public String password;

        /**
         * Set this to true to disable authentication.
         */
        @ConfigItem(defaultValue = "false")
        public boolean disabled = false;
    }

    @ConfigGroup
    static class Pool {

        /**
         * Flag, if metrics are enabled.
         */
        @ConfigItem(defaultValue = "false")
        public boolean metricsEnabled;

        /**
         * Flag, if leaked sessions logging is enabled.
         */
        @ConfigItem(defaultValue = "false")
        public boolean logLeakedSessions;

        /**
         * The maximum amount of connections in the connection pool towards a single database.
         */
        @ConfigItem(defaultValue = "100")
        public int maxConnectionPoolSize;

        /**
         * Pooled connections that have been idle in the pool for longer than this timeout will be tested before they are used
         * again. The value {@literal 0} means connections will always be tested for validity and negative values mean
         * connections
         * will never be tested.
         */
        @ConfigItem(defaultValue = "-0.001S")
        public Duration idleTimeBeforeConnectionTest;

        /**
         * Pooled connections older than this threshold will be closed and removed from the pool.
         */
        @ConfigItem(defaultValue = "1H")
        public Duration maxConnectionLifetime;

        /**
         * Acquisition of new connections will be attempted for at most configured timeout.
         */
        @ConfigItem(defaultValue = "1M")
        public Duration connectionAcquisitionTimeout;

        @Override
        public String toString() {
            return "Pool{" +
                    "metricsEnabled=" + metricsEnabled +
                    ", logLeakedSessions=" + logLeakedSessions +
                    ", maxConnectionPoolSize=" + maxConnectionPoolSize +
                    ", idleTimeBeforeConnectionTest=" + idleTimeBeforeConnectionTest +
                    ", maxConnectionLifetime=" + maxConnectionLifetime +
                    ", connectionAcquisitionTimeout=" + connectionAcquisitionTimeout +
                    '}';
        }
    }
}
