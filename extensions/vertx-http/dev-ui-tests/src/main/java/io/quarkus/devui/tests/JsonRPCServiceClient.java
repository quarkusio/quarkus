package io.quarkus.devui.tests;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocketClientOptions;
import io.vertx.core.http.WebSocketConnectOptions;

/**
 * The client to invoke the RPC service.
 *
 * <h2>Example:</h2>
 *
 * To send a request and synchronously receive the response, just call:
 *
 * <pre>
 *     var response = client
 *          .request(...)
 *          .send()
 *          .get(5, TimeUnit.SECONDS)
 * </pre>
 */
public class JsonRPCServiceClient {

    private static final Logger log = Logger.getLogger(JsonRPCServiceClient.class);

    private final String namespace;
    private final URI uri;

    private final ObjectMapper mapper = new ObjectMapper();
    private final Random random = new Random();

    public JsonRPCServiceClient(final String namespace, DevUiResourceResolver resourceResolver) {
        this.namespace = namespace;
        this.uri = resourceResolver.resolve("json-rpc-ws");
    }

    public <T> Request<T> request(String method, TypeReference<T> type) throws Exception {
        return request(method, type, Map.of());
    }

    public Request<JsonNode> request(String method) throws Exception {
        return request(method, Map.of());
    }

    public Request<JsonNode> request(String method, Map<String, Object> params) throws Exception {
        return request(method, JsonNode.class, params);
    }

    public <T> Request<T> request(String method, Class<T> clazz) throws Exception {
        return request(method, clazz, null);
    }

    public <T> Request<T> request(String method, TypeReference<T> type, Map<String, Object> params)
            throws Exception {
        return request(method, params, createMapper(type));
    }

    public <T> Request<T> request(String method, Class<T> clazz, Map<String, Object> params) throws Exception {
        return request(method, params, createMapper(clazz));
    }

    @FunctionalInterface
    private interface JsonMapper<T> {
        T map(JsonNode node) throws IOException;
    }

    @SuppressWarnings("unchecked")
    private <T> JsonMapper<T> createMapper(final TypeReference<T> type) {
        final var javaType = mapper.getTypeFactory().constructType(type);
        return node -> {
            if (node == null) {
                return null;
            }
            return (T) mapper.treeToValue(node, javaType);
        };
    }

    @SuppressWarnings("unchecked")
    private <T> JsonMapper<T> createMapper(final Class<T> clazz) {
        return node -> {
            if (node == null) {
                return null;
            }
            if (clazz == null || clazz.equals(JsonNode.class)) {
                return (T) node;
            } else if (clazz.equals(String.class)) {
                return (T) node.asText();
            } else if (clazz.equals(Boolean.class)) {
                return (T) Boolean.valueOf(node.asBoolean());
            } else if (clazz.equals(Double.class)) {
                return (T) Double.valueOf(node.asDouble());
            } else if (clazz.equals(Integer.class)) {
                return (T) Integer.valueOf(node.asInt());
            } else if (clazz.equals(Long.class)) {
                return (T) Long.valueOf(node.asLong());
            } else {
                return mapper.treeToValue(node, clazz);
            }
        };
    }

    private String createRequestPayload(String method, Map<String, Object> params) throws IOException {

        final var payload = mapper.createObjectNode();
        payload.put("jsonrpc", "2.0");
        payload.put("id", random.nextInt());
        payload.put("method", this.namespace + "." + method);

        final var paramsPayload = mapper.createObjectNode();
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, Object> p : params.entrySet()) {
                final var jsonValue = mapper.convertValue(p.getValue(), JsonNode.class);
                paramsPayload.putIfAbsent(p.getKey(), jsonValue);
            }
        }
        payload.set("params", paramsPayload);

        return mapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(payload);

    }

    /**
     * The object to send a request.
     *
     * @param <T> the response type
     */
    @FunctionalInterface
    public interface Request<T> {
        /**
         * Sends the request.
         *
         * @return a {@link CompletableFuture} that can be used to block until the response was received or an error occured.
         * @see CompletableFuture#get(long, TimeUnit)
         */
        CompletableFuture<T> send();
    }

    private <T> Request<T> request(String method, Map<String, Object> params, JsonMapper<T> mapper) throws IOException {
        final var payload = createRequestPayload(method, params);
        log.debug("request = " + method);

        final var options = new WebSocketClientOptions()
                .setDefaultHost(this.uri.getHost())
                .setDefaultPort(this.uri.getPort());

        final var socketOptions = new WebSocketConnectOptions()
                .setHost(this.uri.getHost())
                .setPort(this.uri.getPort())
                .setURI(this.uri.getPath())
                .setTimeout(3000);
        return () -> {
            final var result = new CompletableFuture<T>();
            final var vertx = Vertx.vertx();
            final var client = vertx.createWebSocketClient(options);
            client.connect(socketOptions, socketHandler -> {
                if (socketHandler.succeeded()) {
                    final var socket = socketHandler.result();
                    socket.writeTextMessage(payload);
                    socket.textMessageHandler(text -> {
                        try {
                            final var node = (ObjectNode) JsonRPCServiceClient.this.mapper.readTree(text);
                            final var resultNode = node.get("result");
                            result.complete(
                                    resultNode != null
                                            ? mapper.map(resultNode.get("object"))
                                            : null);
                        } catch (Exception e) {
                            result.completeExceptionally(e);
                        }

                    });
                    socket.exceptionHandler(ex -> {
                        result.completeExceptionally(ex);
                        vertx.close();
                    });
                    socket.closeHandler(v -> vertx.close());
                } else {
                    result.completeExceptionally(socketHandler.cause());
                    vertx.close();
                }
            });
            return result;
        };

    }

}
