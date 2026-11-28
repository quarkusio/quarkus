package io.quarkus.rest.client.reactive.proxy;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.restclient.config.RestClientsConfig;
import io.quarkus.test.QuarkusExtensionTest;

public class HttpProxyDevServicesBasePathTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Resource.class, Client.class))
            .overrideConfigKey("quarkus.rest-client.\"client\".enable-local-proxy", "true")
            .overrideConfigKey("quarkus.rest-client.\"client\".url", "quarkus://vertx-http/base");

    @RestClient
    Client client;

    @Inject
    RestClientsConfig restClientsConfig;

    @Test
    void proxyForwardsUnderThePathOfTheBaseUri() {
        // the REST Client calls the proxy at its root, and the proxy adds the path of the base URI
        String proxyUrl = restClientsConfig.clients().get(Client.class.getName()).overrideUri().orElseThrow();
        given().baseUri(proxyUrl)
                .get("hello")
                .then()
                .statusCode(200)
                .body(equalTo("hello from base"));

        assertThat(client.hello()).isEqualTo("hello from base");
    }

    @RegisterRestClient(configKey = "client")
    public interface Client {

        @GET
        @Path("hello")
        String hello();
    }

    @Path("base")
    public static class Resource {

        @GET
        @Path("hello")
        public String hello() {
            return "hello from base";
        }
    }
}
