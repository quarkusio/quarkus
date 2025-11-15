package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestStreamElementType;
import org.jboss.resteasy.reactive.common.util.RestMediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.web.ReactiveRoutes;
import io.quarkus.vertx.web.Route;
import io.smallrye.mutiny.Multi;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

public class MultiNdjsonTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(TestJacksonBasicMessageBodyReader.class));

    @TestHTTPResource
    URI uri;

    @Test
    void shouldReadNdjsonStringAsMulti() throws InterruptedException {
        var client = createClient(uri);
        var collected = new CopyOnWriteArrayList<String>();
        var completionLatch = new CountDownLatch(1);
        client.readString().onCompletion().invoke(completionLatch::countDown)
                .subscribe().with(collected::add);

        if (!completionLatch.await(5, TimeUnit.SECONDS)) {
            fail("Streaming did not complete in time");
        }
        assertThat(collected).hasSize(4)
                .contains("\"one\"", "\"two\"", "\"three\"", "\"four\"");
    }

    @Test
    void shouldReadNdjsonPojoAsMulti() throws InterruptedException {
        var client = createClient(uri);
        var collected = new CopyOnWriteArrayList<Message>();
        var completionLatch = new CountDownLatch(1);
        client.readPojo().onCompletion().invoke(completionLatch::countDown)
                .subscribe().with(collected::add);

        if (!completionLatch.await(5, TimeUnit.SECONDS)) {
            fail("Streaming did not complete in time");
        }
        var expected = Arrays.asList(Message.of("one", "1"),
                Message.of("two", "2"), Message.of("three", "3"),
                Message.of("four", "4"));
        assertThat(collected).hasSize(4).containsAll(expected);
    }

    @Test
    void shouldReadNdjsonPojoFromReactiveRoutes() throws InterruptedException {
        URI reactiveRoutesBaseUri = URI.create(uri.toString() + "/rr");
        var client = createClient(reactiveRoutesBaseUri);
        var collected = new CopyOnWriteArrayList<Message>();
        var completionLatch = new CountDownLatch(1);
        client.readPojo().onCompletion().invoke(completionLatch::countDown)
                .subscribe().with(collected::add);

        if (!completionLatch.await(5, TimeUnit.SECONDS)) {
            fail("Streaming did not complete in time");
        }
        var expected = Arrays.asList(Message.of("superman", "1"),
                Message.of("batman", "2"), Message.of("spiderman", "3"));
        assertThat(collected).hasSize(3).containsAll(expected);
    }

    @Test
    void shouldReadNdjsonFromSingleMessage() throws InterruptedException {
        var client = createClient(uri);
        var collected = new CopyOnWriteArrayList<Message>();
        var completionLatch = new CountDownLatch(1);
        client.readPojoSingle().onCompletion().invoke(completionLatch::countDown)
                .subscribe().with(collected::add);

        if (!completionLatch.await(5, TimeUnit.SECONDS)) {
            fail("Streaming did not complete in time");
        }
        var expected = Arrays.asList(
                Message.of("zero", "0"), Message.of("one", "1"),
                Message.of("two", "2"), Message.of("three", "3"));
        assertThat(collected).hasSize(4).containsAll(expected);
    }

    @Test
    void shouldReadNdjsonFromSingleMessageWithNoDelimiter() throws InterruptedException {
        var client = createClient(uri);
        var collected = new CopyOnWriteArrayList<Message>();
        var completionLatch = new CountDownLatch(1);
        client.readSingleMessageNoDelimiter().onCompletion().invoke(completionLatch::countDown)
                .subscribe().with(collected::add);

        if (!completionLatch.await(5, TimeUnit.SECONDS)) {
            fail("Streaming did not complete in time");
        }
        assertThat(collected).singleElement().satisfies(m -> assertThat(m).isEqualTo(Message.of("foo", "bar")));
    }

    @Test
    void shouldReadNdjsonFromMultipleMessagesWithNoEndingDelimiter() throws InterruptedException {
        var client = createClient(uri);
        var collected = new CopyOnWriteArrayList<Message>();
        var completionLatch = new CountDownLatch(1);
        client.readMultipleMessagesNoEndingDelimiter().onCompletion().invoke(completionLatch::countDown)
                .subscribe().with(collected::add);

        if (!completionLatch.await(5, TimeUnit.SECONDS)) {
            fail("Streaming did not complete in time");
        }
        assertThat(collected).hasSize(100);
    }

    @Test
    void shouldReadLargeNdjsonPojoAsMulti() throws InterruptedException {
        var client = createClient(uri);
        var collected = new CopyOnWriteArrayList<Message>();
        var completionLatch = new CountDownLatch(1);
        client.readLargePojo().onCompletion().invoke(completionLatch::countDown)
                .subscribe().with(collected::add);

        if (!completionLatch.await(5, TimeUnit.SECONDS)) {
            fail("Streaming did not complete in time");
        }

        assertThat(collected).hasSize(4);
    }

    private Client createClient(URI uri) {
        return QuarkusRestClientBuilder.newBuilder().baseUri(uri).register(new TestJacksonBasicMessageBodyReader())
                .build(Client.class);
    }

    @Path("/stream")
    public interface Client {

        @GET
        @Path("/string")
        @Produces(RestMediaType.APPLICATION_NDJSON)
        @RestStreamElementType(MediaType.APPLICATION_JSON)
        Multi<String> readString();

        @GET
        @Path("/pojo")
        @Produces(RestMediaType.APPLICATION_NDJSON)
        @RestStreamElementType(MediaType.APPLICATION_JSON)
        Multi<Message> readPojo();

        @GET
        @Path("/single-pojo")
        @Produces(RestMediaType.APPLICATION_NDJSON)
        @RestStreamElementType(MediaType.APPLICATION_JSON)
        Multi<Message> readPojoSingle();

        @GET
        @Path("/single-message-no-delimiter")
        @Produces(RestMediaType.APPLICATION_NDJSON)
        @RestStreamElementType(MediaType.APPLICATION_JSON)
        Multi<Message> readSingleMessageNoDelimiter();

        @GET
        @Path("multiple-messages-no-ending-delimiter")
        @Produces(RestMediaType.APPLICATION_NDJSON)
        @RestStreamElementType(MediaType.APPLICATION_JSON)
        Multi<Message> readMultipleMessagesNoEndingDelimiter();

        @GET
        @Path("/large-pojo")
        @Produces(RestMediaType.APPLICATION_NDJSON)
        @RestStreamElementType(MediaType.APPLICATION_JSON)
        Multi<Message> readLargePojo();

    }

    public static class ReactiveRoutesResource {

        @Route(path = "/rr/stream/pojo", produces = ReactiveRoutes.ND_JSON)
        Multi<Message> people(RoutingContext context) {
            return Multi.createFrom().items(
                    Message.of("superman", "1"),
                    Message.of("batman", "2"),
                    Message.of("spiderman", "3"));
        }
    }

    @Path("/stream")
    public static class StreamingResource {
        private final ObjectMapper mapper = new ObjectMapper();
        private final ObjectWriter messageWriter = mapper.writerFor(Message.class);

        private final Vertx vertx;

        public StreamingResource(Vertx vertx) {
            this.vertx = vertx;
        }

        @GET
        @Path("/string")
        @Produces(RestMediaType.APPLICATION_NDJSON)
        @RestStreamElementType(MediaType.APPLICATION_JSON)
        public Multi<String> readString() {
            return Multi.createFrom().emitter(
                    em -> {
                        em.emit("one");
                        em.emit("two");
                        em.emit("three");
                        em.emit("four");
                        em.complete();
                    });
        }

        @GET
        @Path("/pojo")
        @Produces(RestMediaType.APPLICATION_NDJSON)
        @RestStreamElementType(MediaType.APPLICATION_JSON)
        public Multi<Message> readPojo() {
            return Multi.createFrom().emitter(
                    em -> {
                        em.emit(Message.of("one", "1"));
                        em.emit(Message.of("two", "2"));
                        em.emit(Message.of("three", "3"));
                        vertx.setTimer(100, id -> {
                            em.emit(Message.of("four", "4"));
                            em.complete();
                        });
                    });
        }

        @GET
        @Path("/single-pojo")
        @Produces(RestMediaType.APPLICATION_NDJSON)
        @RestStreamElementType(MediaType.APPLICATION_JSON)
        public String getPojosAsString() throws JsonProcessingException {
            StringBuilder result = new StringBuilder();
            for (var msg : List.of(Message.of("zero", "0"),
                    Message.of("one", "1"),
                    Message.of("two", "2"),
                    Message.of("three", "3"))) {
                result.append(messageWriter.writeValueAsString(msg));
                result.append("\n");
            }
            return result.toString();
        }

        @GET
        @Path("/single-message-no-delimiter")
        @Produces(RestMediaType.APPLICATION_NDJSON)
        @RestStreamElementType(MediaType.APPLICATION_JSON)
        public String singleMessageNoDelimiter() throws JsonProcessingException {
            return messageWriter.writeValueAsString(Message.of("foo", "bar"));
        }

        @GET
        @Path("/multiple-messages-no-ending-delimiter")
        @Produces(RestMediaType.APPLICATION_NDJSON)
        @RestStreamElementType(MediaType.APPLICATION_JSON)
        public String multipleMessagesNoEndingDelimiter() throws JsonProcessingException {
            return IntStream.range(0, 100)
                    .mapToObj(i -> Message.of("foo", "bar"))
                    .map(m -> {
                        try {
                            return messageWriter.writeValueAsString(m);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    }).collect(Collectors.joining("\n"));
        }

        @GET
        @Path("/large-pojo")
        @Produces(RestMediaType.APPLICATION_NDJSON)
        @RestStreamElementType(MediaType.APPLICATION_JSON)
        public Multi<Message> readLargePojo() {
            return Multi.createFrom().emitter(
                    em -> {
                        byte[] bytes = new byte[4 * 1024];
                        Random random = new Random();
                        random.nextBytes(bytes);
                        String value = Base64.getEncoder().encodeToString(bytes);
                        em.emit(Message.of("one", value));
                        em.emit(Message.of("two", value));
                        em.emit(Message.of("three", value));
                        vertx.setTimer(100, id -> {
                            em.emit(Message.of("four", value));
                            em.complete();
                        });
                    });
        }
    }

    public static class Message {
        public String name;
        public String value;

        public static Message of(String name, String value) {
            Message message = new Message();
            message.name = name;
            message.value = value;
            return message;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Message message = (Message) o;
            return Objects.equals(name, message.name) && Objects.equals(value, message.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, value);
        }

        @Override
        public String toString() {
            return "Message{" +
                    "name='" + name + '\'' +
                    ", value='" + value + '\'' +
                    '}';
        }
    }
}
