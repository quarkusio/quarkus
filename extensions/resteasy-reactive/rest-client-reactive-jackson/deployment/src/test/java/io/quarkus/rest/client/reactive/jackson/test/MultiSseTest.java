package io.quarkus.rest.client.reactive.jackson.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestStreamElementType;
import org.jboss.resteasy.reactive.server.jackson.JacksonBasicMessageBodyReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
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
        createClient()
                .get()
                .subscribe().with(resultList::add);
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(
                        () -> assertThat(resultList).containsExactly("foo", "bar"));
    }

    @Test
    void shouldConsumeJsonEntity() {
        var resultList = new CopyOnWriteArrayList<>();
        createClient()
                .getJson()
                .subscribe().with(resultList::add);
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(
                        () -> assertThat(resultList).containsExactly(new Dto("foo", "bar"), new Dto("chocolate", "bar")));
    }

    @Test
    void shouldConsumeAsParametrizedType() {
        var resultList = new CopyOnWriteArrayList<>();
        createClient()
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
        createClient()
                .post("test")
                .subscribe().with(resultList::add);
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(
                        () -> assertThat(resultList).containsExactly("test", "test", "test"));
    }

    @Test
    void shouldSendPayloadAndConsumeAsParametrizedType() {
        var resultList = new CopyOnWriteArrayList<>();
        createClient()
                .postAndReadAsMap("test")
                .subscribe().with(resultList::add);
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(
                        () -> assertThat(resultList).containsExactly(
                                Map.of("name", "foo", "value", "test"),
                                Map.of("name", "foo", "value", "test"),
                                Map.of("name", "foo", "value", "test")));
    }

    /**
     * Test to reproduce the issue: https://github.com/quarkusio/quarkus/issues/32012.
     */
    @Test
    void shouldRestStreamElementTypeOverwriteProducesAtClassLevel() {
        var resultList = new CopyOnWriteArrayList<>();
        QuarkusRestClientBuilder.newBuilder().baseUri(uri)
                .build(SeeWithRestStreamElementTypeClient.class)
                .getJson()
                .subscribe()
                .with(resultList::add);
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(
                        () -> assertThat(resultList)
                                .containsExactly(new Dto("foo", "bar"), new Dto("chocolate", "bar")));
    }

    private SseClient createClient() {
        return QuarkusRestClientBuilder.newBuilder()
                .baseUri(uri)
                .register(new JacksonBasicMessageBodyReader(new ObjectMapper()))
                .build(SseClient.class);
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

    @Path("/sse-rest-stream-element-type")
    // The following annotation should be ignored because we're using `@RestStreamElementType(MediaType.APPLICATION_JSON)`.
    @Produces(MediaType.APPLICATION_JSON)
    public static class SseWithRestStreamElementTypeResource {
        @GET
        @RestStreamElementType(MediaType.APPLICATION_JSON)
        @Path("/json")
        public Multi<Dto> getJson() {
            return Multi.createFrom().items(new Dto("foo", "bar"), new Dto("chocolate", "bar"));
        }
    }

    @RegisterRestClient
    @Path("/sse-rest-stream-element-type")
    // The following annotation should be ignored because we're using `@RestStreamElementType(MediaType.APPLICATION_JSON)`.
    @Produces(MediaType.APPLICATION_JSON)
    public interface SeeWithRestStreamElementTypeClient {
        @GET
        @Path("/json")
        @RestStreamElementType(MediaType.APPLICATION_JSON)
        Multi<Dto> getJson();
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
