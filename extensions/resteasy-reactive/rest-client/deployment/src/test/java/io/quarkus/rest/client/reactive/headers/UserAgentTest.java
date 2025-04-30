package io.quarkus.rest.client.reactive.headers;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class UserAgentTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Resource.class));

    @TestHTTPResource
    URI baseUri;

    @Test
    void testHeadersWithSubresource() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        assertThat(client.call()).isEqualTo("Quarkus REST Client");
    }

    @Test
    void testHeaderOverride() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        assertThat(client.callWithUserAgent("custom-agent")).isEqualTo("custom-agent");
    }

    @Test
    void testHeadersWithImplicitValue() {
        ClientWithAnnotation client = RestClientBuilder.newBuilder().baseUri(baseUri).build(ClientWithAnnotation.class);
        assertThat(client.callWithImplicitValue()).isEqualTo("Annotated");
    }

    @Test
    void testHeadersWithExplicitValue() {
        ClientWithAnnotation client = RestClientBuilder.newBuilder().baseUri(baseUri).build(ClientWithAnnotation.class);
        assertThat(client.callWithExplicitValue("custom-agent")).isEqualTo("custom-agent");
    }

    @Path("/")
    @ApplicationScoped
    public static class Resource {
        @GET
        public String returnHeaders(@HeaderParam("user-agent") String header) {
            return header;
        }
    }

    public interface Client {

        @Path("/")
        @GET
        String call();

        @Path("/")
        @GET
        String callWithUserAgent(@HeaderParam("User-AgenT") String userAgent);
    }

    @ClientHeaderParam(name = "User-Agent", value = "Annotated")
    public interface ClientWithAnnotation {

        @Path("/")
        @GET
        String callWithImplicitValue();

        @Path("/")
        @GET
        String callWithExplicitValue(@HeaderParam("User-Agent") String userAgent);
    }

}
