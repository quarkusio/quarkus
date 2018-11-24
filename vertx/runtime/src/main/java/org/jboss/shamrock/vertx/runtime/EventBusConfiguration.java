package org.jboss.shamrock.vertx.runtime;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shamrock.runtime.ConfigGroup;

@ConfigGroup
public class EventBusConfiguration {

    /**
     * The key configuration for the PEM format.
     */
    @ConfigProperty(name = "key-certificate-pem")
    public PemKeyCertConfiguration keyPem;

    /**
     * The key configuration for the JKS format.
     */
    @ConfigProperty(name = "key-certificate-jks")
    public JksConfiguration keyJks;

    /**
     * The key configuration for the PFX format.
     */
    @ConfigProperty(name = "key-certificate-pfx")
    public PfxConfiguration keyPfx;

    /**
     * The trust key configuration for the PEM format.
     */
    @ConfigProperty(name = "trust-certificate-pem")
    public PemTrustCertConfiguration trustPem;

    /**
     * The trust key configuration for the JKS format.
     */
    @ConfigProperty(name = "trust-certificate-jks")
    public JksConfiguration trustJks;

    /**
     * The trust key configuration for the PFX format.
     */
    @ConfigProperty(name = "trust-certificate-pfx")
    public PfxConfiguration trustPfx;

    /**
     * The accept backlog.
     */
    @ConfigProperty(name = "acceptBacklog", defaultValue = "-1")
    public int acceptBacklog;

    /**
     * The client authentication.
     */
    @ConfigProperty(name = "clientAuth", defaultValue = "NONE")
    public String clientAuth;

    /**
     * The connect timeout in milliseconds.
     */
    @ConfigProperty(name = "connectTimeout", defaultValue = "60000")
    public int connectTimeout;

    /**
     * The idle timeout in milliseconds.
     */
    @ConfigProperty(name = "idleTimeout", defaultValue = "0")
    public int idleTimeout;

    /**
     * The receive buffer size.
     */
    @ConfigProperty(name = "receiveBufferSize", defaultValue = "-1")
    public int receiveBufferSize;

    /**
     * The number of reconnection attempts.
     */
    @ConfigProperty(name = "reconnect-attempts", defaultValue = "0")
    public int reconnectAttempts;

    /**
     * The reconnection interval in milliseconds.
     */
    @ConfigProperty(name = "reconnect-interval", defaultValue = "1000")
    public int reconnectInterval;

    /**
     * Whether or not to reuse the address.
     */
    @ConfigProperty(name = "reuse-address", defaultValue = "true")
    public boolean reuseAddress;

    /**
     * Whether or not to reuse the port.
     */
    @ConfigProperty(name = "reuse-port", defaultValue = "false")
    public boolean reusePort;

    /**
     * The send buffer size.
     */
    @ConfigProperty(name = "sendBufferSize", defaultValue = "-1")
    public int sendBufferSize;

    /**
     * The so linger.
     */
    @ConfigProperty(name = "soLinger", defaultValue = "-1")
    public int soLinger;

    /**
     * Enables or Disabled SSL.
     */
    @ConfigProperty(name = "ssl", defaultValue = "false")
    public boolean ssl;

    /**
     * Whether or not to keep the TCP connection opened (keep-alive).
     */
    @ConfigProperty(name = "tcpKeepAlive", defaultValue = "false")
    public boolean tcpKeepAlive;

    /**
     * Configure the TCP no delay.
     */
    @ConfigProperty(name = "tcpNoDelay", defaultValue = "true")
    public boolean tcpNoDelay;

    /**
     * Configure the traffic class.
     */
    @ConfigProperty(name = "trafficClass", defaultValue = "-1")
    public int trafficClass;

    /**
     * Enables or disables the trust all parameter.
     */
    @ConfigProperty(name = "trustAll", defaultValue = "false")
    public boolean trustAll;


}
