package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.MatrixParam;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestMatrix;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class MatrixParamTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(Resource.class, Client.class));

    @TestHTTPResource
    URI baseUri;

    @Test
    void clientSendsMatrixParam() {
        Client client = QuarkusRestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        assertThat(client.greet("world")).isEqualTo("hello world");
        assertThat(client.greetRest("world")).isEqualTo("hello world");
    }

    @Test
    void clientOmitsNullMatrixParam() {
        Client client = QuarkusRestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        assertThat(client.greet(null)).isEqualTo("hello null");
    }

    @Path("/greet")
    public static class Resource {

        @GET
        public String greet(@MatrixParam("name") String name) {
            return "hello " + name;
        }
    }

    @Path("/greet")
    public interface Client {

        @GET
        String greet(@MatrixParam("name") String name);

        @GET
        String greetRest(@RestMatrix("name") String name);
    }
}
