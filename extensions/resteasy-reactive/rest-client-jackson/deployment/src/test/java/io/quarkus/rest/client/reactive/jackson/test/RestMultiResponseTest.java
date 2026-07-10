package io.quarkus.rest.client.reactive.jackson.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

import org.jboss.resteasy.reactive.client.BasicRestResponse;
import org.jboss.resteasy.reactive.client.RestMultiResponse;
import org.jboss.resteasy.reactive.client.SseEvent;
import org.jboss.resteasy.reactive.server.jackson.JacksonBasicMessageBodyReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class RestMultiResponseTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withEmptyApplication();

    @TestHTTPResource
    URI uri;

    @Test
    void shouldConsumeStreamAndAccessResponseMetadata() {
        RestMultiResponse<SseEvent<Dto>> stream = createClient().stream();

        var response = new AtomicReference<BasicRestResponse>();
        stream.response().subscribe().with(response::set);

        var resultList = new CopyOnWriteArrayList<Dto>();
        stream.subscribe().with(event -> resultList.add(event.data()));

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(response.get()).isNotNull();
                    assertThat(response.get().status()).isEqualTo(200);
                    assertThat(response.get().headers()).isNotNull();
                    assertThat(resultList).containsExactly(
                            new Dto("foo", "1"),
                            new Dto("bar", "2"));
                });
    }

    @Test
    void shouldWorkWithoutProducesAnnotation() {
        RestMultiResponse<SseEvent<Dto>> stream = createClient().streamNoProduces();

        var response = new AtomicReference<BasicRestResponse>();
        stream.response().subscribe().with(response::set);

        var resultList = new CopyOnWriteArrayList<Dto>();
        stream.subscribe().with(event -> resultList.add(event.data()));

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(response.get()).isNotNull();
                    assertThat(response.get().status()).isEqualTo(200);
                    assertThat(resultList).containsExactly(
                            new Dto("foo", "1"),
                            new Dto("bar", "2"));
                });
    }

    @Test
    void shouldConsumeSimpleTypeStream() {
        RestMultiResponse<String> stream = createClient().streamStrings();

        var response = new AtomicReference<BasicRestResponse>();
        stream.response().subscribe().with(response::set);

        var resultList = new CopyOnWriteArrayList<String>();
        stream.subscribe().with(resultList::add);

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(response.get()).isNotNull();
                    assertThat(response.get().status()).isEqualTo(200);
                    assertThat(resultList).containsExactly("hello", "world");
                });
    }

    private StreamClient createClient() {
        return QuarkusRestClientBuilder.newBuilder()
                .baseUri(uri)
                .register(new JacksonBasicMessageBodyReader(new ObjectMapper()))
                .build(StreamClient.class);
    }

    @Path("/sse-stream")
    public interface StreamClient {
        @GET
        @Path("/event")
        @Produces(MediaType.SERVER_SENT_EVENTS)
        RestMultiResponse<SseEvent<Dto>> stream();

        @GET
        @Path("/event")
        RestMultiResponse<SseEvent<Dto>> streamNoProduces();

        @GET
        @Path("/string")
        RestMultiResponse<String> streamStrings();
    }

    @Path("/sse-stream")
    public static class SseStreamResource {

        @GET
        @Path("/event")
        @Produces(MediaType.SERVER_SENT_EVENTS)
        public void event(@Context SseEventSink sink, @Context Sse sse) throws IOException {
            try (sink) {
                sink.send(sse.newEventBuilder()
                        .id("1")
                        .mediaType(MediaType.APPLICATION_JSON_TYPE)
                        .data(Dto.class, new Dto("foo", "1"))
                        .name("event1")
                        .build());
                sink.send(sse.newEventBuilder()
                        .id("2")
                        .mediaType(MediaType.APPLICATION_JSON_TYPE)
                        .data(Dto.class, new Dto("bar", "2"))
                        .name("event2")
                        .build());
            }
        }

        @GET
        @Path("/string")
        @Produces(MediaType.SERVER_SENT_EVENTS)
        public io.smallrye.mutiny.Multi<String> strings() {
            return io.smallrye.mutiny.Multi.createFrom().items("hello", "world");
        }
    }

    public static class Dto {
        public String name;
        public String value;

        public Dto(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public Dto() {
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Dto dto = (Dto) o;
            return Objects.equals(name, dto.name) && Objects.equals(value, dto.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, value);
        }
    }
}
