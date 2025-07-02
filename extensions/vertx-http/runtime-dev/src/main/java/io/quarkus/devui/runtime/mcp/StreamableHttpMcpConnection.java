package io.quarkus.devui.runtime.mcp;

import org.jboss.logging.Logger;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

class StreamableHttpMcpConnection extends McpConnectionBase {

    private static final Logger LOG = Logger.getLogger(StreamableHttpMcpConnection.class);

    StreamableHttpMcpConnection(String id, McpServerRuntimeConfig config) {
        super(id, config);
    }

    @Override
    public Future<Void> send(JsonObject message) {
        String method = message.getString("method");
        LOG.warnf("Discarding message [%s]- 'subsidiary' SSE streams are not supported yet", method);
        return Future.succeededFuture();
    }

}
