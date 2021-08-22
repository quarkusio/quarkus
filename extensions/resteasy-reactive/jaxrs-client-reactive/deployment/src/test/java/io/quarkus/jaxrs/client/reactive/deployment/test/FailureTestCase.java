package io.quarkus.jaxrs.client.reactive.deployment.test;

import java.net.URL;

import javax.enterprise.event.Observes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientResponseFilter;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
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
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
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
