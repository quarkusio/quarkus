package io.quarkus.rest.client.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.Charset;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class ReadInputStreamInResponseFilterTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(
                    Client.class, BodyReadingInterceptor.class, Resource.class));

    private static final String OUTPUT = "Hello".repeat(1000);
    public static final String OUTPUT_HEADER = "OutputBody";

    @TestHTTPResource
    URI uri;

    @Test
    public void test() {
        Client client = createClient();
        Response response = client.hello();
        assertEquals(200, response.getStatus());
        assertEquals(OUTPUT, response.getHeaderString(OUTPUT_HEADER));
        assertEquals(OUTPUT, response.readEntity(String.class));
    }

    private Client createClient() {
        return RestClientBuilder.newBuilder()
                .baseUri(uri)
                .build(Client.class);
    }

    @Path("/test")
    @RegisterProvider(value = BodyReadingInterceptor.class)
    public interface Client {

        @Produces(MediaType.APPLICATION_JSON)
        @Path("/hello")
        @GET
        Response hello();
    }

    @Path("test")
    public static class Resource {

        @GET
        @Path("hello")
        public Response hello() {
            return Response.ok(OUTPUT).build();
        }
    }

    public static class BodyReadingInterceptor implements ClientRequestFilter, ClientResponseFilter {

        @Override
        public void filter(ClientRequestContext requestContext) throws JsonProcessingException {
        }

        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
            responseContext.getHeaders().add(OUTPUT_HEADER, readEntityStream(responseContext));
        }

        private String readEntityStream(final ClientResponseContext responseContext) {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            InputStream inputStream = responseContext.getEntityStream();
            StringBuilder builder = new StringBuilder();
            try {
                IOUtils.copy(inputStream, outStream);
                byte[] requestEntity = outStream.toByteArray();
                if (requestEntity.length > 0) {
                    builder.append(new String(requestEntity, Charset.defaultCharset()));
                }
                responseContext.setEntityStream(new ByteArrayInputStream(requestEntity));
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
            return builder.toString();
        }
    }
}
