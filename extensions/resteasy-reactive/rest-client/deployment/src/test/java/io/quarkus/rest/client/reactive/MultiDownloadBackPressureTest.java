package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

public class MultiDownloadBackPressureTest {

    private static final int CHUNK_SIZE = 64 * 1024;
    private static final int CHUNKS = 32 * 1024;

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Resource.class, Client.class));

    @TestHTTPResource
    URI uri;

    @Test
    void chunksAreFetchedOnDemand() throws InterruptedException {
        Client client = RestClientBuilder.newBuilder().baseUri(uri).build(Client.class);
        Resource.emitted.set(0);

        CountingSubscriber subscriber = new CountingSubscriber();
        client.download().subscribe().withSubscriber(subscriber);
        subscriber.request(2);
        await().atMost(Duration.ofSeconds(10)).until(() -> subscriber.received.get() == 2);
        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(10))
                .until(() -> subscriber.received.get() == 2 && Resource.emitted.get() < 512);

        subscriber.request(Long.MAX_VALUE);
        assertThat(subscriber.completed.await(120, TimeUnit.SECONDS)).isTrue();
        assertThat(subscriber.failure.get()).isNull();
        assertThat(subscriber.bytes.get()).isEqualTo((long) CHUNKS * CHUNK_SIZE);
        assertThat(Resource.emitted.get()).isEqualTo(CHUNKS);
    }

    @Test
    void cancellingStopsTheDownload() {
        Client client = RestClientBuilder.newBuilder().baseUri(uri).build(Client.class);
        Resource.emitted.set(0);

        AssertSubscriber<byte[]> subscriber = client.download().subscribe().withSubscriber(AssertSubscriber.create(1));
        subscriber.awaitItems(1, Duration.ofSeconds(10));
        subscriber.cancel();
        AtomicInteger lastSeen = new AtomicInteger(-1);
        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(10))
                .until(() -> {
                    int now = Resource.emitted.get();
                    return now == lastSeen.getAndSet(now);
                });
        assertThat(Resource.emitted.get()).isLessThan(CHUNKS);
    }

    static class CountingSubscriber implements Flow.Subscriber<byte[]> {

        final AtomicInteger received = new AtomicInteger();
        final AtomicLong bytes = new AtomicLong();
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final CountDownLatch completed = new CountDownLatch(1);
        volatile Flow.Subscription subscription;

        void request(long n) {
            subscription.request(n);
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
        }

        @Override
        public void onNext(byte[] item) {
            received.incrementAndGet();
            bytes.addAndGet(item.length);
        }

        @Override
        public void onError(Throwable throwable) {
            failure.set(throwable);
            completed.countDown();
        }

        @Override
        public void onComplete() {
            completed.countDown();
        }
    }

    @Path("/download")
    public static class Resource {

        static final AtomicInteger emitted = new AtomicInteger();

        @GET
        @Produces("application/octet-stream")
        public Multi<byte[]> download() {
            byte[] chunk = new byte[CHUNK_SIZE];
            return Multi.createFrom().range(0, CHUNKS).map(i -> chunk).onItem().invoke(emitted::incrementAndGet);
        }
    }

    @Path("/download")
    public interface Client {

        @GET
        Multi<byte[]> download();
    }
}
