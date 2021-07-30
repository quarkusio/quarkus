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
     * Sets the default `from` attribute when not specified in the {@link io.quarkus.mailer.Mail} instance.
     * It's the sender email address.
     */
    @ConfigItem
    public Optional<String> from;

    /**
     * Enables the mock mode.
     * When enabled, mails are not sent, but stored in an in-memory mailbox.
     * The content of the emails is also printed on the console.
     * <p>
     * Disabled by default on PROD, enabled by default on DEV and TEST modes.
     */
    @ConfigItem
    public Optional<Boolean> mock;

    /**
     * Sets the default bounce email address.
     * A bounced email, or bounce, is an email message that gets rejected by a mail server.
     */
    @ConfigItem
    public Optional<String> bounceAddress;

    /**
     * Sets the SMTP host name.
     */
    @ConfigItem(defaultValue = "localhost")
    public String host;

    /**
     * The SMTP port.
     * The default value depends on the configuration.
     * The port 25 is used as default when {@link #ssl} is disabled.
     * This port continues to be used primarily for SMTP relaying.
     * SMTP relaying is the transmission of email from email server to email server.
     * The port 587 is the default port when {@link #ssl} is enabled.
     * It ensures that email is submitted securely.
     *
     * Note that the port 465 may be used by SMTP servers, however, IANA has reassigned a new service to this port,
     * and it should no longer be used for SMTP communications.
     */
    @ConfigItem
    public OptionalInt port;

    /**
     * Sets the username to connect to the SMTP server.
     */
    @ConfigItem
    public Optional<String> username;

    /**
     * Sets the password to connect to the SMTP server.
     */
    @ConfigItem
    public Optional<String> password;

    /**
     * Enables or disables the TLS/SSL.
     */
    @ConfigItem(defaultValue = "false")
    public boolean ssl;

    /**
     * Set whether all server certificates should be trusted.
     * This option is only used when {@link #ssl} is enabled.
     */
    @ConfigItem
    public Optional<Boolean> trustAll;

    /**
     * Sets the max number of open connections to the mail server.
     */
    @ConfigItem(defaultValue = "10")
    public int maxPoolSize;

    /**
     * Sets the hostname to be used for HELO/EHLO and the Message-ID.
     */
    @ConfigItem
    public Optional<String> ownHostName;

    /**
     * Sets if connection pool is enabled.
     * If the connection pooling is disabled, the max number of sockets is enforced nevertheless.
     */
    @ConfigItem(defaultValue = "true")
    public boolean keepAlive;

    /**
     * Disable ESMTP.
     *
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
    public String startTLS;

    /**
     * Sets the login mode for the connection.
     * Either {@code NONE}, @{code DISABLED}, {@code OPTIONAL}, {@code REQUIRED} or {@code XOAUTH2}.
     * <ul>
     * <li>DISABLED means no login will be attempted</li>
     * <li>NONE means a login will be attempted if the server supports in and login credentials are set</li>
     * <li>REQUIRED means that a login will be attempted if the server supports it and the send operation will fail
     * otherwise</li>
     * <li>XOAUTH2 means that a login will be attempted using Google Gmail Oauth2 tokens</li>
     * </ul>
     */
    @ConfigItem(defaultValue = "NONE")
    public String login;

    /**
     * Sets the allowed authentication methods.
     * These methods will be used only if the server supports them.
     * If not set, all supported methods may be used.
     *
     * The list is given as a space separated list, such as {@code DIGEST-MD5 CRAM-SHA256 CRAM-SHA1 CRAM-MD5 PLAIN LOGIN}.
     */
    @ConfigItem
    public Optional<String> authMethods;

    /**
     * Set the trust store.
     * 
     * @deprecated Use {{@link #truststore} instead.
     */
    @Deprecated
    @ConfigItem
    public Optional<String> keyStore;

    /**
     * Sets the trust store password if any.
     * 
     * @deprecated Use {{@link #truststore} instead.
     */
    @ConfigItem
    @Deprecated
    public Optional<String> keyStorePassword;

    /**
     * Configures the trust store.
     */
    @ConfigItem
    public TrustStoreConfig truststore;

    /**
     * Whether or not the mail should always been sent as multipart even if they don't have attachments.
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
    public boolean pipelining;

    /**
     * Sets the connection pool cleaner period.
     * Zero disables expiration checks and connections will remain in the pool until they are closed.
     */
    @ConfigItem(defaultValue = "PT1S")
    public Duration poolCleanerPeriod;

    /**
     * Set the keep alive timeout for the SMTP connection.
     * This value determines how long a connection remains unused in the pool before being evicted and closed.
     * A timeout of 0 means there is no timeout.
     */
    @ConfigItem(defaultValue = "PT300S")
    public Duration keepAliveTimeout;

    /**
     * Configures NTLM (Windows New Technology LAN Manager).
     */
    @ConfigItem
    public NtlmConfig ntlm;

}
