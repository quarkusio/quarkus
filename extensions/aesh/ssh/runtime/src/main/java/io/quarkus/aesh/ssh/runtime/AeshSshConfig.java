package io.quarkus.aesh.ssh.runtime;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration for the Aesh SSH terminal extension.
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.aesh.ssh")
public interface AeshSshConfig {

    /**
     * Whether the SSH terminal server is enabled.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * SSH server port.
     */
    @WithDefault("2222")
    int port();

    /**
     * SSH server bind address.
     */
    @WithDefault("localhost")
    String host();

    /**
     * Path to the host key file.
     * If the file does not exist, a new RSA key pair will be generated and saved to this path.
     */
    @WithDefault("hostkey.ser")
    String hostKeyFile();

    /**
     * Password for SSH authentication.
     * If not set, any password is accepted (suitable for development only).
     * <p>
     * For production, use public key authentication via {@code authorized-keys-file}
     * instead. If password authentication is needed, avoid storing the password
     * in plaintext in {@code application.properties}. Instead, reference an
     * environment variable:
     *
     * <pre>
     * quarkus.aesh.ssh.password=${SSH_PASSWORD}
     * </pre>
     */
    Optional<String> password();

    /**
     * Path to an OpenSSH-format authorized_keys file for public key authentication.
     * This is the recommended authentication method for production environments.
     * When set, clients can authenticate with a private key whose public key
     * is listed in this file. Can be used alongside password authentication.
     */
    Optional<String> authorizedKeysFile();

    /**
     * Maximum number of concurrent SSH sessions.
     * Zero means no limit.
     */
    @WithDefault("0")
    int maxConnections();

    /**
     * Idle timeout for SSH sessions. If a session has no input activity
     * for this duration, it will be closed. If not set, sessions can
     * remain idle indefinitely.
     */
    Optional<Duration> idleTimeout();
}
