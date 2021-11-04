package io.quarkus.jaxrs.client.reactive.deployment.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import javax.enterprise.event.Observes;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestFilter;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientResponseFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class AsyncRestClientFilterTestCase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Endpoint.class));

    @TestHTTPResource
    URL url;

    private Client client;

    @BeforeEach
    public void before() {
        client = ClientBuilder.newClient()
                .register(SyncClientRequestFilter.class)
                .register(AsyncClientRequestFilter.class)
                .register(AsyncClientResponseFilter.class);
    }

    @AfterEach
    public void after() {
        client.close();
    }

    @Test
    public void test() {
        Response response = client.target(url.toExternalForm() + "/hello").request().get();
        assertEquals(201, response.getStatus());
        MultivaluedMap<String, Object> headers = response.getHeaders();
        assertEquals("foo", headers.getFirst("sync"));
        assertEquals("bar", headers.getFirst("async"));
    }

    public static class Endpoint {

        public void setup(@Observes Router router) {
            router.route("/hello").handler(new Handler<RoutingContext>() {
                @Override
                public void handle(RoutingContext event) {
                    String syncHeader = event.request().getHeader("sync");
                    String asyncHeader = event.request().getHeader("async");
                    event.response()
                            .putHeader("content-type", "text/plain")
                            .putHeader("sync", syncHeader)
                            .putHeader("async", asyncHeader)
                            .end();
                }
            });
        }
    }

    @Provider
    public static class SyncClientRequestFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext context) throws IOException {
            context.getHeaders().add("sync", "foo");
        }
    }

    @Provider
    public static class AsyncClientRequestFilter implements ResteasyReactiveClientRequestFilter {

        @Override
        public void filter(ResteasyReactiveClientRequestContext requestContext) {
            requestContext.suspend();
            Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> "bar"))
                    .onItem().delayIt().by(Duration.ofSeconds(3))
                    .subscribe().with(new Consumer<String>() {
                        @Override
                        public void accept(String s) {
                            requestContext.getHeaders().add("async", s);
                            requestContext.resume();
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable t) {
                            requestContext.resume(t);
                        }
                    });
        }
    }

    @Provider
    public static class AsyncClientResponseFilter implements ResteasyReactiveClientResponseFilter {

        @Override
        public void filter(ResteasyReactiveClientRequestContext requestContext, ClientResponseContext responseContext) {
            requestContext.suspend();
            Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> 201))
                    .onItem().delayIt().by(Duration.ofSeconds(3))
                    .subscribe().with(new Consumer<Integer>() {
                        @Override
                        public void accept(Integer r) {
                            responseContext.setStatus(r);
                            requestContext.resume();
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable t) {
                            requestContext.resume(t);
                        }
                    });
        }
    }
}
