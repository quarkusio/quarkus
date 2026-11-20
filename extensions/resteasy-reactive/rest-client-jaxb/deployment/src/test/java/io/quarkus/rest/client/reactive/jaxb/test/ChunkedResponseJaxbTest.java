package io.quarkus.rest.client.reactive.jaxb.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import jakarta.enterprise.event.Observes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;

/**
 * The server sends the response headers first and the chunked body later, so the client sees an entity stream with no
 * bytes available yet when the entity is read.
 */
public class ChunkedResponseJaxbTest {

    private static final String DTO_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Dto><name>foo</name></Dto>";

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Endpoint.class, Client.class, Dto.class));

    @TestHTTPResource
    URI uri;

    @Test
    void shouldReadEntityWhenBodyArrivesAfterHeaders() {
        Client client = QuarkusRestClientBuilder.newBuilder().baseUri(uri).build(Client.class);
        try (Response response = client.delayed()) {
            Dto dto = response.readEntity(Dto.class);
            assertThat(dto).isNotNull();
            assertThat(dto.name).isEqualTo("foo");
        }
    }

    @Test
    void shouldReturnNullForEmptyBody() {
        Client client = QuarkusRestClientBuilder.newBuilder().baseUri(uri).build(Client.class);
        try (Response response = client.empty()) {
            assertThat(response.readEntity(Dto.class)).isNull();
        }
    }

    @Path("/chunked")
    public interface Client {

        @GET
        @Path("/delayed")
        @Produces(MediaType.APPLICATION_XML)
        Response delayed();

        @GET
        @Path("/empty")
        @Produces(MediaType.APPLICATION_XML)
        Response empty();
    }

    public static class Endpoint {

        public void setup(@Observes Router router, Vertx vertx) {
            router.get("/chunked/delayed").handler(rc -> {
                HttpServerResponse response = rc.response()
                        .setChunked(true)
                        .putHeader("Content-Type", MediaType.APPLICATION_XML);
                response.writeHead();
                vertx.setTimer(500, id -> response.end(DTO_XML));
            });
            router.get("/chunked/empty").handler(rc -> rc.response()
                    .putHeader("Content-Type", MediaType.APPLICATION_XML)
                    .end());
        }
    }

    @XmlRootElement(name = "Dto")
    public static class Dto {
        public String name;
    }
}
