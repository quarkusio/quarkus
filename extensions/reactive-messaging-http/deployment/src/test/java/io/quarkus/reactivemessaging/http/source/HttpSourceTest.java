package io.quarkus.reactivemessaging.http.source;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Arrays.asList;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.reactivemessaging.http.runtime.IncomingHttpMetadata;
import io.quarkus.reactivemessaging.http.source.app.Consumer;
import io.quarkus.reactivemessaging.utils.VertxFriendlyLock;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.response.ValidatableResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

class HttpSourceTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Consumer.class, VertxFriendlyLock.class))
            .withConfigurationResource("http-source-test-application.properties");

    @Inject
    Consumer consumer;

    @AfterEach
    void setUp() {
        consumer.clear();
    }

    @Test
    void shouldPassTextContentPathAndHeaders() {
        String headerName = "my-custom-header";
        String headerValue = "my-custom-header-value";
        // @formatter:off
        given()
                .header(headerName, headerValue)
                .body("some-text")
        .when()
                .post("/my-http-source")
        .then()
                .statusCode(202);
        // @formatter:on

        List<Message<?>> messages = consumer.getPostMessages();
        assertThat(messages).hasSize(1);
        Message<?> message = messages.get(0);
        assertThat(message.getPayload().toString()).isEqualTo("some-text");
        Optional<IncomingHttpMetadata> maybeMetadata = message.getMetadata(IncomingHttpMetadata.class);
        assertThat(maybeMetadata).isNotEmpty();
        IncomingHttpMetadata metadata = maybeMetadata.get();
        assertThat(metadata.getHeaders().get(headerName)).isEqualTo(headerValue);
        assertThat(metadata.getPath()).isEqualTo("/my-http-source");
        assertThat(metadata.getMethod()).isEqualTo(HttpMethod.POST);
    }

    @Test
    void shouldDifferentiatePostAndPut() {
        // @formatter:off
        given()
                .body("some-text")
        .when()
                .put("/my-http-source")
        .then()
                .statusCode(202);
        // @formatter:on

        send("some-text", "/my-http-source");
        assertThat(consumer.getPostMessages()).hasSize(1);
        assertThat(consumer.getPutMessages()).hasSize(1);
    }

    @Test
    void shouldConsumeHttpTwice() {
        send("some-text", "/my-http-source");

        send("some-text", "/my-http-source");
        List<Message<?>> messages = consumer.getPostMessages();
        assertThat(messages).hasSize(2);
    }

    @Test
    void shouldConsumeJsonObject() {
        send("{\"some\": \"json\"}", "/json-http-source");

        List<?> payloads = consumer.getPayloads();
        assertThat(payloads).hasSize(1);
        assertThat(payloads.get(0)).isInstanceOf(JsonObject.class);
        JsonObject payload = (JsonObject) payloads.get(0);
        assertThat(payload.getString("some")).isEqualTo("json");
    }

    @Test
    void shouldReportFailureOnInvalidJsonAndSucceedOnProperOne() {
        // TODO: change to assert 422 when proper propagation of failures is implemented
        assertThat(sendAndGetStatus("{\"some\": \"json}", "/json-http-source")).isEqualTo(500);
        send("{\"some\": \"json\"}", "/json-http-source");

        List<?> payloads = consumer.getPayloads();
        assertThat(payloads).hasSize(1);
        assertThat(payloads.get(0)).isInstanceOf(JsonObject.class);
        JsonObject payload = (JsonObject) payloads.get(0);
        assertThat(payload.getString("some")).isEqualTo("json");
    }

    @Test
    void shouldConsumeJsonArray() {
        send("[{\"some\": \"json\"}]", "/jsonarray-http-source");

        List<?> payloads = consumer.getPayloads();
        assertThat(payloads).hasSize(1);
        assertThat(payloads.get(0)).isInstanceOf(JsonArray.class);
        JsonArray payload = (JsonArray) payloads.get(0);
        assertThat(payload.getJsonObject(0).getString("some")).isEqualTo("json");
    }

    @Test
    void shouldConsumeString() {
        send("someString", "/string-http-source");

        List<?> payloads = consumer.getPayloads();
        assertThat(payloads).hasSize(1);
        assertThat(payloads.get(0)).isInstanceOf(String.class);
        String payload = (String) payloads.get(0);
        assertThat(payload).isEqualTo("someString");
    }

    @Test
    void shouldBuffer13MessagesIfConfigured() {
        // 1 message should start being consumed, 13 should be buffered, the rest should respond with 503
        consumer.pause();
        List<Future<Integer>> sendStates = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(17);
        for (int i = 0; i < 17; i++) {
            sendStates.add(executorService.submit(() -> sendAndGetStatus("some-text", "/my-http-source")));
        }

        await("assert 3 failures")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> countCodes(sendStates, 503), Predicate.isEqual(3L));

        consumer.resume();

        await("all processing finished")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> countCodes(sendStates, 503, 202), Predicate.isEqual(17L));

        assertThat(consumer.getPostMessages()).hasSize(14);
    }

    private long countCodes(List<Future<Integer>> sendStates, int... codes) {
        List<Integer> statusCodes = new ArrayList<>();
        for (Future<Integer> sendState : sendStates) {
            if (sendState.isDone()) {
                try {
                    statusCodes.add(sendState.get());
                } catch (InterruptedException | ExecutionException e) {
                    fail("checking the status code for http connection failed unexpectedly", e);
                }
            }
        }
        return statusCodes.stream().filter(asList(codes)::contains).count();
    }

    static int sendAndGetStatus(String body, String path) {
        return sendAndGetResponse(body, path).extract().statusCode();
    }

    static ValidatableResponse sendAndGetResponse(String body, String path) {
        return given().body(body)
                .when().post(path)
                .then();
    }

    static void send(String body, String path) {
        sendAndGetResponse(body, path).statusCode(202);
    }
}
