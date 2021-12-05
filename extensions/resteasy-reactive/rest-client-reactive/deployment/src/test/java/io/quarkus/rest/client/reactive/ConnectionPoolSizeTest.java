package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.headers.ClientHeaderParamFromPropertyTest;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

public class ConnectionPoolSizeTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class).addClasses(ClientHeaderParamFromPropertyTest.Client.class));

    @TestHTTPResource
    URI uri;

    @Test
    void shouldPerform20CallsWithoutQueuing() throws InterruptedException {
        Client client = RestClientBuilder.newBuilder().baseUri(uri)
                .build(Client.class);

        CountDownLatch latch = executeCalls(client, 20);

        assertThat(latch.await(2, TimeUnit.SECONDS))
                .overridingErrorMessage("Failed to do 20 calls in 2 seconds")
                .isTrue();
    }

    @Test
    @Timeout(5)
    void shouldPerform21CallsWithQueuing() throws InterruptedException {
        Client client = RestClientBuilder.newBuilder().baseUri(uri)
                .build(Client.class);

        long start = System.currentTimeMillis();
        CountDownLatch latch = executeCalls(client, 21);
        latch.await();

        assertThat(System.currentTimeMillis() - start).isLessThan(3000).isGreaterThanOrEqualTo(2000);
    }

    @Test
    @Timeout(5)
    void shouldPerform5CallsWithoutQueueingOnQueue6() throws InterruptedException {
        Client client = RestClientBuilder.newBuilder().baseUri(uri)
                .property(QuarkusRestClientProperties.CONNECTION_POOL_SIZE, 6)
                .build(Client.class);

        long start = System.currentTimeMillis();
        CountDownLatch latch = executeCalls(client, 5);
        latch.await();

        assertThat(System.currentTimeMillis() - start).isLessThan(2000);
    }

    @Test
    @Timeout(5)
    void shouldPerform5CallsWithQueueingOnQueue4() throws InterruptedException {
        Client client = RestClientBuilder.newBuilder().baseUri(uri)
                .property(QuarkusRestClientProperties.CONNECTION_POOL_SIZE, 4)
                .build(Client.class);

        long start = System.currentTimeMillis();
        CountDownLatch latch = executeCalls(client, 5);
        latch.await();

        assertThat(System.currentTimeMillis() - start).isLessThan(3000).isGreaterThanOrEqualTo(2000);
    }

    private CountDownLatch executeCalls(Client client, int callAmount) {
        ExecutorService executorService = Executors.newFixedThreadPool(callAmount);
        CountDownLatch latch = new CountDownLatch(callAmount);
        for (int i = 0; i < callAmount; i++) {
            executorService.execute(() -> {
                String result = client.get();
                latch.countDown();
                assertThat(result).isEqualTo("hello, world!");
            });
        }
        return latch;
    }

    @Path("/hello")
    public interface Client {
        @GET
        String get();
    }

    @Path("/hello")
    public static class SlowResource {
        @Inject
        Vertx vertx;

        @GET
        public Uni<String> getSlowly() {
            return Uni.createFrom().emitter(emitter -> vertx.setTimer(1000 /* ms */,
                    val -> emitter.complete("hello, world!")));
        }
    }

}
