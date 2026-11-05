package io.quarkus.rest.client.reactive.error;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ProcessingException;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * A connection failure must name the target and keep the original exception as the cause.
 */
public class ConnectionFailureMessageTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(Client.class));

    @Test
    void connectionRefusedNamesTheTarget() throws IOException {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        URI uri = URI.create("http://localhost:" + port);
        Client client = RestClientBuilder.newBuilder().baseUri(uri).build(Client.class);

        assertThatThrownBy(client::call)
                .isInstanceOf(ProcessingException.class)
                .hasMessageStartingWith("Unable to connect to " + uri)
                .hasMessageContaining("listening")
                .cause().isInstanceOf(IOException.class);
    }

    public interface Client {

        @Path("/")
        @GET
        String call();
    }
}
