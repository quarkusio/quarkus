package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.reactive.RestStreamElementType;
import org.jboss.resteasy.reactive.common.util.RestMediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.MultiDownloadBackPressureTest.CountingSubscriber;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

public class MultiNdjsonBackPressureTest {

    private static final int LINE_SIZE = 64 * 1024;
    private static final int LINES = 32 * 1024;
    private static final int SHORT_LINES = 100;

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Resource.class, Client.class, Line.class, CountingSubscriber.class,
                    TestJacksonBasicMessageBodyReader.class));

    @TestHTTPResource
    URI uri;

    @Test
    void linesAreFetchedOnDemand() throws InterruptedException {
        Client client = RestClientBuilder.newBuilder().baseUri(uri).build(Client.class);
        Resource.emitted.set(0);

        CountingSubscriber subscriber = new CountingSubscriber();
        client.lines().subscribe().withSubscriber(subscriber);
        subscriber.request(2);
        await().atMost(Duration.ofSeconds(10)).until(() -> subscriber.received.get() == 2);
        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(10))
                .until(() -> subscriber.received.get() == 2 && Resource.emitted.get() < 512);

        long lineLength = subscriber.bytes.get() / subscriber.received.get();
        subscriber.request(Long.MAX_VALUE);
        assertThat(subscriber.completed.await(120, TimeUnit.SECONDS)).isTrue();
        assertThat(subscriber.failure.get()).isNull();
        assertThat(subscriber.bytes.get()).isEqualTo((long) LINES * lineLength);
        assertThat(Resource.emitted.get()).isEqualTo(LINES);
    }

    @Test
    void cancellingStopsTheDownload() {
        Client client = RestClientBuilder.newBuilder().baseUri(uri).build(Client.class);
        Resource.emitted.set(0);

        CountingSubscriber subscriber = new CountingSubscriber();
        client.lines().subscribe().withSubscriber(subscriber);
        subscriber.request(Long.MAX_VALUE);
        await().atMost(Duration.ofSeconds(10)).until(() -> subscriber.received.get() > 0);
        subscriber.subscription.cancel();
        AtomicInteger lastSeen = new AtomicInteger(-1);
        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(10))
                .until(() -> {
                    int now = Resource.emitted.get();
                    return now == lastSeen.getAndSet(now);
                });
        assertThat(Resource.emitted.get()).isLessThan(LINES);
    }

    @Test
    void linesRequestedOneAtATimeAreAllDelivered() throws InterruptedException {
        Client client = RestClientBuilder.newBuilder().baseUri(uri).build(Client.class);

        // the whole body fits in one buffer, so the end of the response is queued behind lines nobody asked for yet
        CountingSubscriber subscriber = new CountingSubscriber();
        client.shortLines().subscribe().withSubscriber(subscriber);
        for (int i = 1; i <= SHORT_LINES; i++) {
            subscriber.request(1);
            int expected = i;
            await().atMost(Duration.ofSeconds(10)).until(() -> subscriber.received.get() == expected);
        }
        // the demand is met exactly at the end of the body, so completion is signalled on the next request
        subscriber.request(1);
        assertThat(subscriber.completed.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(subscriber.failure.get()).isNull();
        assertThat(subscriber.received.get()).isEqualTo(SHORT_LINES);
    }

    @Test
    void emptyLinesDoNotStallTheDemand() throws InterruptedException {
        Client client = RestClientBuilder.newBuilder().baseUri(uri).build(Client.class);

        CountingSubscriber subscriber = new CountingSubscriber();
        client.emptyLines().subscribe().withSubscriber(subscriber);
        subscriber.request(1);
        await().atMost(Duration.ofSeconds(10)).until(() -> subscriber.received.get() == 1);
        subscriber.request(1);
        await().atMost(Duration.ofSeconds(10)).until(() -> subscriber.received.get() == 2);
        subscriber.request(1);
        assertThat(subscriber.completed.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(subscriber.failure.get()).isNull();
    }

    @Test
    void malformedLastLineFailsTheMulti() {
        Client client = QuarkusRestClientBuilder.newBuilder().baseUri(uri)
                .register(new TestJacksonBasicMessageBodyReader()).build(Client.class);

        AssertSubscriber<Line> subscriber = client.malformedLastLine().subscribe()
                .withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));
        subscriber.awaitFailure(Duration.ofSeconds(10));
        assertThat(subscriber.getItems()).extracting(line -> line.name).containsExactly("first");
    }

    public static class Line {

        public String name;
    }

    @Path("/lines")
    public static class Resource {

        static final AtomicInteger emitted = new AtomicInteger();

        @GET
        @Produces(RestMediaType.APPLICATION_NDJSON)
        @RestStreamElementType(MediaType.APPLICATION_JSON)
        public Multi<String> lines() {
            String line = "a".repeat(LINE_SIZE);
            return Multi.createFrom().range(0, LINES).map(i -> line).onItem().invoke(emitted::incrementAndGet);
        }

        @GET
        @Path("/short")
        @Produces(RestMediaType.APPLICATION_NDJSON)
        @RestStreamElementType(MediaType.APPLICATION_JSON)
        public Multi<String> shortLines() {
            return Multi.createFrom().range(0, SHORT_LINES).map(i -> "line " + i);
        }

        @GET
        @Path("/empty-lines")
        @Produces(RestMediaType.APPLICATION_NDJSON)
        public String emptyLines() {
            return "\nfirst\n\nsecond\n";
        }

        @GET
        @Path("/malformed-last")
        @Produces(RestMediaType.APPLICATION_NDJSON)
        public String malformedLastLine() {
            return "{\"name\":\"first\"}\n{\"name\":";
        }
    }

    @Path("/lines")
    public interface Client {

        @GET
        @Produces(RestMediaType.APPLICATION_NDJSON)
        Multi<byte[]> lines();

        @GET
        @Path("/short")
        @Produces(RestMediaType.APPLICATION_NDJSON)
        Multi<byte[]> shortLines();

        @GET
        @Path("/empty-lines")
        @Produces(RestMediaType.APPLICATION_NDJSON)
        Multi<byte[]> emptyLines();

        @GET
        @Path("/malformed-last")
        @Produces(RestMediaType.APPLICATION_NDJSON)
        @RestStreamElementType(MediaType.APPLICATION_JSON)
        Multi<Line> malformedLastLine();
    }
}
