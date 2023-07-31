package io.quarkus.devui.tests;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketConnectOptions;

public abstract class DevUIJsonRPCTest {

    protected static final Logger log = Logger.getLogger(DevUIJsonRPCTest.class);

    protected URI uri;

    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonFactory factory = mapper.getFactory();
    private final Random random = new Random();

    private final String namespace;

    public DevUIJsonRPCTest(String namespace) {
        this.namespace = namespace;
        String testUrl = ConfigProvider.getConfig().getValue("test.url", String.class);
        String nonApplicationRoot = ConfigProvider.getConfig()
                .getOptionalValue("quarkus.http.non-application-root-path", String.class).orElse("q");
        if (!nonApplicationRoot.startsWith("/")) {
            nonApplicationRoot = "/" + nonApplicationRoot;
        }
        this.uri = URI.create(testUrl + nonApplicationRoot + "/dev-ui/json-rpc-ws");
    }

    public JsonNode executeJsonRPCMethod(String methodName) throws Exception {
        return executeJsonRPCMethod(methodName, null);
    }

    public JsonNode executeJsonRPCMethod(String methodName, Map<String, String> params) throws Exception {
        int id = random.nextInt(Integer.MAX_VALUE);
        String request = createJsonRPCRequest(id, methodName, params);
        log.debug("request = " + request);

        Vertx vertx = Vertx.vertx();

        HttpClientOptions options = new HttpClientOptions()
                .setDefaultHost(uri.getHost())
                .setDefaultPort(uri.getPort());

        HttpClient client = vertx.createHttpClient(options);

        WebSocketConnectOptions socketOptions = new WebSocketConnectOptions()
                .setHost(uri.getHost())
                .setPort(uri.getPort())
                .setURI(uri.getPath());

        client.webSocket(socketOptions, ar -> {
            if (ar.succeeded()) {
                WebSocket socket = ar.result();
                Buffer accumulatedBuffer = Buffer.buffer();

                socket.frameHandler((e) -> {
                    Buffer b = accumulatedBuffer.appendBuffer(e.binaryData());
                    if (e.isFinal()) {
                        MESSAGES.add(b.toString());
                    }
                });

                socket.writeTextMessage(request);

                socket.exceptionHandler((e) -> {
                    e.printStackTrace();
                    vertx.close();
                });
                socket.closeHandler(v -> {
                    vertx.close();
                });
            } else {
                vertx.close();
            }
        });

        JsonNode response = parseJsonRPCResponse(id);
        log.debug("response = " + response.toPrettyString());
        vertx.close();
        return response;
    }

    protected JsonNode toJsonNode(String json) {
        try {
            JsonParser parser = factory.createParser(json);
            return mapper.readTree(parser);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private JsonNode parseJsonRPCResponse(int id) throws InterruptedException, IOException {
        return parseJsonRPCResponse(id, 0);
    }

    private JsonNode parseJsonRPCResponse(int id, int loopCount) throws InterruptedException, IOException {
        String response = MESSAGES.poll(10, TimeUnit.SECONDS);
        JsonNode jsonResponse = toJsonNode(response);
        if (jsonResponse.isObject()) {
            int responseId = jsonResponse.get("id").asInt();
            if (responseId == id) {

                JsonNode result = jsonResponse.get("result");
                JsonNode error = jsonResponse.get("error");

                if (result != null) {
                    return result.get("object");
                } else if (error != null) {
                    String errorMessage = error.get("message").asText();
                    throw new RuntimeException(errorMessage);
                } else {
                    throw new RuntimeException("No result from json-rpc backend \n " + jsonResponse.toPrettyString());
                }
            }
        }

        if (loopCount > 10)
            throw new RuntimeException("Too many recursions, message not returned for id [" + id + "]");
        return parseJsonRPCResponse(id, loopCount + 1);
    }

    private String createJsonRPCRequest(int id, String methodName, Map<String, String> params) throws IOException {

        ObjectNode request = mapper.createObjectNode();

        request.put("jsonrpc", "2.0");
        request.put("id", id);
        request.put("method", this.namespace + "." + methodName);
        ObjectNode jsonParams = mapper.createObjectNode();
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, String> p : params.entrySet()) {
                jsonParams.put(p.getKey(), p.getValue());
            }
        }
        request.set("params", jsonParams);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
    }

    private static final LinkedBlockingDeque<String> MESSAGES = new LinkedBlockingDeque<>();

}
