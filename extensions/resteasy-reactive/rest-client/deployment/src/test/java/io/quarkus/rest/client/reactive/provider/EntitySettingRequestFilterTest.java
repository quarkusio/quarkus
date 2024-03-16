package io.quarkus.rest.client.reactive.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class EntitySettingRequestFilterTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Resource.class, Client.class, EntityRequestFilter.class));

    @TestHTTPResource
    URI baseUri;

    @Test
    void testNullEntity() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).register(new EntityRequestFilter(null))
                .build(Client.class);
        assertThat(client.call("ignored")).isEqualTo("null");
    }

    @Test
    void testNonNullEntity() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).register(new EntityRequestFilter("foo"))
                .build(Client.class);
        assertThat(client.call("ignored")).isEqualTo("foo");
    }

    @Path("/")
    public static class Resource {
        @POST
        public String returnHeaders(String body) {
            if ((body == null) || body.isEmpty()) {
                return "null";
            }
            return body;
        }
    }

    public interface Client {

        @Path("/")
        @POST
        String call(String body);
    }

    public static class EntityRequestFilter implements ClientRequestFilter {

        private final String entity;

        public EntityRequestFilter(String entity) {
            this.entity = entity;
        }

        @Override
        public void filter(ClientRequestContext context) throws IOException {
            context.setEntity(entity);
        }
    }
}
