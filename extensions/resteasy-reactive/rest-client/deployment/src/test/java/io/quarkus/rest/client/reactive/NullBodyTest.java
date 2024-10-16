package io.quarkus.rest.client.reactive;

import java.net.URI;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class NullBodyTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Client.class));

    @TestHTTPResource
    URI baseUri;

    @Test
    void withBody() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);

        Assertions.assertThatThrownBy(() -> client.call("test")).hasMessageContaining("404");
    }

    @Test
    void withoutBody() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);

        Assertions.assertThatThrownBy(() -> client.call(null)).hasMessageContaining("404");
    }

    public interface Client {

        @Path("/")
        @POST
        void call(String body);
    }
}
