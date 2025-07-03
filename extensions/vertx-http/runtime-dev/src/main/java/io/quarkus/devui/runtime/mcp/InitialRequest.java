package io.quarkus.devui.runtime.mcp;

import java.util.List;

/**
 * Represents the initial request sent from a client.
 *
 * @param implementation the client implementation information (must not be {@code null})
 * @param protocolVersion the protocol version supported by the client (must not be {@code null})
 * @param clientCapabilities the capabilities supported by the client (must not be {@code null})
 * @param transport the transport used by the client (must not be {@code null})
 */
record InitialRequest(Implementation implementation, String protocolVersion,
        List<ClientCapability> clientCapabilities, Transport transport) {

    InitialRequest {
        if (implementation == null) {
            throw new IllegalArgumentException("implementation must not be null");
        }
        if (protocolVersion == null) {
            throw new IllegalArgumentException("protocolVersion must not be null");
        }
        if (clientCapabilities == null) {
            throw new IllegalArgumentException("clientCapabilities must not be null");
        }
        if (transport == null) {
            throw new IllegalArgumentException("transport must not be null");
        }
    }

    /**
     * @return {@code true} if the client supports the {@link ClientCapability#SAMPLING} capability
     */
    public boolean supportsSampling() {
        return supportsCapability(ClientCapability.SAMPLING);
    }

    /**
     * @return {@code true} if the client supports the {@link ClientCapability#ROOTS} capability
     */
    public boolean supportsRoots() {
        return supportsCapability(ClientCapability.ROOTS);
    }

    /**
     * @return {@code true} if the client supports the specified capability
     */
    public boolean supportsCapability(String name) {
        for (ClientCapability capability : clientCapabilities) {
            if (capability.name().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public enum Transport {
        /**
         * The stdio transport.
         */
        STDIO,
        /**
         * The HTTP/SSE transport from version 2024-11-05.
         */
        SSE,
        /**
         * The Streamable HTTP transport from version 2025-03-26.
         */
        STREAMABLE_HTTP
    }

}
