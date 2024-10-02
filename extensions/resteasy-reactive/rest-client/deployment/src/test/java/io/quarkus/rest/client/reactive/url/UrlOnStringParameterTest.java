package io.quarkus.rest.client.reactive.url;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.UnknownHostException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.Url;
import io.quarkus.test.QuarkusUnitTest;

public class UrlOnStringParameterTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(
                    jar -> jar.addClasses(Resource.class, Client.class))
            .overrideConfigKey("quarkus.rest-client.client.uri", "http://does-not-exist.io");

    @RestClient
    Client client;

    @ConfigProperty(name = "quarkus.http.test-port")
    Integer testPort;

    @Test
    public void testOverride() {
        String result = client.test(String.format("http://localhost:%d", testPort));
        assertEquals("bar", result);

    }

    @Test
    public void testNoOverride() {
        assertThatThrownBy(() -> client.test(null)).cause().isInstanceOf(UnknownHostException.class);
    }

    @Path("test")
    @RegisterRestClient(configKey = "client")
    public interface Client {

        @Path("count")
        @GET
        String test(@Url String uri);
    }

    @Path("test")
    public static class Resource {

        @GET
        @Path("count")
        public String test() {
            return "bar";
        }
    }
}
