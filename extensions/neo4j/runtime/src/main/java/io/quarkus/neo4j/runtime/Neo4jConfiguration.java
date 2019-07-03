package io.quarkus.neo4j.runtime;

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
     * The uri this driver should connect to. The driver supports bolt, bolt+routing or neo4j as schemes. If uri is empty,
     * the driver tries to connect to 'neo4j://localhost:7687'.
     */
    @ConfigItem(defaultValue = DEFAULT_SERVER_URI)
    public String uri;

    /**
     * The authentication the driver is supposed to use.
     */
    @ConfigItem
    public Authentication authentication;

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
}
