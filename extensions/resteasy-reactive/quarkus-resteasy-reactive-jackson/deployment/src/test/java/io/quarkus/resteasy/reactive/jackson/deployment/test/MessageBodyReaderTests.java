package io.quarkus.resteasy.reactive.jackson.deployment.test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.ServerJacksonMessageBodyReader;
import org.jboss.resteasy.reactive.common.providers.serialisers.AbstractJsonMessageBodyReader;
import org.jboss.resteasy.reactive.server.jackson.JacksonBasicMessageBodyReader;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.ws.rs.WebApplicationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
class MessageBodyReaderTests {

    static Stream<AbstractJsonMessageBodyReader> getReaders() {
        ObjectMapper mapper = new ObjectMapper();
        return Stream.of(
                new JacksonBasicMessageBodyReader(mapper),
                new ServerJacksonMessageBodyReader(mapper)
        );
    }

    @ParameterizedTest
    @MethodSource("getReaders")
    void shouldThrowStreamReadException(AbstractJsonMessageBodyReader reader) throws IOException {
        // missing ending brace
        var stream = new ByteArrayInputStream("{\"model\": \"model\", \"cost\": 2".getBytes(StandardCharsets.UTF_8));
        Object widget = new Widget("", 1d);

        try {
            reader.readFrom((Class<Object>) widget.getClass(), null, null, null, null, stream);
        } catch (WebApplicationException e) {
            assertThat(StreamReadException.class).isAssignableFrom(e.getCause().getClass());
        }
    }

    @ParameterizedTest
    @MethodSource("getReaders")
    void shouldThrowValueInstantiationException(AbstractJsonMessageBodyReader reader) throws IOException {
        // missing non-nullable property
        var stream = new ByteArrayInputStream("{\"cost\": 2}".getBytes(StandardCharsets.UTF_8));
        Object widget = new Widget("", 1d);

        try {
            reader.readFrom((Class<Object>) widget.getClass(), null, null, null, null, stream);
        } catch (WebApplicationException e) {
            assertThat(ValueInstantiationException.class).isAssignableFrom(e.getCause().getClass());
        }
    }

    @ParameterizedTest
    @MethodSource("getReaders")
    void shouldThrowDatabindException(AbstractJsonMessageBodyReader reader) throws IOException {

        var json =
                "{\n" +
                "  \"id\" : 1,\n" +
                "  \"name\" : \"Learn HTML\",\n" +
                "  \"owner\" : 1\n" + // unresolved reference to student
                "}";

        var stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        Object book = new Book(1, null, null);
        try {
            reader.readFrom((Class<Object>) book.getClass(), null, null, null, null, stream);
        } catch (WebApplicationException e) {
            assertThat(DatabindException.class).isAssignableFrom(e.getCause().getClass());
        }
    }

    static class Widget {

        public final String model;
        public final double cost;

        @JsonCreator
        public Widget(
                @JsonProperty("model") String model,
                @JsonProperty("cost") double cost
        ) {
            this.model = Objects.requireNonNull(model, "'model' must be supplied");
            this.cost = cost;
        }
    }

    @JsonIdentityInfo(
            generator = ObjectIdGenerators.PropertyGenerator.class,
            property = "id")
    static class Student {
        public int id;
        public int rollNo;
        public String name;
        public List<Book> books;

        Student(int id, int rollNo, String name){
            this.id = id;
            this.rollNo = rollNo;
            this.name = name;
            this.books = new ArrayList<>();
        }
    }

    @JsonIdentityInfo(
            generator = ObjectIdGenerators.PropertyGenerator.class,
            property = "id")
    static class Book{
        @JsonProperty("id") public int id;
        @JsonProperty("name") public String name;

        Book() {
            // do nothing ... for Jackson
        }

        Book(int id, String name, Student owner){
            this.id = id;
            this.name = name;
            this.owner = owner;
        }

        @JsonIdentityReference(alwaysAsId = true)
        @JsonProperty("owner")
        public Student owner;
    }
}
