package io.quarkus.arango.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class ArangoConfiguration {

    static final String DEFAULT_SERVER_URI = "http://127.0.0.1:8529";

    /**
     * The uri this driver should connect to. The driver supports bolt, bolt+routing or neo4j as schemes.
     */
    @ConfigItem(defaultValue = DEFAULT_SERVER_URI)
    public String uri;
}
