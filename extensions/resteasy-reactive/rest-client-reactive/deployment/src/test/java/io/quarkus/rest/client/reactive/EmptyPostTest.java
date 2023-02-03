package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class EmptyPostTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest();

    @Test
    void shouldWork() {
        Client client = clientWithUri(
                "http://localhost:" + ConfigProvider.getConfig().getValue("quarkus.http.test-port", Integer.class));

        assertThat(client.post()).isEqualTo("0");
    }

    private Client clientWithUri(String uri) {
        return RestClientBuilder.newBuilder().baseUri(URI.create(uri)).build(Client.class);
    }

    @Path("/foo")
    public interface Client {
        @POST
        String post();
    }

    @Path("/foo")
    public static class Resource {
        @POST
        public String post(@HeaderParam("Content-Length") String contentLength) {
            return contentLength;
        }
    }

}
