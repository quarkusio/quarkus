package io.quarkus.devui.tests;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class DevUIJsonRPCTest {

    protected static final Logger log = Logger.getLogger(DevUIJsonRPCTest.class);

    protected URI uri;

    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonFactory factory = mapper.getFactory();
    private final Random random = new Random();

    protected abstract String getNamespace();

    public DevUIJsonRPCTest() {
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
        try (Session session = ContainerProvider.getWebSocketContainer().connectToServer(Client.class, uri)) {
            Assertions.assertEquals("CONNECT", MESSAGES.poll(10, TimeUnit.SECONDS));

            int id = random.nextInt(Integer.MAX_VALUE);

            String request = createJsonRPCRequest(id, methodName, params);
            log.debug("request = " + request);
            session.getAsyncRemote().sendText(request);

            JsonNode response = parseJsonRPCResponse(id);
            log.debug("response = " + response.toPrettyString());

            return response;
        }
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
                return jsonResponse.get("result").get("object");
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
        request.put("method", getNamespace() + "." + methodName);
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

    @ClientEndpoint
    public static class Client {

        @OnOpen
        public void open(Session session) {
            MESSAGES.add("CONNECT");
        }

        @OnMessage
        void message(String msg) {
            MESSAGES.add(msg);
        }
    }

}
