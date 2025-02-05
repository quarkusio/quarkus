package io.quarkus.mailer.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Pattern;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConvertWith;

@ConfigGroup
public class MailerRuntimeConfig {

    /**
     * Sets the default `from` attribute when not specified in the {@link io.quarkus.mailer.Mail} instance.
     * It's the sender email address.
     */
    @ConfigItem
    public Optional<String> from = Optional.empty();

    /**
     * Enables the mock mode.
     * When enabled, mails are not sent, but stored in an in-memory mailbox.
     * The content of the emails is also printed on the console.
     * <p>
     * Disabled by default on PROD, enabled by default on DEV and TEST modes.
     */
    @ConfigItem
    public Optional<Boolean> mock = Optional.empty();

    /**
     * Sets the default bounce email address.
     * A bounced email, or bounce, is an email message that gets rejected by a mail server.
     */
    @ConfigItem
    public Optional<String> bounceAddress = Optional.empty();

    /**
     * Sets the SMTP host name.
     */
    @ConfigItem(defaultValue = "localhost")
    public String host = "localhost";

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
    @ConfigItem
    public OptionalInt port = OptionalInt.empty();

    /**
     * Sets the username to connect to the SMTP server.
     */
    @ConfigItem
    public Optional<String> username = Optional.empty();

    /**
     * Sets the password to connect to the SMTP server.
     */
    @ConfigItem
    public Optional<String> password = Optional.empty();

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
    @ConfigItem
    public Optional<String> tlsConfigurationName = Optional.empty();

    /**
     * Enables or disables the TLS/SSL.
     *
     * @deprecated Use {{@link #tls}}
     */
    @Deprecated
    @ConfigItem(defaultValue = "false")
    public boolean ssl;

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
    @ConfigItem
    public Optional<Boolean> tls;

    /**
     * Set whether all server certificates should be trusted.
     * This option is only used when {@link #ssl} is enabled.
     *
     * @deprecated Use the TLS registry instead.
     */
    @ConfigItem
    @Deprecated
    public Optional<Boolean> trustAll = Optional.empty();

    /**
     * Sets the max number of open connections to the mail server.
     */
    @ConfigItem(defaultValue = "10")
    public int maxPoolSize = 10;

    /**
     * Sets the hostname to be used for HELO/EHLO and the Message-ID.
     */
    @ConfigItem
    public Optional<String> ownHostName = Optional.empty();

    /**
     * Sets if connection pool is enabled.
     * If the connection pooling is disabled, the max number of sockets is enforced nevertheless.
     */
    @ConfigItem(defaultValue = "true")
    public boolean keepAlive = true;

    /**
     * Disable ESMTP.
     * <p>
     * The RFC-1869 states that clients should always attempt {@code EHLO} as first command to determine if ESMTP
     * is supported, if this returns an error code, {@code HELO} is tried to use the <em>regular</em> SMTP command.
     */
    @ConfigItem(defaultValue = "false")
    public boolean disableEsmtp;

    /**
     * Sets the TLS security mode for the connection.
     * Either {@code DISABLED}, {@code OPTIONAL} or {@code REQUIRED}.
     */
    @ConfigItem(defaultValue = "OPTIONAL")
    public String startTLS = "OPTIONAL";

    /**
     * Configures DKIM signature verification.
     */
    @ConfigItem
    public DkimSignOptionsConfig dkim = new DkimSignOptionsConfig();

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
    @ConfigItem(defaultValue = "NONE")
    public String login = "NONE";

    /**
     * Sets the allowed authentication methods.
     * These methods will be used only if the server supports them.
     * If not set, all supported methods may be used.
     * <p>
     * The list is given as a space separated list, such as {@code DIGEST-MD5 CRAM-SHA256 CRAM-SHA1 CRAM-MD5 PLAIN LOGIN}.
     */
    @ConfigItem
    public Optional<String> authMethods = Optional.empty();

    /**
     * Set the trust store.
     *
     * @deprecated Use the TLS registry instead.
     */
    @Deprecated
    @ConfigItem
    public Optional<String> keyStore = Optional.empty();

    /**
     * Sets the trust store password if any.
     *
     * @deprecated Use the TLS registry instead.
     */
    @ConfigItem
    @Deprecated
    public Optional<String> keyStorePassword = Optional.empty();

    /**
     * Configures the trust store.
     *
     * @deprecated Use the TLS registry instead.
     */
    @ConfigItem
    @Deprecated
    public TrustStoreConfig truststore = new TrustStoreConfig();

    /**
     * Whether the mail should always been sent as multipart even if they don't have attachments.
     * When sets to true, the mail message will be encoded as multipart even for simple mails without attachments.
     */
    @ConfigItem(defaultValue = "false")
    public boolean multiPartOnly;

    /**
     * Sets if sending allows recipients errors.
     * If set to true, the mail will be sent to the recipients that the server accepted, if any.
     */
    @ConfigItem(defaultValue = "false")
    public boolean allowRcptErrors;

    /**
     * Enables or disables the pipelining capability if the SMTP server supports it.
     */
    @ConfigItem(defaultValue = "true")
    public boolean pipelining = true;

    /**
     * Sets the connection pool cleaner period.
     * Zero disables expiration checks and connections will remain in the pool until they are closed.
     */
    @ConfigItem(defaultValue = "PT1S")
    public Duration poolCleanerPeriod = Duration.ofSeconds(1L);

    /**
     * Set the keep alive timeout for the SMTP connection.
     * This value determines how long a connection remains unused in the pool before being evicted and closed.
     * A timeout of 0 means there is no timeout.
     */
    @ConfigItem(defaultValue = "PT300S")
    public Duration keepAliveTimeout = Duration.ofSeconds(300L);

    /**
     * Configures NTLM (Windows New Technology LAN Manager).
     */
    @ConfigItem
    public NtlmConfig ntlm = new NtlmConfig();

    /**
     * Allows sending emails to these recipients only.
     * <p>
     * Approved recipients are compiled to a {@code Pattern} and must be a valid regular expression.
     * The created {@code Pattern} is case-insensitive as emails are case insensitive.
     * Provided patterns are trimmed before being compiled.
     *
     * @see {@link #logRejectedRecipients}
     */
    @ConfigItem
    @ConvertWith(TrimmedPatternConverter.class)
    public Optional<List<Pattern>> approvedRecipients = Optional.empty();

    /**
     * Log rejected recipients as warnings.
     * <p>
     * If false, the rejected recipients will be logged at the DEBUG level.
     *
     * @see {@link #approvedRecipients}
     */
    @ConfigItem(defaultValue = "false")
    public boolean logRejectedRecipients = false;

    /**
     * Log invalid recipients as warnings.
     * <p>
     * If false, the invalid recipients will not be logged and the thrown exception will not contain the invalid email address.
     *
     */
    @ConfigItem(defaultValue = "false")
    public boolean logInvalidRecipients = false;
}
