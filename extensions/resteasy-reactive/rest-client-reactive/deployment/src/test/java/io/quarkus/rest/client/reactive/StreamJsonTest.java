package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
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

public class StreamJsonTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest();

    @TestHTTPResource
    URI uri;

    @Test
    void shouldReadStreamJsonStringAsMulti() throws InterruptedException {
        var client = RestClientBuilder.newBuilder().baseUri(uri)
                .build(Client.class);
        var collected = new CopyOnWriteArrayList<String>();
        var completionLatch = new CountDownLatch(1);
        client.readString().onCompletion().invoke(completionLatch::countDown)
                .subscribe().with(collected::add);

        if (!completionLatch.await(5, TimeUnit.SECONDS)) {
            fail("Streaming did not complete in time");
        }
        assertThat(collected).hasSize(4)
                .contains("\"one\"", "\"two\"", "\"3\"", "\"four\"");
    }

    @Test
    void shouldReadNdjsonPojoAsMulti() throws InterruptedException {
        var client = RestClientBuilder.newBuilder().baseUri(uri)
                .build(Client.class);
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
        var client = RestClientBuilder.newBuilder().baseUri(reactiveRoutesBaseUri)
                .build(Client.class);
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
        var client = RestClientBuilder.newBuilder().baseUri(uri)
                .build(Client.class);
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

    @Path("/stream")
    public interface Client {
        @GET
        @Path("/string")
        @Produces(RestMediaType.APPLICATION_STREAM_JSON)
        @RestStreamElementType(MediaType.APPLICATION_JSON)
        Multi<String> readString();

        @GET
        @Path("/pojo")
        @Produces(RestMediaType.APPLICATION_STREAM_JSON)
        @RestStreamElementType(MediaType.APPLICATION_JSON)
        Multi<Message> readPojo();

        @GET
        @Path("/single-pojo")
        @Produces(RestMediaType.APPLICATION_STREAM_JSON)
        @RestStreamElementType(MediaType.APPLICATION_JSON)
        Multi<Message> readPojoSingle();
    }

    public static class ReactiveRoutesResource {
        @Route(path = "/rr/stream/pojo", produces = ReactiveRoutes.JSON_STREAM)
        Multi<Message> people(RoutingContext context) {
            return Multi.createFrom().items(
                    Message.of("superman", "1"),
                    Message.of("batman", "2"),
                    Message.of("spiderman", "3"));
        }
    }

    @Path("/stream")
    public static class StreamingResource {
        @Inject
        Vertx vertx;

        @GET
        @Path("/string")
        @Produces(RestMediaType.APPLICATION_STREAM_JSON)
        @RestStreamElementType(MediaType.APPLICATION_JSON)
        public Multi<String> readString() {
            return Multi.createFrom().emitter(
                    em -> {
                        em.emit("one");
                        em.emit("two");
                        em.emit("3");
                        em.emit("four");
                        em.complete();
                    });
        }

        @GET
        @Path("/pojo")
        @Produces(RestMediaType.APPLICATION_STREAM_JSON)
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
        @Produces(RestMediaType.APPLICATION_STREAM_JSON)
        @RestStreamElementType(MediaType.APPLICATION_JSON)
        public String getPojosAsString() throws JsonProcessingException {
            ObjectMapper mapper = new ObjectMapper();
            StringBuilder result = new StringBuilder();
            ObjectWriter objectWriter = mapper.writerFor(Message.class);
            for (var msg : List.of(Message.of("zero", "0"),
                    Message.of("one", "1"),
                    Message.of("two", "2"),
                    Message.of("three", "3"))) {
                result.append(objectWriter.writeValueAsString(msg));
                result.append("\n");
            }
            return result.toString();
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
