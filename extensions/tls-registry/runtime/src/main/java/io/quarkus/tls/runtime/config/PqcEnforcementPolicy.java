package io.quarkus.tls.runtime.config;

public enum PqcEnforcementPolicy {

    /**
     * A connection is refused unless both the client and the server negotiate a PQC key exchange
     * ({@code X25519MLKEM768}). Clients that do not advertise PQC support are rejected during the TLS handshake.
     */
    STRICT,

    /**
     * The server advertises PQC key exchange groups but does not require the client to support them.
     * If the client does not support PQC, the handshake falls back to classical key exchange.
     */
    CLIENT_NEGOTIATED,

    /**
     * No PQC enforcement; standard TLS key exchange negotiation applies.
     * The server neither advertises nor requires PQC groups.
     */
    RELAXED
}
