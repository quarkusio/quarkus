package io.quarkus.resteasy.reactive.server.test;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.server.Cancellable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

public class CancellableResponseFilterTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Resource.class,
                    NonCancellableResponseFilter.class, OtherResponseFilter.class));

    @BeforeEach
    void setUp() {
        Resource.COUNT.set(0);
        NonCancellableResponseFilter.COUNT.set(0);
        OtherResponseFilter.COUNT.set(0);
    }

    @Inject
    Vertx vertx;

    @TestHTTPResource
    URL url;

    @Test
    public void testFilterRunsOnConnectionClose() {
        WebClient client = WebClient.create(vertx);

        client.get(url.getPort(), url.getHost(), "/test/slow").send();

        try {
            // make sure we did make the proper request
            await().atMost(Duration.ofSeconds(2)).untilAtomic(Resource.COUNT, equalTo(1));

            // this will effectively close the connection
            client.close();

            // make sure we wait until the request could have completed
            Thread.sleep(7_000);

            // the Uni was not cancelled because @Cancellable(false) is on the method,
            // so the count should have been incremented again by the Uni completion
            assertEquals(2, Resource.COUNT.get());

            // the non-cancellable filter should have run
            assertEquals(1, NonCancellableResponseFilter.COUNT.get());

            // the cancellable filter should NOT run when the connection is closed
            assertEquals(0, OtherResponseFilter.COUNT.get());
        } catch (InterruptedException ignored) {

        } finally {
            try {
                client.close();
            } catch (Exception ignored) {

            }
        }
    }

    @Test
    public void testFilterRunsNormally() {
        WebClient client = WebClient.create(vertx);

        client.get(url.getPort(), url.getHost(), "/test/slow").send()
                .onComplete(ar -> {
                });

        try {
            // both filters run on a normal request
            await().atMost(Duration.ofSeconds(10)).untilAtomic(NonCancellableResponseFilter.COUNT, equalTo(1));
            await().atMost(Duration.ofSeconds(10)).untilAtomic(OtherResponseFilter.COUNT, equalTo(1));
            assertEquals(2, Resource.COUNT.get());
        } finally {
            try {
                client.close();
            } catch (Exception ignored) {

            }
        }
    }

    @Path("test")
    public static class Resource {

        public static final AtomicInteger COUNT = new AtomicInteger(0);

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        @Path("slow")
        @Cancellable(false)
        public Uni<String> slowHello() {
            COUNT.incrementAndGet();
            return Uni.createFrom().item("Hello, world")
                    .onItem().delayIt().by(Duration.ofSeconds(5))
                    .onItem().invoke(() -> COUNT.incrementAndGet());
        }
    }

    @Provider
    @Cancellable(false)
    public static class NonCancellableResponseFilter implements ContainerResponseFilter {

        public static final AtomicInteger COUNT = new AtomicInteger(0);

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
                throws IOException {
            COUNT.incrementAndGet();
        }
    }

    @Provider
    public static class OtherResponseFilter implements ContainerResponseFilter {

        public static final AtomicInteger COUNT = new AtomicInteger(0);

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
                throws IOException {
            COUNT.incrementAndGet();
        }
    }
}
