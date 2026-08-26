package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * {@link RestClient} is the qualifier of the injection point and has no meaning on the interface itself, but putting
 * it there used to be tolerated and must keep working rather than fail the build with a duplicate annotation on the
 * generated bean.
 */
public class RestClientQualifierOnInterfaceTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(Client.class, Resource.class))
            .overrideConfigKey("quarkus.rest-client.annotated.url", "http://localhost:${quarkus.http.test-port:8081}");

    @Inject
    @RestClient
    Client client;

    @Test
    void clientWithQualifierOnInterfaceIsGeneratedAndInjected() {
        assertThat(client.hello()).isEqualTo("hello");
    }

    @RestClient
    @RegisterRestClient(configKey = "annotated")
    @Path("/hello")
    public interface Client {

        @GET
        String hello();
    }

    @Path("/hello")
    public static class Resource {

        @GET
        public String hello() {
            return "hello";
        }
    }
}
