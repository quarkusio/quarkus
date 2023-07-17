package io.quarkus.jaxrs.client.reactive.deployment.test;

import java.net.URL;

import jakarta.enterprise.event.Observes;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientResponseFilter;
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

public class FailureTestCase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Endpoint.class));

    @TestHTTPResource
    URL url;

    private Client client;

    @BeforeEach
    public void before() {
        client = ClientBuilder.newClient().register(TestClientResponseFilter.class);
    }

    @AfterEach
    public void after() {
        client.close();
    }

    @Test
    public void test() {
        Assertions.assertThrows(WebApplicationException.class,
                () -> client.target(url.toExternalForm() + "/hello").request().get());
    }

    public static class Endpoint {

        public void setup(@Observes Router router) {
            router.route("/hello").handler(new Handler<RoutingContext>() {
                @Override
                public void handle(RoutingContext event) {
                    event.response().end();
                }
            });
        }
    }

    @Provider
    public static class TestClientResponseFilter implements ResteasyReactiveClientResponseFilter {

        @Override
        public void filter(ResteasyReactiveClientRequestContext requestContext, ClientResponseContext responseContext) {
            //make sure the response builder works with no server components installed
            throw new WebApplicationException(Response.status(500).build());
        }
    }
}
