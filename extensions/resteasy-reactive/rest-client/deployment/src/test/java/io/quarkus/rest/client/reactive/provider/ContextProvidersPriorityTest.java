package io.quarkus.rest.client.reactive.provider;

import static io.restassured.RestAssured.given;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.ContextResolver;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.jboss.resteasy.reactive.server.jackson.JacksonBasicMessageBodyReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.rest.client.reactive.TestJacksonBasicMessageBodyReader;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class ContextProvidersPriorityTest {
    private static final String HEADER_NAME = "my-header";
    private static final String HEADER_VALUE_FROM_LOW_PRIORITY = "low-priority";
    private static final String HEADER_VALUE_FROM_HIGH_PRIORITY = "high-priority";

    @TestHTTPResource
    URI baseUri;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Client.class, TestJacksonBasicMessageBodyReader.class));

    @Test
    void shouldUseTheHighestPriorityContextProvider() {
        // @formatter:off
        var response =
                given()
                        .body(baseUri.toString())
                        .when()
                        .post("/call-client")
                        .thenReturn();
        // @formatter:on
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getString(HEADER_NAME)).isEqualTo(format("[%s]", HEADER_VALUE_FROM_HIGH_PRIORITY));
    }

    @Path("/")
    @ApplicationScoped
    public static class Resource {

        @GET
        @Produces("application/json")
        public Map<String, List<String>> returnHeaderValues(@Context HttpHeaders headers) {
            return headers.getRequestHeaders();
        }

        @Path("/call-client")
        @POST
        public Map<String, List<String>> callClient(String uri) {
            Client client = QuarkusRestClientBuilder.newBuilder()
                    .baseUri(URI.create(uri))
                    .register(LowPriorityClientHeadersProvider.class)
                    .register(HighPriorityClientHeadersProvider.class)
                    .register(new TestJacksonBasicMessageBodyReader())
                    .build(Client.class);
            return client.get();
        }
    }

    @RegisterProvider(ErroneousJacksonBasicMessageBodyReader.class)
    public interface Client {
        @GET
        Map<String, List<String>> get();
    }

    @Priority(2)
    public static class LowPriorityClientHeadersProvider implements ContextResolver<ClientHeadersFactory> {

        @Override
        public ClientHeadersFactory getContext(Class<?> aClass) {
            return new CustomClientHeadersFactory(HEADER_VALUE_FROM_LOW_PRIORITY);
        }
    }

    @Priority(1)
    public static class HighPriorityClientHeadersProvider implements ContextResolver<ClientHeadersFactory> {

        @Override
        public ClientHeadersFactory getContext(Class<?> aClass) {
            return new CustomClientHeadersFactory(HEADER_VALUE_FROM_HIGH_PRIORITY);
        }
    }

    @Priority(Priorities.USER + 100)
    public static class ErroneousJacksonBasicMessageBodyReader extends JacksonBasicMessageBodyReader {
        public ErroneousJacksonBasicMessageBodyReader() {
            super(new ObjectMapper());
        }

        @Override
        public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
                throws IOException, WebApplicationException {
            throw new IllegalStateException("should never be called");
        }
    }

    public static class CustomClientHeadersFactory implements ClientHeadersFactory {

        private final String value;

        public CustomClientHeadersFactory(String value) {
            this.value = value;
        }

        @Override
        public MultivaluedMap<String, String> update(MultivaluedMap<String, String> multivaluedMap,
                MultivaluedMap<String, String> multivaluedMap1) {
            MultivaluedHashMap<String, String> newHeaders = new MultivaluedHashMap<>();
            newHeaders.add(HEADER_NAME, value);
            return newHeaders;
        }
    }
}
