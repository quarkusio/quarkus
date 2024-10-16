package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class VoidReturnTypeTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Resource.class));

    @TestHTTPResource
    URI baseUri;

    @Test
    void testHeadersWithSubresource() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);

        client.call();
        client.call();

        assertThat(Resource.COUNTER.get()).isEqualTo(2);
    }

    @Path("/")
    @ApplicationScoped
    public static class Resource {

        static final AtomicInteger COUNTER = new AtomicInteger(0);

        @POST
        public int returnCount() {
            return COUNTER.incrementAndGet();
        }
    }

    public interface Client {

        @Path("/")
        @POST
        void call();
    }
}
