package io.quarkus.reactive.mssql.client.runtime;

import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.vertx.mssqlclient.EncryptionMode;

@ConfigGroup
public interface DataSourceReactiveMSSQLConfig {

    /**
     * The desired size (in bytes) for TDS packets.
     */
    OptionalInt packetSize();

    /**
     * Whether SSL/TLS is enabled.
     */
    @WithDefault("false")
    public boolean ssl();

    /**
     * The encryption mode for the connection.
     * <p>
     * Determines the TDS protocol version and whether TLS encryption is used:
     * <ul>
     * <li>{@link EncryptionMode#OFF} — no encryption, TDS 7.x.</li>
     * <li>{@link EncryptionMode#ON} — optional encryption, TDS 7.x. Equivalent to setting {@code ssl=true}.</li>
     * <li>{@link EncryptionMode#STRICT} — mandatory encryption, TDS 8.0.</li>
     * </ul>
     * When this option is set it takes precedence over {@link #ssl()}.
     */
    Optional<EncryptionMode> encryptionMode();

}
