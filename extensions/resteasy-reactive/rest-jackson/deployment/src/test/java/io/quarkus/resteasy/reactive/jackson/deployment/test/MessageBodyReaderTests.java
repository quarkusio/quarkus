package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.CompletionCallback;
import jakarta.ws.rs.container.ConnectionCallback;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.common.providers.serialisers.AbstractJsonMessageBodyReader;
import org.jboss.resteasy.reactive.server.jackson.JacksonBasicMessageBodyReader;
import org.jboss.resteasy.reactive.server.jaxrs.HttpHeadersImpl;
import org.jboss.resteasy.reactive.server.spi.ContentType;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerHttpResponse;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;

import io.quarkus.resteasy.reactive.jackson.runtime.serialisers.ServerJacksonMessageBodyReader;

@SuppressWarnings("unchecked")
class MessageBodyReaderTests {

    static class CommonReaderTests {
        private final AbstractJsonMessageBodyReader reader;

        public CommonReaderTests(AbstractJsonMessageBodyReader reader) {
            this.reader = reader;
        }

        void deserializeMissingToken() throws IOException {
            var stream = new ByteArrayInputStream("{\"model\": \"model\", \"cost\": 2".getBytes(StandardCharsets.UTF_8));
            Object widget = new Widget("", 1d);
            reader.readFrom((Class<Object>) widget.getClass(), null, null, null, null, stream);
        }

        void deserializeMissingRequiredProperty() throws IOException {
            // missing non-nullable property
            var stream = new ByteArrayInputStream("{\"cost\": 2}".getBytes(StandardCharsets.UTF_8));
            Object widget = new Widget("", 1d);
            reader.readFrom((Class<Object>) widget.getClass(), null, null, null, null, stream);
        }

        void deserializeMissingReferenceProperty() throws IOException {
            var json = "{\n" +
                    "  \"id\" : 1,\n" +
                    "  \"name\" : \"Learn HTML\",\n" +
                    "  \"owner\" : 1\n" + // unresolved reference to student
                    "}";

            var stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
            Object book = new Book(1, null, null);
            reader.readFrom((Class<Object>) book.getClass(), null, null, null, null, stream);
        }

        void deserializeClassWithInvalidDefinition() throws IOException {
            var json = "{\n" +
                    "  \"arg\" : \"Learn HTML\"" +
                    "}";

            var stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
            Object invalid = new InvalidDefinition(null);
            reader.readFrom((Class<Object>) invalid.getClass(), null, null, null, null, stream);
        }
    }

    @Nested
    @DisplayName("JacksonMessageBodyReader")
    class JacksonMessageBodyReaderTests {
        private final CommonReaderTests tests = new CommonReaderTests(new JacksonBasicMessageBodyReader(new ObjectMapper()));

        @Test
        void shouldThrowStreamReadException() {
            assertThrows(StreamReadException.class, tests::deserializeMissingToken);
        }

        @Test
        void shouldThrowValueInstantiationException() {
            assertThrows(ValueInstantiationException.class, tests::deserializeMissingRequiredProperty);
        }

        @Test
        void shouldThrowDatabindException() {
            assertThrows(DatabindException.class, tests::deserializeMissingReferenceProperty);
        }

        @Test
        void shouldThrowInvalidDefinitionException() {
            assertThrows(InvalidDefinitionException.class, tests::deserializeClassWithInvalidDefinition);
        }
    }

    @Nested
    @DisplayName("ServerJacksonMessageBodyReader")
    class ServerJacksonMessageBodyReaderTests {
        private final CommonReaderTests tests = new CommonReaderTests(new ServerJacksonMessageBodyReader(new ObjectMapper()));

        @Test
        void shouldThrowWebExceptionWithStreamReadExceptionCause() {
            var e = assertThrows(WebApplicationException.class, tests::deserializeMissingToken);
            assertThat(StreamReadException.class).isAssignableFrom(e.getCause().getClass());
        }

        @Test
        void shouldThrowWebExceptionWithValueInstantiationExceptionCause() {
            var e = assertThrows(WebApplicationException.class, tests::deserializeMissingRequiredProperty);
            assertThat(ValueInstantiationException.class).isAssignableFrom(e.getCause().getClass());
        }

        @Test
        void shouldThrowWebExceptionWithDatabindExceptionCause() {
            var e = assertThrows(WebApplicationException.class, tests::deserializeMissingReferenceProperty);
            assertThat(DatabindException.class).isAssignableFrom(e.getCause().getClass());
        }

        @Test
        void shouldThrowInvalidDefinitionException() {
            assertThrows(InvalidDefinitionException.class, tests::deserializeClassWithInvalidDefinition);
        }

        @Test
        void shouldThrowWebExceptionWithValueInstantiationExceptionCauseUsingServerRequestContext() throws IOException {
            var reader = new ServerJacksonMessageBodyReader(new ObjectMapper());
            // missing non-nullable property
            var stream = new ByteArrayInputStream("{\"cost\": 2}".getBytes(StandardCharsets.UTF_8));
            var context = new MockServerRequestContext(stream);
            Object widget = new Widget("", 1d);

            try {
                reader.readFrom((Class<Object>) widget.getClass(), null, MediaType.APPLICATION_JSON_TYPE, context);
            } catch (WebApplicationException e) {
                assertThat(ValueInstantiationException.class).isAssignableFrom(e.getCause().getClass());
            }
        }
    }

    static class InvalidDefinition {
        // Note: Multiple constructors marked as JsonCreators should throw InvalidDefinitionException

        private final Object arg;

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public InvalidDefinition(Object arg) {
            this.arg = arg;
        }
    }

    static class Widget {

        public final String model;
        public final double cost;

        @JsonCreator
        public Widget(
                @JsonProperty("model") String model,
                @JsonProperty("cost") double cost) {
            this.model = Objects.requireNonNull(model, "'model' must be supplied");
            this.cost = cost;
        }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    static class Student {
        public int id;
        public int rollNo;
        public String name;
        public List<Book> books;

        Student(int id, int rollNo, String name) {
            this.id = id;
            this.rollNo = rollNo;
            this.name = name;
            this.books = new ArrayList<>();
        }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    static class Book {
        @JsonProperty("id")
        public int id;
        @JsonProperty("name")
        public String name;

        Book() {
            // do nothing ... for Jackson
        }

        Book(int id, String name, Student owner) {
            this.id = id;
            this.name = name;
            this.owner = owner;
        }

        @JsonIdentityReference(alwaysAsId = true)
        @JsonProperty("owner")
        public Student owner;
    }

    private static class MockServerRequestContext implements ServerRequestContext {
        private final InputStream stream;

        public MockServerRequestContext(InputStream stream) {
            this.stream = stream;
        }

        @Override
        public void registerCompletionCallback(CompletionCallback callback) {

        }

        @Override
        public void registerConnectionCallback(ConnectionCallback callback) {

        }

        @Override
        public ServerHttpResponse serverResponse() {
            return null;
        }

        @Override
        public InputStream getInputStream() {
            return stream;
        }

        @Override
        public ContentType getResponseContentType() {
            return null;
        }

        @Override
        public MediaType getResponseMediaType() {
            return null;
        }

        @Override
        public OutputStream getOrCreateOutputStream() {
            return null;
        }

        @Override
        public ResteasyReactiveResourceInfo getResteasyReactiveResourceInfo() {
            return null;
        }

        @Override
        public HttpHeaders getRequestHeaders() {
            return new HttpHeadersImpl(Collections.emptyList());
        }

        @Override
        public void abortWith(Response response) {

        }
    }
}
