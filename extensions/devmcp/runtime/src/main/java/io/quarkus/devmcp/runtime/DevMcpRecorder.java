package io.quarkus.devmcp.runtime;

import static io.quarkus.vertx.runtime.jackson.JsonUtil.BASE64_DECODER;
import static io.quarkus.vertx.runtime.jackson.JsonUtil.BASE64_ENCODER;

import java.util.List;

import jakarta.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import io.quarkus.devjsonrpc.runtime.DevJsonRpcRecorder;
import io.quarkus.devjsonrpc.runtime.jsonrpc.json.JsonMapper;
import io.quarkus.devjsonrpc.runtime.jsonrpc.json.JsonTypeAdapter;
import io.quarkus.devmcp.spi.McpServerConfiguration;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class DevMcpRecorder {
    private static final Logger LOG = Logger.getLogger(DevMcpRecorder.class);

    public Handler<RoutingContext> createLocalHostOnlyFilter(List<String> hosts) {
        return new DevMcpLocalHostOnlyFilter(hosts);
    }

    public Handler<RoutingContext> createCorsFilter(List<String> hosts) {
        return new DevMcpCORSFilter(hosts);
    }

    public Handler<RoutingContext> mcpStreamableHTTPHandler(String quarkusVersion, RuntimeValue<JsonMapper> jsonMapper) {
        return new McpHttpHandler(quarkusVersion, jsonMapper.getValue());
    }

    /**
     * Creates the JSON mapper used by the JsonRPC router and the MCP endpoint, with adapters for the Vert.x JSON types.
     */
    public RuntimeValue<JsonMapper> createJsonMapper() {
        JsonMapper.Factory factory = DevJsonRpcRecorder.createJsonMapperFactory();
        return new RuntimeValue<>(factory.create(new JsonTypeAdapter<>(JsonObject.class, JsonObject::getMap, JsonObject::new),
                new JsonTypeAdapter<>(JsonArray.class, JsonArray::getList, JsonArray::new),
                new JsonTypeAdapter<>(Buffer.class, buffer -> BASE64_ENCODER.encodeToString(buffer.getBytes()), text -> {
                    try {
                        return Buffer.buffer(BASE64_DECODER.decode(text));
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Expected a base64 encoded byte array, got: " + text, e);
                    }
                })));
    }

    public void logDevMcpEndpoint(String path) {
        McpServerConfiguration config = CDI.current().select(McpServerConfiguration.class).get();
        if (config.isEnabled()) {
            LOG.infof("Dev MCP available at: %s", path);
        }
    }
}
