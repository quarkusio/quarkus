package io.quarkus.aesh.ssh.runtime;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

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
     */
    Optional<String> password();

    /**
     * Path to an OpenSSH-format authorized_keys file for public key authentication.
     * When set, clients can authenticate with a private key whose public key
     * is listed in this file. Can be used alongside password authentication.
     */
    Optional<String> authorizedKeysFile();

    /**
     * Maximum number of concurrent SSH sessions.
     * If not set or &lt;= 0, there is no limit.
     */
    OptionalInt maxConnections();

    /**
     * Idle timeout for SSH sessions. If a session has no input activity
     * for this duration, it will be closed. If not set, sessions can
     * remain idle indefinitely.
     */
    Optional<Duration> idleTimeout();
}
