package io.quarkus.rest.client.reactive.url;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.UnknownHostException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.NotBody;
import io.quarkus.rest.client.reactive.Url;
import io.quarkus.test.QuarkusUnitTest;

public class UrlOnUriParameterTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(
                    jar -> jar.addClasses(Resource.class, Client.class))
            .overrideConfigKey("quarkus.rest-client.\"client\".url", "http://does-not-exist.io");

    @RestClient
    Client client;

    @ConfigProperty(name = "quarkus.http.test-port")
    Integer testPort;

    @Test
    public void testOverride() {
        String result = client.test("test", URI.create(String.format("http://localhost:%d", testPort)), "bar");
        assertEquals("bar", result);

    }

    @Test
    public void testNoOverride() {
        assertThatThrownBy(() -> client.test("test", null, "bar")).cause().isInstanceOf(UnknownHostException.class);
    }

    @Path("test")
    @RegisterRestClient(configKey = "client")
    public interface Client {

        @Path("count")
        @GET
        String test(@NotBody String unused, @Url URI uri, @RestQuery String foo);
    }

    @Path("test")
    public static class Resource {

        @GET
        @Path("count")
        public String test(@RestQuery String foo) {
            return foo;
        }
    }
}
