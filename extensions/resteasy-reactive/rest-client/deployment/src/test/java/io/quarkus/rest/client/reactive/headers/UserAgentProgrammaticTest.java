package io.quarkus.rest.client.reactive.headers;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class UserAgentProgrammaticTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Resource.class, Client.class))
            .overrideRuntimeConfigKey("quarkus.rest-client.user-agent", "from-config");

    @TestHTTPResource
    URI baseUri;

    @Test
    void test() {
        Client client = QuarkusRestClientBuilder.newBuilder().baseUri(baseUri).userAgent("programmatic").build(Client.class);
        assertThat(client.call()).isEqualTo("programmatic");

        Client client2 = QuarkusRestClientBuilder.newBuilder().baseUri(baseUri).userAgent("programmatic2").build(Client.class);
        assertThat(client2.call()).isEqualTo("programmatic2");

        Client client3 = QuarkusRestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        assertThat(client3.call()).isEqualTo("from-config");
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
    }

}
