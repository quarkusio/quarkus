package io.quarkus.micrometer.deployment.binder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.search.Search;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.quarkus.rest.client.reactive.Url;
import io.quarkus.test.QuarkusUnitTest;

public class RestClientUriParameterTest {

    final static SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(
                    jar -> jar.addClasses(Resource.class, Client.class))
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .overrideConfigKey("quarkus.rest-client.\"client\".url", "http://does-not-exist.io");

    @RestClient
    Client client;

    @ConfigProperty(name = "quarkus.http.test-port")
    Integer testPort;

    @BeforeAll
    static void setRegistry() {
        Metrics.addRegistry(registry);
    }

    @AfterAll()
    static void removeRegistry() {
        Metrics.removeRegistry(registry);
    }

    @Test
    public void testOverride() {
        String result = client.getById("http://localhost:" + testPort, "bar");
        assertEquals("bar", result);

        Timer clientTimer = registry.find("http.client.requests").timer();
        assertNotNull(clientTimer);
        assertEquals("/example/{id}", clientTimer.getId().getTag("uri"));
    }

    private Search getMeter(String name) {
        return registry.find(name);
    }

    @Path("/example")
    @RegisterRestClient(baseUri = "http://dummy")
    public interface Client {

        @GET
        @Path("/{id}")
        String getById(@Url String baseUri, @PathParam("id") String id);
    }

    @Path("/example")
    public static class Resource {

        @RestClient
        Client client;

        @GET
        @Path("/{id}")
        @Produces(MediaType.TEXT_PLAIN)
        public String example() {
            return "bar";
        }

        @GET
        @Path("/call")
        @Produces(MediaType.TEXT_PLAIN)
        public String call() {
            return client.getById("http://localhost:8080", "1");
        }
    }
}
