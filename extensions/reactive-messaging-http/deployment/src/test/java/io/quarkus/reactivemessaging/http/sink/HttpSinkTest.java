package io.quarkus.reactivemessaging.http.sink;

import static com.google.common.collect.Maps.immutableEntry;
import static io.restassured.RestAssured.given;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.reactivemessaging.http.runtime.OutgoingHttpMetadata;
import io.quarkus.reactivemessaging.http.sink.app.Dto;
import io.quarkus.reactivemessaging.http.sink.app.HttpEmitter;
import io.quarkus.reactivemessaging.http.sink.app.HttpEndpoint;
import io.quarkus.reactivemessaging.utils.ToUpperCaseSerializer;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

class HttpSinkTest {

    @Inject
    HttpEndpoint httpEndpoint;
    @Inject
    HttpEmitter emitter;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HttpEmitter.class, ToUpperCaseSerializer.class, HttpEndpoint.class, Dto.class))
            .withConfigurationResource("http-sink-test-application.properties");

    @AfterEach
    void cleanUp() {
        httpEndpoint.getRequests().clear();
    }

    @Test
    void shouldSendMessage() throws InterruptedException {
        emit(Buffer.buffer("{\"foo\": \"bar\"}"));
        assertThat(httpEndpoint.getRequests()).hasSize(1);
        String body = httpEndpoint.getRequests().get(0).getBody();
        assertThat(new JsonObject(body)).isEqualTo(new JsonObject().put("foo", "bar"));
    }

    @Test
    void shouldUseCustomSerializer() {
        // @formatter:off
        given()
                .body("some-text")
        .when()
                .post("/custom-http-source")
        .then()
                .statusCode(202);
        // @formatter:on
        await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> httpEndpoint.getRequests(), hasSize(1));
        String body = httpEndpoint.getRequests().get(0).getBody();
        assertThat(body).isEqualTo("SOME-TEXT");
    }

    // todo add content-type headers from serializer?
    @Test
    void shouldPassHeadersPathParamAndQueryParam() {
        String id = "10";
        OutgoingHttpMetadata metadata = new OutgoingHttpMetadata.Builder()
                .addHeader("myHeader", "myValue")
                .addPathParameter("id", id)
                .addQueryParameter("sort", "ASC")
                .build();
        emitter.emitMessageWithPathParam(Message.of(new Dto("foo")).addMetadata(metadata));

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> httpEndpoint.getIdentifiableRequests(), aMapWithSize(1));

        HttpEndpoint.Request request = httpEndpoint.getIdentifiableRequests().get(id);
        assertThat(new JsonObject(request.getBody())).isEqualTo(new JsonObject().put("field", "foo"));
        assertThat(request.getHeaders()).contains(immutableEntry("myHeader", singletonList("myValue")));
        assertThat(request.getQueryParameters()).contains(immutableEntry("sort", singletonList("ASC")));
    }

    @Test
    void shouldSerializeCollectionToJson() throws InterruptedException {
        emit(asList(new Dto("foo"), new Dto("bar")));

        assertThat(httpEndpoint.getRequests()).hasSize(1);
        String body = httpEndpoint.getRequests().get(0).getBody();
        assertThat(new JsonArray(body)).isEqualTo(new JsonArray("[{\"field\": \"foo\"}, {\"field\": \"bar\"}]"));
    }

    @Test
    void shouldSerializeObjectToJson() throws InterruptedException {
        emit(new Dto("fooo"));

        List<HttpEndpoint.Request> requests = httpEndpoint.getRequests();
        assertThat(requests).hasSize(1);
        String body = requests.get(0).getBody();
        assertThat(new JsonObject(body)).isEqualTo(new JsonObject("{\"field\": \"fooo\"}"));
    }

    @Test
    void shouldRetry() throws InterruptedException {
        httpEndpoint.setInitialFailures(1);
        emit(emitter::retryingEmitObject, new Dto("fooo"));

        List<HttpEndpoint.Request> requests = httpEndpoint.getRequests();
        assertThat(requests).hasSize(1);
        String body = requests.get(0).getBody();
        assertThat(new JsonObject(body)).isEqualTo(new JsonObject("{\"field\": \"fooo\"}"));
    }

    @Test
    void shouldNotRetryByDefault() throws InterruptedException {
        httpEndpoint.setInitialFailures(1);
        emit(new Dto("fooo"));
        emit(new Dto("fooo2"));

        List<HttpEndpoint.Request> requests = httpEndpoint.getRequests();
        assertThat(requests).hasSize(1);
        String body = requests.get(0).getBody();
        assertThat(new JsonObject(body)).isEqualTo(new JsonObject("{\"field\": \"fooo2\"}"));
    }

    private void emit(Object payload) throws InterruptedException {
        emit(emitter::emitObject, payload);
    }

    private void emit(Function<Object, CompletionStage<Void>> emitter, Object payload) throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        emitter.apply(payload).thenRun(done::countDown);
        done.await(10, TimeUnit.SECONDS);
    }

}
