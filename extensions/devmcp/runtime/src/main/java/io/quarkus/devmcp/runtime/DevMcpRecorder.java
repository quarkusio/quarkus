package io.quarkus.devmcp.runtime;

import static io.quarkus.vertx.runtime.jackson.JsonUtil.BASE64_DECODER;
import static io.quarkus.vertx.runtime.jackson.JsonUtil.BASE64_ENCODER;

import java.util.Map;

import jakarta.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devjsonrpc.runtime.jsonrpc.json.JsonMapper;
import io.quarkus.devjsonrpc.runtime.jsonrpc.json.JsonTypeAdapter;
import io.quarkus.devmcp.spi.McpServerConfiguration;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class DevMcpRecorder {
    private static final Logger LOG = Logger.getLogger(DevMcpRecorder.class);
    public static final String DEV_MANAGER_GLOBALS_JSON_MAPPER_FACTORY = "devjsonrpc-databind-codec-builder";

    public Handler<RoutingContext> mcpStreamableHTTPHandler(String quarkusVersion) {
        return new McpHttpHandler(quarkusVersion, createJsonMapper());
    }

    private JsonMapper createJsonMapper() {
        // We use a codec defined in the deployment module
        // because that module always has access to Jackson-Databind regardless of the application dependencies.
        JsonMapper.Factory factory = JsonMapper.Factory.deploymentLinker().createLink(
                DevConsoleManager.getGlobal(DEV_MANAGER_GLOBALS_JSON_MAPPER_FACTORY));
        // We need to pass some information so that the mapper, who lives in the deployment classloader,
        // knows how to deal with JsonObject/JsonArray/JsonBuffer, who live in the runtime classloader.
        return factory.create(new JsonTypeAdapter<>(JsonObject.class, JsonObject::getMap, JsonObject::new),
                new JsonTypeAdapter<>(JsonArray.class, JsonArray::getList, JsonArray::new),
                new JsonTypeAdapter<>(Buffer.class, buffer -> BASE64_ENCODER.encodeToString(buffer.getBytes()), text -> {
                    try {
                        return Buffer.buffer(BASE64_DECODER.decode(text));
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Expected a base64 encoded byte array, got: " + text, e);
                    }
                }));
    }

    public void populateBuildTimeData(BeanContainer beanContainer,
            Map<String, String> urlAndPath,
            Map<String, String> descriptions,
            Map<String, String> mcpDefaultEnabled,
            Map<String, String> contentTypes) {
        McpBuildTimeData buildTimeData = beanContainer.beanInstance(McpBuildTimeData.class);
        buildTimeData.addData(urlAndPath, descriptions, mcpDefaultEnabled, contentTypes);
    }

    public void logDevMcpEndpoint(String path) {
        McpServerConfiguration config = CDI.current().select(McpServerConfiguration.class).get();
        if (config.isEnabled()) {
            LOG.infof("Dev MCP available at: %s", path);
        }
    }
}
