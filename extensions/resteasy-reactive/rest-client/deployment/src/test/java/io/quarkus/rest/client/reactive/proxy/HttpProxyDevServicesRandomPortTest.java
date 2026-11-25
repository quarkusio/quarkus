package io.quarkus.rest.client.reactive.proxy;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.restclient.config.RestClientsConfig;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.value.registry.ValueRegistry;
import io.quarkus.vertx.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;

public class HttpProxyDevServicesRandomPortTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Resource.class, Client.class))
            .overrideConfigKey("quarkus.rest-client.\"client\".enable-local-proxy", "true")
            .overrideConfigKey("quarkus.rest-client.\"client\".url", "quarkus://vertx-http")
            .overrideConfigKey("quarkus.http.test-port", "0");

    @Inject
    ValueRegistry valueRegistry;

    @RestClient
    Client client;

    @Inject
    RestClientsConfig restClientsConfig;

    @Test
    void proxyForwardsToBoundRandomPort() {
        int boundPort = valueRegistry.get(HttpServer.HTTP_TEST_PORT);
        assertThat(boundPort).isPositive().isNotEqualTo(8081);

        String proxyUrl = restClientsConfig.clients().get(Client.class.getName()).overrideUri().orElseThrow();
        given().baseUri(proxyUrl)
                .get("random-port/local-port")
                .then()
                .statusCode(200)
                .body(equalTo(String.valueOf(boundPort)));

        assertThat(client.localPort()).isEqualTo(boundPort);
    }

    @Path("random-port")
    @RegisterRestClient(configKey = "client")
    public interface Client {

        @GET
        @Path("local-port")
        int localPort();
    }

    @Path("random-port")
    public static class Resource {

        @GET
        @Path("local-port")
        public int localPort(@Context HttpServerRequest request) {
            return request.localAddress().port();
        }
    }
}
