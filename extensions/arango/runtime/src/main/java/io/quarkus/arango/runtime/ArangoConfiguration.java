package io.quarkus.arango.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class ArangoConfiguration {

    private static final String DEFAULT_SERVER_URI = "127.0.0.1:8529";
    private static final String ACQUIRE_HOST_LIST = "false";
    private static final String USER = "root";
    private static final String TIMEOUT = "0";
    private static final String ACQUIRE_HOST_LIST_INTERVAL = "3";
    private static final String MAX_CONNECTIONS = "100";

    /**
     * The uri this driver should connect to. Multiple uris support: 127.0.0.1:8529,127.0.0.1:8529.
     */
    @ConfigItem(defaultValue = DEFAULT_SERVER_URI)
    public String uri;

    /**
     * Whether or not the driver should acquire a list of available coordinators in an ArangoDB cluster or a single server with
     * active failover.
     */
    @ConfigItem(defaultValue = ACQUIRE_HOST_LIST)
    public Boolean acquireHostList;

    /**
     * Sets the username to use for authentication.
     */
    @ConfigItem(defaultValue = USER)
    public String user;

    /**
     * Sets the password for the user for authentication
     */
    @ConfigItem
    public String password;

    /**
     * Sets the connection and request timeout in milliseconds.
     */
    @ConfigItem(defaultValue = TIMEOUT)
    public Integer timeout;

    /**
     * Setting the Interval for acquireHostList Interval in Seconds
     */
    @ConfigItem(defaultValue = ACQUIRE_HOST_LIST_INTERVAL)
    public Integer acquireHostListInterval;

    /**
     * Sets the maximum number of connections the built in connection pool will open per host.
     */
    @ConfigItem(defaultValue = MAX_CONNECTIONS)
    public Integer maxConnections;
}
