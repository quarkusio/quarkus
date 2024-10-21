package io.quarkus.runtime.logging;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.logging.Level;

import org.jboss.logmanager.handlers.SocketHandler.Protocol;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class SocketConfig {

    /**
     * If socket logging should be enabled
     */
    @ConfigItem
    boolean enable;

    /**
     *
     * The IP address and port of the server receiving the logs
     */
    @ConfigItem(defaultValue = "localhost:4560")
    InetSocketAddress endpoint;

    /**
     * Sets the protocol used to connect to the syslog server
     */
    @ConfigItem(defaultValue = "tcp")
    Protocol protocol;

    /**
     * Enables or disables blocking when attempting to reconnect a
     * {@link Protocol#TCP
     * TCP} or {@link Protocol#SSL_TCP SSL TCP} protocol
     */
    @ConfigItem
    boolean blockOnReconnect;

    /**
     * The log message format
     */
    @ConfigItem(defaultValue = "%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) %s%e%n")
    String format;

    /**
     * The log level specifying, which message levels will be logged by socket logger
     */
    @ConfigItem(defaultValue = "ALL")
    Level level;

    /**
     * The name of the filter to link to the file handler.
     */
    @ConfigItem
    Optional<String> filter;

    /**
     * Socket async logging config
     */
    AsyncConfig async;
}