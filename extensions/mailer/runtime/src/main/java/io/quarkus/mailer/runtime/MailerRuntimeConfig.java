package io.quarkus.mailer.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Pattern;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface MailerRuntimeConfig {

    /**
     * Sets the default `from` attribute when not specified in the {@link io.quarkus.mailer.Mail} instance.
     * It's the sender email address.
     */
    Optional<String> from();

    /**
     * Enables the mock mode.
     * When enabled, mails are not sent, but stored in an in-memory mailbox.
     * The content of the emails is also printed on the console.
     * <p>
     * Disabled by default on PROD, enabled by default on DEV and TEST modes.
     */
    Optional<Boolean> mock();

    /**
     * Sets the default bounce email address.
     * A bounced email, or bounce, is an email message that gets rejected by a mail server.
     */
    Optional<String> bounceAddress();

    /**
     * Sets the SMTP host name.
     */
    @WithDefault("localhost")
    String host();

    /**
     * The SMTP port.
     * The default value depends on the configuration.
     * The port 25 is used as default when {@link #ssl} is disabled.
     * This port continues to be used primarily for SMTP relaying.
     * SMTP relaying is the transmission of email from email server to email server.
     * The port 587 is the default port when {@link #ssl} is enabled.
     * It ensures that email is submitted securely.
     * <p>
     * Note that the port 465 may be used by SMTP servers, however, IANA has reassigned a new service to this port,
     * and it should no longer be used for SMTP communications.
     */
    OptionalInt port();

    /**
     * Sets the username to connect to the SMTP server.
     */
    Optional<String> username();

    /**
     * Sets the password to connect to the SMTP server.
     */
    Optional<String> password();

    /**
     * The name of the TLS configuration to use.
     * <p>
     * If a name is configured, it uses the configuration from {@code quarkus.tls.<name>.*}
     * If a name is configured, but no TLS configuration is found with that name then an error will be thrown.
     * <p>
     * If no TLS configuration name is set then, the specific TLS configuration (from {@code quarkus.mailer.*}) will be used.
     * <p>
     * The default TLS configuration is <strong>not</strong> used by default.
     */
    Optional<String> tlsConfigurationName();

    /**
     * Enables or disables the TLS/SSL.
     *
     * @deprecated Use {{@link #tls}}
     */
    @Deprecated
    @WithDefault("false")
    boolean ssl();

    /**
     * Whether the connection should be secured using TLS.
     * <p>
     * SMTP allows establishing connection with or without TLS.
     * When establishing a connection with TLS, the connection is secured and encrypted.
     * When establishing a connection without TLS, it can be secured and encrypted later using the STARTTLS command.
     * In this case, the connection is initially unsecured and unencrypted.
     * To configure this case, set this property to {@code false} and {@link #startTLS} to {@code REQUIRED}
     *
     * Note that if a TLS configuration is set, TLS is enabled automatically. So, setting this property to {@code false} is
     * required to not establish a connection with TLS.
     */
    Optional<Boolean> tls();

    /**
     * Set whether all server certificates should be trusted.
     * This option is only used when {@link #ssl} is enabled.
     *
     * @deprecated Use the TLS registry instead.
     */
    @Deprecated
    Optional<Boolean> trustAll();

    /**
     * Sets the max number of open connections to the mail server.
     */
    @WithDefault("10")
    int maxPoolSize();

    /**
     * Sets the hostname to be used for HELO/EHLO and the Message-ID.
     */
    Optional<String> ownHostName();

    /**
     * Sets if connection pool is enabled.
     * If the connection pooling is disabled, the max number of sockets is enforced nevertheless.
     */
    @WithDefault("true")
    boolean keepAlive();

    /**
     * Disable ESMTP.
     * <p>
     * The RFC-1869 states that clients should always attempt {@code EHLO} as first command to determine if ESMTP
     * is supported, if this returns an error code, {@code HELO} is tried to use the <em>regular</em> SMTP command.
     */
    @WithDefault("false")
    boolean disableEsmtp();

    /**
     * Sets the TLS security mode for the connection.
     * Either {@code DISABLED}, {@code OPTIONAL} or {@code REQUIRED}.
     */
    @WithDefault("OPTIONAL")
    String startTLS();

    /**
     * Configures DKIM signature verification.
     */
    DkimSignOptionsConfig dkim();

    /**
     * Sets the login mode for the connection.
     * Either {@code NONE}, {@code DISABLED}, {@code REQUIRED} or {@code XOAUTH2}.
     * <ul>
     * <li>NONE means a login will be attempted if the server supports it and login credentials are set</li>
     * <li>DISABLED means no login will be attempted</li>
     * <li>REQUIRED means that a login will be attempted if the server supports it and the send operation will fail
     * otherwise</li>
     * <li>XOAUTH2 means that a login will be attempted using Google Gmail Oauth2 tokens</li>
     * </ul>
     */
    @WithDefault("NONE")
    String login();

    /**
     * Sets the allowed authentication methods.
     * These methods will be used only if the server supports them.
     * If not set, all supported methods may be used.
     * <p>
     * The list is given as a space separated list, such as {@code DIGEST-MD5 CRAM-SHA256 CRAM-SHA1 CRAM-MD5 PLAIN LOGIN}.
     */
    Optional<String> authMethods();

    /**
     * Set the trust store.
     *
     * @deprecated Use the TLS registry instead.
     */
    @Deprecated
    Optional<String> keyStore();

    /**
     * Sets the trust store password if any.
     *
     * @deprecated Use the TLS registry instead.
     */
    Optional<String> keyStorePassword();

    /**
     * Configures the trust store.
     *
     * @deprecated Use the TLS registry instead.
     */
    TrustStoreConfig truststore();

    /**
     * Whether the mail should always been sent as multipart even if they don't have attachments.
     * When sets to true, the mail message will be encoded as multipart even for simple mails without attachments.
     */
    @WithDefault("false")
    boolean multiPartOnly();

    /**
     * Sets if sending allows recipients errors.
     * If set to true, the mail will be sent to the recipients that the server accepted, if any.
     */
    @WithDefault("false")
    boolean allowRcptErrors();

    /**
     * Enables or disables the pipelining capability if the SMTP server supports it.
     */
    @WithDefault("true")
    boolean pipelining();

    /**
     * Sets the connection pool cleaner period.
     * Zero disables expiration checks and connections will remain in the pool until they are closed.
     */
    @WithDefault("PT1S")
    Duration poolCleanerPeriod();

    /**
     * Set the keep alive timeout for the SMTP connection.
     * This value determines how long a connection remains unused in the pool before being evicted and closed.
     * A timeout of 0 means there is no timeout.
     */
    @WithDefault("PT300S")
    Duration keepAliveTimeout();

    /**
     * Configures NTLM (Windows New Technology LAN Manager).
     */
    NtlmConfig ntlm();

    /**
     * Allows sending emails to these recipients only.
     * <p>
     * Approved recipients are compiled to a {@code Pattern} and must be a valid regular expression.
     * The created {@code Pattern} is case-insensitive as emails are case insensitive.
     * Provided patterns are trimmed before being compiled.
     *
     * @see {@link #logRejectedRecipients}
     */
    Optional<List<@WithConverter(TrimmedPatternConverter.class) Pattern>> approvedRecipients();

    /**
     * Log rejected recipients as warnings.
     * <p>
     * If false, the rejected recipients will be logged at the DEBUG level.
     *
     * @see {@link #approvedRecipients}
     */
    @WithDefault("false")
    boolean logRejectedRecipients();

    /**
     * Log invalid recipients as warnings.
     * <p>
     * If false, the invalid recipients will not be logged and the thrown exception will not contain the invalid email address.
     *
     */
    @WithDefault("false")
    boolean logInvalidRecipients();
}
