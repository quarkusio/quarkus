package io.quarkus.reactivemessaging.websocket.sink;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.reactivemessaging.utils.ToUpperCaseSerializer;
import io.quarkus.reactivemessaging.websocket.sink.app.WebSocketEmitter;
import io.quarkus.reactivemessaging.websocket.sink.app.WebSocketEndpoint;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

class WebSocketSinkTest {
    private static final Logger log = Logger.getLogger(WebSocketSinkTest.class);

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(WebSocketEndpoint.class, WebSocketEmitter.class, ToUpperCaseSerializer.class))
            .withConfigurationResource("websocket-sink-test-application.properties");

    @Inject
    WebSocketEndpoint webSocketEndpoint;

    @Inject
    WebSocketEmitter emitter;

    @Test
    void shouldSerializeBuffer() {
        log.debug("shouldSerializeBuffer");
        emitter.sendMessage(Message.of(Buffer.buffer("{\"foo\": \"bar\"}")));
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> webSocketEndpoint.getMessages(), hasSize(1));
        assertThat(webSocketEndpoint.getMessages().get(0)).isEqualTo("{\"foo\": \"bar\"}");
    }

    @Test
    void shouldSerializeJsonObject() {
        log.debug("shouldSerializeJsonObject");
        emitter.sendMessage(Message.of(new JsonObject().put("jsonFoo", "jsonBar")));
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> webSocketEndpoint.getMessages(), hasSize(1));
        assertThat(new JsonObject(webSocketEndpoint.getMessages().get(0)))
                .isEqualTo(new JsonObject("{\"jsonFoo\": \"jsonBar\"}"));
    }

    @Test
    void shouldSerializeJsonArray() {
        log.debug("shouldSerializeJsonArray");
        emitter.sendMessage(Message.of(new JsonArray().add(new JsonObject().put("arrFoo", "arrBar"))));
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> webSocketEndpoint.getMessages(), hasSize(1));
        assertThat(new JsonArray(webSocketEndpoint.getMessages().get(0)))
                .isEqualTo(new JsonArray().add(new JsonObject().put("arrFoo", "arrBar")));
    }

    @Test
    void shouldSerializeString() {
        log.debug("shouldSerializeString");
        emitter.sendMessage(Message.of("someText"));
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> webSocketEndpoint.getMessages(), hasSize(1));
        assertThat(webSocketEndpoint.getMessages().get(0)).isEqualTo("someText");
    }

    @Test
    void shouldUseCustomSerializer() {
        log.debug("shouldUseCustomSerializer");
        emitter.sendMessageWithCustomSerializer(Message.of("sometext"));
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> webSocketEndpoint.getMessages(), hasSize(1));
        assertThat(webSocketEndpoint.getMessages().get(0)).isEqualTo("SOMETEXT");
    }

    @Test
    void shouldReuseClientIfConnected() {
        log.debug("shouldReuseClientIfConnected");
        emitter.sendMessage(Message.of("sometext"));
        emitter.sendMessage(Message.of("sometext"));
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> webSocketEndpoint.getMessages(), hasSize(2));
        assertThat(webSocketEndpoint.sessionCount()).isEqualTo(1);
    }

    @Test
    void shouldReconnectOnClientDisconnected() {
        log.debug("shouldReconnectOnClientDisconnected");
        emitter.sendMessage(Message.of("sometext"));
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> webSocketEndpoint.getMessages(), hasSize(1));
        webSocketEndpoint.killAllSessions();
        emitter.sendMessage(Message.of("sometext"));
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> webSocketEndpoint.getMessages(), hasSize(2));
        assertThat(webSocketEndpoint.sessionCount()).isEqualTo(1);
    }

    // TODO: test retry mechanism when STOMP or similar protocol is implemented

    @AfterEach
    void cleanUp() {
        webSocketEndpoint.reset();
    }

}
