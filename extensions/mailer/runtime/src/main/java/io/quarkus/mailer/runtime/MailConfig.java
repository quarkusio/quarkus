package io.quarkus.mailer.runtime;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "mailer", phase = ConfigPhase.RUN_TIME)
public class MailConfig {

    /**
     * Configure the default `from` attribute.
     * It's the sender email address.
     */
    @ConfigItem
    public Optional<String> from;

    /**
     * Enables the mock mode, not sending emails.
     * The content of the emails is printed on the console.
     * <p>
     * Disabled by default on PROD, enabled by default on DEV and TEST modes.
     */
    @ConfigItem
    public Optional<Boolean> mock;

    /**
     * Configures the default bounce email address.
     */
    @ConfigItem
    public Optional<String> bounceAddress;

    /**
     * The SMTP host name.
     */
    @ConfigItem(defaultValue = "localhost")
    public String host;

    /**
     * The SMTP port.
     */
    @ConfigItem
    public OptionalInt port;

    /**
     * The username.
     */
    @ConfigItem
    public Optional<String> username;

    /**
     * The password.
     */
    @ConfigItem
    public Optional<String> password;

    /**
     * Enables or disables the SSL on connect.
     * {@code false} by default.
     */
    @ConfigItem
    public boolean ssl;

    /**
     * Set whether to trust all certificates on ssl connect the option is also
     * applied to {@code STARTTLS} operation. Disabled by default.
     */
    @ConfigItem
    public Optional<Boolean> trustAll = Optional.empty();

    /**
     * Configures the maximum allowed number of open connections to the mail server
     * If not set the default is {@code 10}.
     */
    @ConfigItem(defaultValue = "10")
    public int maxPoolSize;

    /**
     * The hostname to be used for HELO/EHLO and the Message-ID
     */
    @ConfigItem
    public Optional<String> ownHostName;

    /**
     * Set if connection pool is enabled, {@code true} by default.
     * <p>
     * If the connection pooling is disabled, the max number of sockets is enforced nevertheless.
     * <p>
     */
    @ConfigItem(defaultValue = "true")
    public boolean keepAlive;

    /**
     * Disable ESMTP. {@code false} by default.
     * The RFC-1869 states that clients should always attempt {@code EHLO} as first command to determine if ESMTP
     * is supported, if this returns an error code, {@code HELO} is tried to use the <em>regular</em> SMTP command.
     */
    @ConfigItem
    public boolean disableEsmtp;

    /**
     * Set the TLS security mode for the connection.
     * Either {@code DISABLED}, {@code OPTIONAL} or {@code REQUIRED}.
     */
    @ConfigItem(defaultValue = "OPTIONAL")
    public String startTLS;

    /**
     * Set the login mode for the connection.
     * Either @{code DISABLED}, {@code NONE}, {@code OPTIONAL}, {@code REQUIRED}, or {@code XOAUTH2}.
     *
     * DISABLED means no login will be attempted.
     * NONE means a login will be attempted if the server supports in and login credentials are set.
     * REQUIRED means that a login will be attempted if the server supports it and the send operation will fail otherwise.
     * XOAUTH2 means that a login will be attempted using Google Gmail Oauth2 tokens.
     */
    @ConfigItem(defaultValue = "DISABLED")
    public String login;

    /**
     * Set the allowed auth methods.
     * If defined, only these methods will be used, if the server supports them.
     */
    @ConfigItem
    public Optional<String> authMethods;

    /**
     * Set the key store.
     * 
     * @deprecated Use {@link #trustStore} instead
     */
    @ConfigItem
    public Optional<String> keyStore;

    /**
     * Set the key store password.
     * 
     * @deprecated Use {@link #trustStorePassword} instead
     */
    @ConfigItem
    public Optional<String> keyStorePassword;

    /**
     * Whether or not the mail should always been sent as multipart even if they don't have attachments.
     */
    @ConfigItem(defaultValue = "false")
    public boolean multiPartOnly;

    /**
     * Enables DKIM (DomainKeys Identified Mail)
     */
    @ConfigItem
    public Optional<DKIMSignConfig> dkim;

    /**
     * Enables or disables pipelining.
     */
    @ConfigItem(defaultValue = "true")
    public boolean pipelining;

    /**
     * Set the connection pool cleaner period.
     */
    @ConfigItem(defaultValue = "1s")
    public Duration poolCleanerPeriod;

    /**
     * Set the keep alive timeout.
     */
    @ConfigItem(defaultValue = "300s")
    public Duration keepAliveTimeout;

    /**
     * Set the trust store which holds the certificate information of the certificates to trust.
     *
     * The trust store can be either on classpath or in an external file.
     * The file must be a JKS file.
     */
    @ConfigItem
    public Optional<String> trustStore;

    /**
     * Set the truststore password to be used when opening SMTP connections.
     */
    @ConfigItem
    public Optional<String> trustStorePassword;
}
