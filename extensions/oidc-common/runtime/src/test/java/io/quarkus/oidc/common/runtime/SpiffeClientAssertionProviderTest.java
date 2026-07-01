package io.quarkus.oidc.common.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.spiffe.client.api.JwtSvid;
import io.quarkus.spiffe.client.api.JwtSvidRequest;
import io.quarkus.spiffe.client.api.SpiffeClient;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

public class SpiffeClientAssertionProviderTest {

    private static final String AUDIENCE = "https://service.example.com";
    private static final String SPIFFE_ID = "spiffe://example.org/workload";

    private static Vertx vertx;

    @BeforeAll
    static void startVertx() {
        vertx = Vertx.vertx();
    }

    @AfterAll
    static void stopVertx() {
        if (vertx != null) {
            vertx.close().toCompletionStage().toCompletableFuture().join();
        }
    }

    @Test
    public void testGetClientAssertionType() {
        try (var provider = new SpiffeClientAssertionProvider(new StubSpiffeClient(createJwtSvid()), AUDIENCE, vertx)) {
            assertEquals(OidcConstants.SPIFFE_SVID_CLIENT_ASSERTION_TYPE, provider.getClientAssertionType());
        }
    }

    @Test
    public void testGetClientAssertionReturnsCachedToken() {
        String token = createSpiffeSvidToken();
        JwtSvid svid = new TestJwtSvid(token, SPIFFE_ID, Set.of(AUDIENCE),
                Instant.now().plusSeconds(60));
        var spiffeClient = new StubSpiffeClient(svid);
        try (var provider = new SpiffeClientAssertionProvider(spiffeClient, AUDIENCE, vertx)) {
            String firstResult = awaitAssertion(provider);
            String secondResult = awaitAssertion(provider);
            assertNotNull(firstResult);
            assertEquals(firstResult, secondResult);
            assertEquals(1, spiffeClient.fetchCount());
        }
    }

    @Test
    public void testTokenRefreshOnExpiry() {
        String firstToken = createSpiffeSvidToken();
        String secondToken = createSpiffeSvidToken();
        SpiffeClient spiffeClient = new StubSpiffeClient(null) {
            @Override
            public Uni<JwtSvid> fetchJwtSvid(JwtSvidRequest request) {
                int idx = fetchCount.getAndIncrement();
                String token = idx == 0 ? firstToken : secondToken;
                return Uni.createFrom().item(new TestJwtSvid(token, SPIFFE_ID, Set.of(AUDIENCE),
                        Instant.now().plusSeconds(4)));
            }
        };

        try (var provider = new SpiffeClientAssertionProvider(spiffeClient, AUDIENCE, vertx)) {
            String initialToken = awaitAssertion(provider);
            assertEquals(firstToken, initialToken);

            Awaitility.await().atMost(Duration.ofSeconds(10))
                    .untilAsserted(() -> {
                        String current = awaitAssertion(provider);
                        assertNotEquals(firstToken, current);
                        assertEquals(secondToken, current);
                    });
        }
    }

    @Test
    public void testCloseStopsRefresh() throws InterruptedException {
        JwtSvid svid = new TestJwtSvid(createShortLivedSpiffeSvidToken(), SPIFFE_ID,
                Set.of(AUDIENCE), Instant.now().plusSeconds(1));
        var spiffeClient = new StubSpiffeClient(svid);

        SpiffeClientAssertionProvider provider = new SpiffeClientAssertionProvider(spiffeClient, AUDIENCE, vertx);
        awaitAssertion(provider);
        provider.close();
        Thread.sleep(100); // for unlikely case refresh is in a progress
        int countAtClose = spiffeClient.fetchCount();

        // wait well past the refresh interval (85% of 1s = 850ms) then verify no more fetches
        Thread.sleep(1400);
        assertEquals(countAtClose, spiffeClient.fetchCount());
    }

    private static String createSpiffeSvidToken() {
        return Jwt.subject(SPIFFE_ID)
                .issuer("https://server.example.com")
                .audience(AUDIENCE)
                .expiresIn(Duration.ofSeconds(4))
                .signWithSecret("43".repeat(20));
    }

    private static String createShortLivedSpiffeSvidToken() {
        return Jwt.subject(SPIFFE_ID)
                .issuer("https://server.example.com")
                .audience(AUDIENCE)
                .expiresIn(Duration.ofSeconds(1))
                .signWithSecret("43".repeat(20));
    }

    private static JwtSvid createJwtSvid() {
        return new TestJwtSvid(createSpiffeSvidToken(), SPIFFE_ID, Set.of(AUDIENCE),
                Instant.now().plusSeconds(4));
    }

    private record TestJwtSvid(String token, String spiffeId, Set<String> audience,
            Instant expiry) implements JwtSvid {
    }

    private static String awaitAssertion(SpiffeClientAssertionProvider provider) {
        return provider.getClientAssertion().await().atMost(Duration.ofSeconds(5));
    }

    private static class StubSpiffeClient implements SpiffeClient {

        private final JwtSvid svid;
        protected final AtomicInteger fetchCount = new AtomicInteger();

        private StubSpiffeClient(JwtSvid svid) {
            this.svid = svid;
        }

        int fetchCount() {
            return fetchCount.get();
        }

        @Override
        public Uni<JwtSvid> fetchJwtSvid(JwtSvidRequest request) {
            fetchCount.incrementAndGet();
            return Uni.createFrom().item(svid);
        }
    }
}
