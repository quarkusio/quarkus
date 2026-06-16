package io.quarkus.resteasy.reactive.server.test;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.server.Cancellable;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

public class CancellableBlockingResponseFilterTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(Resource.class,
                    NonCancellableResponseFilter.class, OtherResponseFilter.class, Filters.class));

    @BeforeEach
    void setUp() {
        Resource.COUNT.set(0);
        NonCancellableResponseFilter.COUNT.set(0);
        OtherResponseFilter.COUNT.set(0);
        Filters.NON_CANCELLABLE_COUNT.set(0);
    }

    @Inject
    Vertx vertx;

    @TestHTTPResource
    URL url;

    @Test
    public void testFilterRunsOnConnectionClose() {
        WebClient client = WebClient.create(vertx);

        client.get(url.getPort(), url.getHost(), "/test/hello").send();

        try {
            // make sure we did make the proper request
            await().atMost(Duration.ofSeconds(2)).untilAtomic(Resource.COUNT, equalTo(1));

            // this will effectively close the connection
            client.close();

            // make sure we wait until the request could have completed
            Thread.sleep(7_000);

            // the blocking method completed
            assertEquals(2, Resource.COUNT.get());

            // the non-cancellable filters should have run
            assertEquals(1, NonCancellableResponseFilter.COUNT.get());
            assertEquals(1, Filters.NON_CANCELLABLE_COUNT.get());

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

        CountDownLatch latch = new CountDownLatch(1);
        client.get(url.getPort(), url.getHost(), "/test/hello?sleep=0").send()
                .onComplete(ar -> {
                    latch.countDown();
                });

        try {
            latch.await(10, java.util.concurrent.TimeUnit.SECONDS);

            // all filters run on a normal request
            assertEquals(1, NonCancellableResponseFilter.COUNT.get());
            assertEquals(1, OtherResponseFilter.COUNT.get());
            assertEquals(1, Filters.NON_CANCELLABLE_COUNT.get());
            assertEquals(2, Resource.COUNT.get());
        } catch (InterruptedException ignored) {

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
        @Path("hello")
        @Blocking
        public String hello(@RestQuery @DefaultValue("5000") Integer sleep) {
            COUNT.incrementAndGet();
            if ((sleep != null) && (sleep > 0)) {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException ignored) {

                }
            }
            COUNT.incrementAndGet();
            return "Hello, world";
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

    public static class Filters {

        public static final AtomicInteger NON_CANCELLABLE_COUNT = new AtomicInteger(0);

        @ServerResponseFilter(cancellable = false)
        public void nonCancellableFilter(ContainerResponseContext responseContext) {
            NON_CANCELLABLE_COUNT.incrementAndGet();
        }
    }
}
