package io.quarkus.devui.runtime.mcp;

import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;

/**
 * Normal Dev UI Json RPC Service for the Dev MPC Screen
 */
@ApplicationScoped
public class DevMcpJsonRpcService {

    private final BroadcastProcessor<McpClientInfo> connectedClientStream = BroadcastProcessor.create();

    // TODO: We need to be able to deregister a client if the connection drops. Mayby ping it ?

    private final Set<McpClientInfo> connectedClients = new HashSet<>();

    public Set<McpClientInfo> getConnectedClients() {
        if (!this.connectedClients.isEmpty()) {
            return this.connectedClients;
        }
        return null;
    }

    public Multi<McpClientInfo> getConnectedClientStream() {
        return connectedClientStream;
    }

    public void addClientInfo(McpClientInfo clientInfo) {
        this.connectedClients.add(clientInfo);
        this.connectedClientStream.onNext(clientInfo);
    }

}
