package io.quarkus.jaxrs.client.reactive.deployment.test;

import java.net.URL;
import java.util.stream.Collectors;

import jakarta.enterprise.event.Observes;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

import org.jboss.resteasy.reactive.client.impl.UniInvoker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class QueryParamNoTemplateTestCase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Endpoint.class));

    @TestHTTPResource
    URL url;

    private Client client;

    @BeforeEach
    public void before() {
        client = ClientBuilder.newBuilder().build();
    }

    @AfterEach
    public void after() {
        client.close();
    }

    @Test
    public void testInjection() {
        Object data = client.target(url.toExternalForm() + "/hello")
                .queryParam("param", "{foo&bar}", "%FF")
                .request()
                .rx(UniInvoker.class)
                .get()
                .await()
                .indefinitely();
        Assertions.assertEquals("%FF,{foo&bar}", data);
    }

    @Test
    public void testEmptyQueryParam() {
        Object data = client.target(url.toExternalForm() + "/absoluteURI")
                // Empty query param should be omitted in the generated URI
                .queryParam("empty")
                .queryParam("param", "a")
                .request()
                .rx(UniInvoker.class)
                .get()
                .await()
                .indefinitely();
        Assertions.assertEquals("http://localhost:8081//absoluteURI?param=a", data);
    }

    public static class Endpoint {

        public void setup(@Observes Router router) {
            router.route("/hello").handler(new Handler<RoutingContext>() {
                @Override
                public void handle(RoutingContext event) {
                    event.response()
                            .end(
                                    event.queryParam("param")
                                            .stream()
                                            .sorted()
                                            .collect(Collectors.joining(",")));
                }
            });
            router.route("/absoluteURI").handler(new Handler<RoutingContext>() {
                @Override
                public void handle(RoutingContext event) {
                    event.response().end(event.request().absoluteURI());
                }
            });
        }

    }

}
