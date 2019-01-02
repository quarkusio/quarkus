package org.jboss.shamrock.vertx.runtime;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shamrock.runtime.ConfigGroup;

import java.util.Optional;

@ConfigGroup
public class ClusterConfiguration {

    /**
     * The host name.
     */
    @ConfigProperty(name = "host", defaultValue = "localhost")
    public String host;

    /**
     * The port.
     */
    @ConfigProperty(name = "port", defaultValue = "0")
    public int port;

    /**
     * The public host name.
     */
    @ConfigProperty(name = "public-host")
    public Optional<String> publicHost;

    /**
     * The public port.
     */
    @ConfigProperty(name = "public-port", defaultValue = "-1")
    public int publicPort;

    /**
     * Enables or disables the clustering.
     */
    @ConfigProperty(name = "clustered", defaultValue = "false")
    public boolean clustered;

    /**
     * The ping interval in milliseconds.
     */
    @ConfigProperty(name = "ping-interval", defaultValue = "20000")
    public int pingInterval;

    /**
     * The ping reply interval in milliseconds.
     */
    @ConfigProperty(name = "ping-reply-interval", defaultValue = "20000")
    public int pingReplyInterval;
}
