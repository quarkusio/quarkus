package io.quarkus.rest.client.reactive;

import static org.awaitility.Awaitility.await;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;

// Reproducer for https://github.com/quarkusio/quarkus/issues/57343
public class CancelledQueuedRequestConnectionLeakTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(Client.class, Resource.class));

    @TestHTTPResource
    URI uri;

    @Test
    public void cancelledQueuedRequestShouldNotBlockConnection() {
        Client client = RestClientBuilder.newBuilder()
                .baseUri(uri)
                .property(QuarkusRestClientProperties.CONNECTION_POOL_SIZE, 1)
                .property(QuarkusRestClientProperties.READ_TIMEOUT, 10000L)
                .build(Client.class);

        // A: occupies the only connection for ~1.5 s
        AtomicBoolean aIsDone = new AtomicBoolean();
        client.slow().subscribe().with(res -> aIsDone.set(true), err -> aIsDone.set(true));

        // B: queued behind A (pool size = 1), then cancelled
        Cancellable b = client.fast().subscribe().with(ignored -> {
            // NO-OP
        }, Throwable::printStackTrace);
        b.cancel();

        await().atMost(5, TimeUnit.SECONDS).untilTrue(aIsDone);

        // C: should get the connection promptly, not after B's read-timeout
        AtomicBoolean cIsDone = new AtomicBoolean();
        client.fast().subscribe().with(res -> cIsDone.set(true), err -> cIsDone.set(true));

        // Without the fix for #57343: C is blocked ~10s (read-timeout); < 1s with the fix
        await().atMost(3, TimeUnit.SECONDS).untilTrue(cIsDone);
    }

    @Path("resource")
    public interface Client {

        @GET
        @Path("slow")
        Uni<String> slow();

        @GET
        @Path("fast")
        Uni<String> fast();
    }

    @Path("resource")
    public static class Resource {

        @GET
        @Path("slow")
        public Uni<String> slow() {
            return Uni.createFrom().item("slow")
                    .onItem().delayIt().by(Duration.ofMillis(1500));
        }

        @GET
        @Path("fast")
        public String fast() {
            return "fast";
        }
    }
}
