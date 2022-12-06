package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class FormTest {

    @TestHTTPResource
    URI baseUri;

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest();

    @Test
    void shouldPassUrlEncodedAsForm() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        assertThat(client.echo(new Form().param("name", "World"))).isEqualTo("Hello, World!");
    }

    @Path("/hello")
    public interface Client {
        @POST
        @Produces(MediaType.TEXT_PLAIN)
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        String echo(Form form);
    }

    @Path("/hello")
    public static class Resource {
        @POST
        @Produces(MediaType.TEXT_PLAIN)
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        public String echo(@FormParam("name") String name) {
            return String.format("Hello, %s!", name);
        }
    }
}
