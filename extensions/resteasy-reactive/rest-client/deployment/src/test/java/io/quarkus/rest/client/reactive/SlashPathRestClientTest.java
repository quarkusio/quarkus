package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class SlashPathRestClientTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloClientRemovingSlashes.class,
                            HelloClientUsingConfigKey.class,
                            HelloClientUsingSimpleName.class,
                            HelloClientUsingClassName.class,
                            HelloResource.class))
            // disable the removal of trailing slash at client side
            .overrideConfigKey("quarkus.rest-client.test.removes-trailing-slash", "false")
            .overrideConfigKey("quarkus.rest-client.HelloClientUsingSimpleName.removes-trailing-slash", "false")
            .overrideConfigKey(
                    "quarkus.rest-client.\"io.quarkus.rest.client.reactive.SlashPathRestClientTest$HelloClientUsingClassName\".removes-trailing-slash",
                    "false")
            // disable the removal of trailing slash at server side
            .overrideConfigKey("quarkus.rest.removes-trailing-slash", "false")
            .overrideRuntimeConfigKey("quarkus.rest-client.test.url",
                    "http://localhost:${quarkus.http.test-port:8081}")
            .overrideRuntimeConfigKey("quarkus.rest-client.default.url",
                    "http://localhost:${quarkus.http.test-port:8081}")
            .overrideRuntimeConfigKey("quarkus.rest-client.HelloClientUsingSimpleName.url",
                    "http://localhost:${quarkus.http.test-port:8081}")
            .overrideRuntimeConfigKey(
                    "quarkus.rest-client.\"io.quarkus.rest.client.reactive.SlashPathRestClientTest$HelloClientUsingClassName\".url",
                    "http://localhost:${quarkus.http.test-port:8081}");

    @RestClient
    HelloClientRemovingSlashes clientUsingDefaultBehaviour;

    @RestClient
    HelloClientUsingConfigKey clientUsingConfigKey;

    @RestClient
    HelloClientUsingSimpleName clientUsingSimpleName;

    @RestClient
    HelloClientUsingClassName clientUsingClassName;

    @Test
    void shouldRemoveTrailingSlashByDefault() {
        assertThat(clientUsingDefaultBehaviour.echo()).isEqualTo("/slash/without");
    }

    @Test
    void shouldRemoveTrailingSlashUsingConfigKey() {
        assertThat(clientUsingConfigKey.echo()).isEqualTo("/slash/with/");
    }

    @Test
    void shouldRemoveTrailingSlashUsingSimpleName() {
        assertThat(clientUsingSimpleName.echo()).isEqualTo("/slash/with/");
    }

    @Test
    void shouldRemoveTrailingSlashUsingClassName() {
        assertThat(clientUsingClassName.echo()).isEqualTo("/slash/with/");
    }

    @RegisterRestClient(configKey = "default")
    @Path("/slash/without/")
    public interface HelloClientRemovingSlashes {
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        String echo();
    }

    @RegisterRestClient(configKey = "test")
    @Path("/slash/with/")
    public interface HelloClientUsingConfigKey {
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        String echo();
    }

    @RegisterRestClient
    @Path("/slash/with/")
    public interface HelloClientUsingSimpleName {
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        String echo();
    }

    @RegisterRestClient
    @Path("/slash/with/")
    public interface HelloClientUsingClassName {
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        String echo();
    }

    @Path("/slash")
    @Produces(MediaType.TEXT_PLAIN)
    public static class HelloResource {

        @GET
        @Path("/without")
        public String withoutSlash(@Context UriInfo uriInfo) {
            return uriInfo.getPath();
        }

        @GET
        @Path("/with/")
        public String usingSlash(@Context UriInfo uriInfo) {
            return uriInfo.getPath();
        }
    }
}
