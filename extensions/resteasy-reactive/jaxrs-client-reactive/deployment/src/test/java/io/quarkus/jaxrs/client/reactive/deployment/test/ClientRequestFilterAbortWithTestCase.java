package io.quarkus.jaxrs.client.reactive.deployment.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;

import javax.enterprise.event.Observes;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class ClientRequestFilterAbortWithTestCase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Endpoint.class));

    @TestHTTPResource
    URL url;

    private Client client;

    @BeforeEach
    public void before() {
        client = ClientBuilder.newClient().register(TestClientRequestFilter.class);
    }

    @AfterEach
    public void after() {
        client.close();
    }

    @Test
    public void test() {
        Response response = client.target(url.toExternalForm() + "/hello").request().get();
        assertEquals(999, response.getStatus());
    }

    public static class Endpoint {

        public void setup(@Observes Router router) {
            router.route("/hello").handler(new Handler<>() {
                @Override
                public void handle(RoutingContext event) {
                    event.response().setStatusCode(200).end();
                }
            });
        }
    }

    @Provider
    public static class TestClientRequestFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext requestContext) {
            requestContext.abortWith(Response.status(999).build());
        }
    }
}
