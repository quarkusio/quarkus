package io.quarkus.mailer.impl;

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
     *
     * Disabled by default.
     */
    @ConfigItem
    public boolean mock;

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
     * applied to {@code STARTTLS} operation. {@code false} by default.
     */
    @ConfigItem
    public boolean trustAll;

    /**
     * The pool maximum size.
     */
    @ConfigItem
    public OptionalInt maxPoolSize;

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
     * Set if ESMTP should be tried as first command (EHLO).
     * {@code false} by default.
     */
    @ConfigItem
    public boolean disableEsmtp;

    /**
     * Set the TLS security mode for the connection.
     * Either {@code NONE}, {@code OPTIONAL} or {@code REQUIRED}.
     */
    @ConfigItem
    public Optional<String> startTLS;

    /**
     * Set the login mode for the connection.
     * Either {@code DISABLED}, @{code OPTIONAL} or {@code REQUIRED}
     */
    @ConfigItem
    public Optional<String> login;

    /**
     * Set the allowed auth methods.
     * If defined, only these methods will be used, if the server supports them.
     */
    @ConfigItem
    public Optional<String> authMethods;

    /**
     * Set the key store.
     */
    @ConfigItem
    public Optional<String> keyStore;

    /**
     * Set the key store password.
     */
    @ConfigItem
    public Optional<String> keyStorePassword;
}
