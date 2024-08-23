package io.quarkus.rest.client.reactive.jackson.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestHeader;
import org.jboss.resteasy.reactive.RestStreamElementType;
import org.jboss.resteasy.reactive.client.SseEvent;
import org.jboss.resteasy.reactive.client.SseEventFilter;
import org.jboss.resteasy.reactive.server.jackson.JacksonBasicMessageBodyReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.rest.client.reactive.ClientExceptionMapper;
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
    void shouldReadBodyFromFailedResponse() {
        var errorBody = new AtomicReference<String>();
        createClient()
                .fail()
                .subscribe().with(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) {

                    }
                }, new Consumer<>() {
                    @Override
                    public void accept(Throwable t) {
                        errorBody.set(t.getMessage());
                    }
                });

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(
                        () -> assertThat(errorBody.get()).isEqualTo("invalid input provided"));
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

    @Test
    void shouldBeAbleReadEntireEvent() {
        var resultList = new CopyOnWriteArrayList<>();
        createClient()
                .event()
                .subscribe().with(new Consumer<>() {
                    @Override
                    public void accept(SseEvent<Dto> event) {
                        resultList.add(new EventContainer(event.id(), event.name(), event.data()));
                    }
                });
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(
                        () -> assertThat(resultList).containsExactly(
                                new EventContainer("id0", "name0", new Dto("name0", "0")),
                                new EventContainer("id1", "name1", new Dto("name1", "1"))));
    }

    @Test
    void shouldBeAbleReadEntireEventWhileAlsoBeingAbleToFilterEvents() {
        var resultList = new CopyOnWriteArrayList<>();
        createClient()
                .eventWithFilter()
                .subscribe().with(new Consumer<>() {
                    @Override
                    public void accept(SseEvent<Dto> event) {
                        resultList.add(new EventContainer(event.id(), event.name(), event.data()));
                    }
                });
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(
                        () -> assertThat(resultList).containsExactly(
                                new EventContainer("id", "n0", new Dto("name0", "0")),
                                new EventContainer("id", "n1", new Dto("name1", "1")),
                                new EventContainer("id", "n2", new Dto("name2", "2"))));
    }

    static class EventContainer {
        final String id;
        final String name;
        final Dto dto;

        EventContainer(String id, String name, Dto dto) {
            this.id = id;
            this.name = name;
            this.dto = dto;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            EventContainer that = (EventContainer) o;
            return Objects.equals(id, that.id) && Objects.equals(name, that.name)
                    && Objects.equals(dto, that.dto);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name, dto);
        }

        @Override
        public String toString() {
            return "EventContainer{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", dto=" + dto +
                    '}';
        }
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
        @Produces(MediaType.SERVER_SENT_EVENTS)
        @ClientHeaderParam(name = "fail", value = "true")
        Multi<String> fail();

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

        @GET
        @Path("/event")
        @Produces(MediaType.SERVER_SENT_EVENTS)
        Multi<SseEvent<Dto>> event();

        @GET
        @Path("/event-with-filter")
        @Produces(MediaType.SERVER_SENT_EVENTS)
        @SseEventFilter(CustomFilter.class)
        Multi<SseEvent<Dto>> eventWithFilter();

        @ClientExceptionMapper
        static RuntimeException toException(Response response) {
            if (response.getStatusInfo().getStatusCode() == 400) {
                return new IllegalArgumentException(response.readEntity(String.class));
            }
            return null;
        }
    }

    public static class CustomFilter implements Predicate<SseEvent<String>> {

        @Override
        public boolean test(SseEvent<String> event) {
            if ("heartbeat".equals(event.id())) {
                return false;
            }
            if ("END".equals(event.data())) {
                return false;
            }
            return true;
        }
    }

    @Path("/sse")
    public static class SseResource {

        @GET
        @Produces(MediaType.SERVER_SENT_EVENTS)
        public Multi<String> get(@DefaultValue("false") @RestHeader boolean fail) {
            if (fail) {
                throw new WebApplicationException(Response.status(400).entity("invalid input provided").build());
            }
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

        @GET
        @Path("/event")
        @Produces(MediaType.SERVER_SENT_EVENTS)
        public void event(@Context SseEventSink sink, @Context Sse sse) {
            // send a stream of few events
            try (sink) {
                for (int i = 0; i < 2; i++) {
                    final OutboundSseEvent.Builder builder = sse.newEventBuilder();
                    builder.id("id" + i)
                            .mediaType(MediaType.APPLICATION_JSON_TYPE)
                            .data(Dto.class, new Dto("name" + i, String.valueOf(i)))
                            .name("name" + i);

                    sink.send(builder.build());
                }
            }
        }

        @GET
        @Path("/event-with-filter")
        @Produces(MediaType.SERVER_SENT_EVENTS)
        public void eventWithFilter(@Context SseEventSink sink, @Context Sse sse) {
            try (sink) {
                sink.send(sse.newEventBuilder()
                        .id("id")
                        .mediaType(MediaType.APPLICATION_JSON_TYPE)
                        .data(Dto.class, new Dto("name0", "0"))
                        .name("n0")
                        .build());

                sink.send(sse.newEventBuilder()
                        .id("heartbeat")
                        .comment("heartbeat")
                        .mediaType(MediaType.APPLICATION_JSON_TYPE)
                        .build());

                sink.send(sse.newEventBuilder()
                        .id("id")
                        .mediaType(MediaType.APPLICATION_JSON_TYPE)
                        .data(Dto.class, new Dto("name1", "1"))
                        .name("n1")
                        .build());

                sink.send(sse.newEventBuilder()
                        .id("heartbeat")
                        .comment("heartbeat")
                        .build());

                sink.send(sse.newEventBuilder()
                        .id("id")
                        .mediaType(MediaType.APPLICATION_JSON_TYPE)
                        .data(Dto.class, new Dto("name2", "2"))
                        .name("n2")
                        .build());

                sink.send(sse.newEventBuilder()
                        .id("end")
                        .data("END")
                        .build());
            }
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

        @Override
        public String toString() {
            return "Dto{" +
                    "name='" + name + '\'' +
                    ", value='" + value + '\'' +
                    '}';
        }
    }
}
