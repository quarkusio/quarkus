package io.quarkus.jfr.deployment.http;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Multi;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

public class JfrRestClientTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Resource.class, HelloClient.class, UnreachableClient.class, StreamingClient.class))
            .overrideConfigKey("quarkus.rest-client.hello.url", "${test.url}")
            .overrideConfigKey("quarkus.rest-client.unreachable.url", "http://localhost:1")
            .overrideConfigKey("quarkus.rest-client.streaming.url", "${test.url}");

    @Test
    public void test() throws IOException {
        final java.nio.file.Path dumpPath = java.nio.file.Path.of("./dump.jfr");
        try {
            try (Recording r = new Recording()) {
                r.start();

                // 1) server resource /exercise calls HelloClient#hello("world") -> GET /hello/world on this server
                when().get("/exercise").then().statusCode(200).body(is("Hello world"));
                // 2) server resource /exercise-failure calls UnreachableClient#hello("world") -> GET http://localhost:1/hello/world,
                // the connection is refused and the resource swallows the ProcessingException
                when().get("/exercise-failure").then().statusCode(200).body(is("failed"));
                // 3) server resource /exercise-stream calls StreamingClient#stream() -> GET /stream, consumed as an SSE Multi
                when().get("/exercise-stream").then().statusCode(200).body(is("a,b,c"));

                r.stop();
                r.dump(dumpPath);
            }
            List<RecordedEvent> recordedEvents = RecordingFile.readAllEvents(dumpPath);

            List<RecordedEvent> clientEvents = recordedEvents.stream()
                    .filter(e -> e.getEventType().getName().equals("quarkus.RestClient")).toList();
            // only 1) and 2) produce an event, the streaming invocation 3) is not recorded
            assertThat(clientEvents).hasSize(2);

            // 1) the successful call: GET /hello/world made by the "hello" client
            RecordedEvent success = clientEvents.stream().filter(e -> e.getInt("status") == 200).findFirst().orElseThrow();
            assertThat(success.getString("httpMethod")).isEqualTo("GET");
            // the URI is the @Path template of HelloClient#hello, not the concrete /hello/world path
            assertThat(success.getString("uri")).isEqualTo("/hello/{name}");
            assertThat(success.getString("address")).startsWith("localhost:");
            assertThat(success.getString("clientName")).isEqualTo("hello");
            assertThat(success.getString("failure")).isNull();
            assertThat(success.getDuration()).isPositive();

            // the client event shares the trace ID of the quarkus.Rest server event for GET /exercise that triggered it
            RecordedEvent server = recordedEvents.stream()
                    .filter(e -> e.getEventType().getName().equals("quarkus.Rest"))
                    .filter(e -> e.getString("uri").equals("/exercise"))
                    .findFirst().orElseThrow();
            assertThat(success.getString("traceId")).isNotNull().isEqualTo(server.getString("traceId"));

            // 2) the failed call: GET http://localhost:1/hello/world made by the "unreachable" client, no response received
            RecordedEvent failure = clientEvents.stream().filter(e -> e.getInt("status") == 0).findFirst().orElseThrow();
            assertThat(failure.getString("httpMethod")).isEqualTo("GET");
            // the @Path template is recorded even though the request never reached a server
            assertThat(failure.getString("uri")).isEqualTo("/hello/{name}");
            assertThat(failure.getString("address")).isEqualTo("localhost:1");
            assertThat(failure.getString("clientName")).isEqualTo("unreachable");
            assertThat(failure.getString("failure")).contains("ConnectException");
        } finally {
            Files.deleteIfExists(dumpPath);
        }
    }

    @RegisterRestClient(configKey = "hello")
    public interface HelloClient {

        @GET
        @Path("/hello/{name}")
        String hello(@PathParam("name") String name);
    }

    @RegisterRestClient(configKey = "unreachable")
    public interface UnreachableClient {

        @GET
        @Path("/hello/{name}")
        String hello(@PathParam("name") String name);
    }

    @RegisterRestClient(configKey = "streaming")
    public interface StreamingClient {

        @GET
        @Path("/stream")
        @Produces(MediaType.SERVER_SENT_EVENTS)
        Multi<String> stream();
    }

    @Path("/")
    public static class Resource {

        @RestClient
        HelloClient helloClient;

        @RestClient
        UnreachableClient unreachableClient;

        @RestClient
        StreamingClient streamingClient;

        @GET
        @Path("/hello/{name}")
        public String hello(@PathParam("name") String name) {
            return "Hello " + name;
        }

        @GET
        @Path("/stream")
        @Produces(MediaType.SERVER_SENT_EVENTS)
        public Multi<String> stream() {
            return Multi.createFrom().items("a", "b", "c");
        }

        @GET
        @Path("/exercise")
        public String exercise() {
            return helloClient.hello("world");
        }

        @GET
        @Path("/exercise-failure")
        public String exerciseFailure() {
            try {
                return unreachableClient.hello("world");
            } catch (ProcessingException e) {
                return "failed";
            }
        }

        @GET
        @Path("/exercise-stream")
        public String exerciseStream() {
            return String.join(",", streamingClient.stream().collect().asList().await().atMost(Duration.ofSeconds(5)));
        }
    }
}
