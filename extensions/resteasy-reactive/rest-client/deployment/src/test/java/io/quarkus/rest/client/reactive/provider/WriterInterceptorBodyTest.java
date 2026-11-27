package io.quarkus.rest.client.reactive.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

/**
 * A client {@link WriterInterceptor} may replace the output stream, call {@code proceed()} and only then
 * write to the original stream (the usual way to log or transform a request body). Everything written
 * after {@code proceed()} returns must still be sent.
 */
public class WriterInterceptorBodyTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(Resource.class, Client.class, CapturingInterceptor.class));

    @TestHTTPResource
    URI baseUri;

    @Test
    void bodyWrittenAfterProceedIsSent() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).register(CapturingInterceptor.class)
                .build(Client.class);
        assertThat(client.call("this is a sample body")).isEqualTo("this is a sample body|intercepted");
    }

    @Path("/")
    public static class Resource {
        @POST
        public String echo(String body) {
            if ((body == null) || body.isEmpty()) {
                return "null";
            }
            return body;
        }
    }

    public interface Client {

        @Path("/")
        @POST
        String call(String body);
    }

    /**
     * Captures what the writer produces, then writes it to the original stream together with a marker,
     * so the assertion can only pass if the interceptor ran and its late writes were kept.
     */
    public static class CapturingInterceptor implements WriterInterceptor {

        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
            OutputStream original = context.getOutputStream();
            ByteArrayOutputStream capture = new ByteArrayOutputStream();
            context.setOutputStream(capture);
            context.proceed();
            original.write(capture.toByteArray());
            original.write("|intercepted".getBytes(StandardCharsets.UTF_8));
            context.setOutputStream(original);
        }
    }
}
