package io.quarkus.rest.client.reactive.jackson.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.mutiny.Multi;

public class MultiSseTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withEmptyApplication();

    @TestHTTPResource
    URI uri;

    @Test
    void shouldConsume() {
        var resultList = new CopyOnWriteArrayList<>();
        RestClientBuilder.newBuilder().baseUri(uri).build(SseClient.class)
                .get()
                .subscribe().with(resultList::add);
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(
                        () -> assertThat(resultList).containsExactly("foo", "bar"));
    }

    @Test
    void shouldConsumeJsonEntity() {
        var resultList = new CopyOnWriteArrayList<>();
        RestClientBuilder.newBuilder().baseUri(uri).build(SseClient.class)
                .getJson()
                .subscribe().with(resultList::add);
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(
                        () -> assertThat(resultList).containsExactly(new Dto("foo", "bar"), new Dto("chocolate", "bar")));
    }

    @Test
    void shouldConsumeAsParametrizedType() {
        var resultList = new CopyOnWriteArrayList<>();
        RestClientBuilder.newBuilder().baseUri(uri).build(SseClient.class)
                .getJsonAsMap()
                .subscribe().with(resultList::add);
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(
                        () -> assertThat(resultList).containsExactly(Map.of("name", "foo", "value", "bar"),
                                Map.of("name", "chocolate", "value", "bar")));
    }

    @Test
    void shouldSendPayloadAndConsume() {
        var resultList = new CopyOnWriteArrayList<>();
        RestClientBuilder.newBuilder().baseUri(uri).build(SseClient.class)
                .post("test")
                .subscribe().with(resultList::add);
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(
                        () -> assertThat(resultList).containsExactly("test", "test", "test"));
    }

    @Test
    void shouldSendPayloadAndConsumeAsParametrizedType() {
        var resultList = new CopyOnWriteArrayList<>();
        RestClientBuilder.newBuilder().baseUri(uri).build(SseClient.class)
                .postAndReadAsMap("test")
                .subscribe().with(resultList::add);
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(
                        () -> assertThat(resultList).containsExactly(
                                Map.of("name", "foo", "value", "test"),
                                Map.of("name", "foo", "value", "test"),
                                Map.of("name", "foo", "value", "test")));
    }

    @Path("/sse")
    public interface SseClient {
        @GET
        @Produces(MediaType.SERVER_SENT_EVENTS)
        Multi<String> get();

        @GET
        @Path("/json")
        @Produces(MediaType.SERVER_SENT_EVENTS)
        Multi<Dto> getJson();

        @GET
        @Produces(MediaType.SERVER_SENT_EVENTS)
        @Path("/json")
        Multi<Map<String, String>> getJsonAsMap();

        @POST
        @Produces(MediaType.SERVER_SENT_EVENTS)
        @Path("/with-entity")
        Multi<String> post(String entity);

        @POST
        @Produces(MediaType.SERVER_SENT_EVENTS)
        @Path("/with-entity-json")
        Multi<Map<String, String>> postAndReadAsMap(String entity);
    }

    @Path("/sse")
    public static class SseResource {

        @GET
        @Produces(MediaType.SERVER_SENT_EVENTS)
        public Multi<String> get() {
            return Multi.createFrom().items("foo", "bar");
        }

        @GET
        @Produces(MediaType.SERVER_SENT_EVENTS)
        @Path("/json")
        public Multi<Dto> getJson() {
            return Multi.createFrom().items(new Dto("foo", "bar"), new Dto("chocolate", "bar"));
        }

        @POST
        @Produces(MediaType.SERVER_SENT_EVENTS)
        @Path("/with-entity")
        public Multi<String> post(String entity) {
            return Multi.createBy().repeating().supplier(() -> entity).atMost(3);
        }

        @POST
        @Produces(MediaType.SERVER_SENT_EVENTS)
        @Path("/with-entity-json")
        public Multi<Dto> postAndReadAsMap(String entity) {
            return Multi.createBy().repeating().supplier(() -> new Dto("foo", entity)).atMost(3);
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
