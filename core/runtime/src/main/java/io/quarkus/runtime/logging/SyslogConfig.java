package io.quarkus.runtime.logging;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.logging.Level;

import org.jboss.logmanager.handlers.SyslogHandler.Facility;
import org.jboss.logmanager.handlers.SyslogHandler.Protocol;
import org.jboss.logmanager.handlers.SyslogHandler.SyslogType;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class SyslogConfig {

    /**
     * If syslog logging should be enabled
     */
    @ConfigItem
    boolean enable;

    /**
     *
     * The IP address and port of the syslog server
     */
    @ConfigItem(defaultValue = "localhost:514")
    InetSocketAddress endpoint;

    /**
     * The app name used when formatting the message in RFC5424 format
     */
    @ConfigItem
    Optional<String> appName;

    /**
     * The name of the host the messages are being sent from
     */
    @ConfigItem
    Optional<String> hostname;

    /**
     * Sets the facility used when calculating the priority of the message as defined by RFC-5424 and RFC-3164
     */
    @ConfigItem(defaultValue = "user-level")
    Facility facility;

    /**
     * Set the {@link SyslogType syslog type} this handler should use to format the message sent
     */
    @ConfigItem(defaultValue = "rfc5424")
    SyslogType syslogType;

    /**
     * Sets the protocol used to connect to the syslog server
     */
    @ConfigItem(defaultValue = "tcp")
    Protocol protocol;

    /**
     * Set to {@code true} if the message being sent should be prefixed with the size of the message
     */
    @ConfigItem
    boolean useCountingFraming;

    /**
     * Set to {@code true} if the message should be truncated
     */
    @ConfigItem(defaultValue = "true")
    boolean truncate;

    /**
     * Enables or disables blocking when attempting to reconnect a
     * {@link org.jboss.logmanager.handlers.SyslogHandler.Protocol#TCP
     * TCP} or {@link org.jboss.logmanager.handlers.SyslogHandler.Protocol#SSL_TCP SSL TCP} protocol
     */
    @ConfigItem
    boolean blockOnReconnect;

    /**
     * The log message format
     */
    @ConfigItem(defaultValue = "%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) %s%e%n")
    String format;

    /**
     * The log level specifying, which message levels will be logged by syslog logger
     */
    @ConfigItem(defaultValue = "ALL")
    Level level;

    /**
     * Syslog async logging config
     */
    AsyncConfig async;
}
