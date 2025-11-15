package io.quarkus.rest.client.reactive;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.mutiny.Uni;

public class RequestCancellationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Client.class, Resource.class));

    @TestHTTPResource
    URI uri;

    @Test
    public void test() throws InterruptedException {
        Client client = RestClientBuilder.newBuilder()
                .baseUri(uri)
                .property(QuarkusRestClientProperties.CONNECTION_POOL_SIZE, 1) // make sure client requests are serialized
                .build(Client.class);

        when().get("resource/count")
                .then()
                .statusCode(200)
                .body(is("0"));

        CountDownLatch latch = new CountDownLatch(1);
        client.get().subscribe().with(res -> {
            latch.countDown();
        });

        // create a bunch of requests that we test won't end up hitting the server
        Uni.join().all(IntStream.range(0, 100).mapToObj(i -> client.get()).toList())
                .andCollectFailures()
                .subscribe() // actually initiate the requests
                .with(res -> {
                })
                .cancel(); // cancel all the requests

        latch.await(5, TimeUnit.SECONDS);

        // ensure that only the first request was made
        when().get("resource/count")
                .then()
                .statusCode(200)
                .body(is("1"));
    }

    @Path("resource")
    public interface Client {

        @GET
        Uni<String> get();
    }

    @Path("resource")
    public static class Resource {

        private static final AtomicLong COUNTER = new AtomicLong();

        @GET
        public String get() throws InterruptedException {
            COUNTER.incrementAndGet();
            // ensure that each request takes a long time to complete to we don't end up with a race
            // condition where the client requests that were to be canceled, had time to execute because
            // the previous requests in the queue completed too fast
            Thread.sleep(2000);
            return "foo";
        }

        @Path("count")
        @GET
        public long count() {
            return COUNTER.get();
        }
    }
}
