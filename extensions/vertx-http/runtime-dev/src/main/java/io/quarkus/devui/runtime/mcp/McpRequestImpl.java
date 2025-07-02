package io.quarkus.devui.runtime.mcp;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;

class McpRequestImpl implements McpRequest {

    private final String serverName;
    private final Object json;
    private final McpConnectionBase connection;
    private final Sender sender;
    private final ContextSupport requestContextSupport;

    private final ManagedContext requestContext;

    public McpRequestImpl(String serverName, Object json, McpConnectionBase connection, Sender sender,
            ContextSupport requestContextSupport) {
        this.serverName = serverName;
        this.json = json;
        this.connection = connection;
        this.sender = sender;
        this.requestContextSupport = requestContextSupport;
        this.requestContext = Arc.container().requestContext();
    }

    @Override
    public String serverName() {
        return serverName;
    }

    @Override
    public Object json() {
        return json;
    }

    @Override
    public McpConnectionBase connection() {
        return connection;
    }

    @Override
    public Sender sender() {
        return sender;
    }

    @Override
    public ContextSupport contextSupport() {
        return requestContextSupport;
    }

    @Override
    public void contextStart() {
        final ContextSupport contextSupport = contextSupport();
        if (!requestContext.isActive()) {
            requestContext.activate();
            if (contextSupport != null) {
                contextSupport.requestContextActivated();
            }
        }
    }

    @Override
    public void contextEnd() {
        requestContext.terminate();
    }

}
