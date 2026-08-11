package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.MatrixParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.jboss.resteasy.reactive.RestMatrix;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class MatrixParamWithPathParamTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(Resource.class, Client.class));

    @TestHTTPResource
    URI baseUri;

    @Test
    void clientSendsMatrixParamWithPathParam() {
        Client client = QuarkusRestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        assertThat(client.get("42", "full")).isEqualTo("42/full");
        assertThat(client.getMulti("42", List.of("a", "b"))).isEqualTo("42/a,b");
    }

    @Path("/items/{id}")
    public static class Resource {

        @GET
        public String get(@PathParam("id") String id, @MatrixParam("view") String view) {
            return id + "/" + view;
        }

        @GET
        @Path("multi")
        public String getMulti(@PathParam("id") String id, @MatrixParam("tag") List<String> tags) {
            return id + "/" + String.join(",", tags);
        }
    }

    @Path("/items/{id}")
    public interface Client {

        @GET
        String get(@PathParam("id") String id, @RestMatrix("view") String view);

        @GET
        @Path("multi")
        String getMulti(@PathParam("id") String id, @MatrixParam("tag") List<String> tags);
    }
}
