package io.quarkus.rest.client.reactive.jackson.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URL;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;

import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.quarkus.rest.client.reactive.runtime.RestClientBuilderImpl;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.mutiny.Uni;

public class ClientWithCustomObjectMapperTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Resource.class));

    @TestHTTPResource
    URL url;

    MyClient clientAllowsUnknown;
    MyClient clientDisallowsUnknown;

    @BeforeEach
    public void setUp() {
        clientAllowsUnknown = new RestClientBuilderImpl()
                .baseUrl(url)
                .register(ClientObjectMapperUnknown.class)
                .build(MyClient.class);

        clientDisallowsUnknown = new RestClientBuilderImpl()
                .baseUrl(url)
                .register(ClientObjectMapperNoUnknown.class)
                .build(MyClient.class);
    }

    @Test
    void testCustomObjectMappersShouldBeUsed() {
        Request request = new Request();
        request.value = "someValue";

        // FAIL_ON_UNKNOWN_PROPERTIES disabled
        assertThat(clientAllowsUnknown.post(request).await().indefinitely())
                .isInstanceOf(Response.class)
                .satisfies(r -> assertThat(r.value).isEqualTo(request.value));

        // FAIL_ON_UNKNOWN_PROPERTIES enabled
        assertThatThrownBy(() -> clientDisallowsUnknown.post(request).await().indefinitely())
                .isInstanceOf(ClientWebApplicationException.class);
    }

    @Path("/post")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public interface MyClient {
        @POST
        Uni<Response> post(Request request);
    }

    @Path("/post")
    public static class Resource {

        @Produces(MediaType.APPLICATION_JSON)
        @Consumes(MediaType.APPLICATION_JSON)
        @POST
        public ResponseWithSecondValue post(Request request) {
            ResponseWithSecondValue response = new ResponseWithSecondValue();
            response.value = request.value;
            response.secondValue = "extraValue";
            return response;
        }
    }

    public static class Request {
        public String value;
    }

    public static class Response {
        public String value;
    }

    public static class ResponseWithSecondValue extends Response {
        public String secondValue;
    }

    public static class ClientObjectMapperUnknown implements ContextResolver<ObjectMapper> {
        @Override
        public ObjectMapper getContext(Class<?> type) {
            return new ObjectMapper()
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        }
    }

    public static class ClientObjectMapperNoUnknown implements ContextResolver<ObjectMapper> {
        @Override
        public ObjectMapper getContext(Class<?> type) {
            return new ObjectMapper()
                    .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .enable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        }
    }
}
