package io.quarkus.oidc.common.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.spiffe.client.SpiffeClient;
import io.quarkus.spiffe.svid.jwt.WorkloadJsonWebToken;
import io.quarkus.spiffe.svid.x509.WorkloadCertificateDocument;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

public class SpiffeClientAssertionProviderTest {

    private static final String AUDIENCE = "https://service.example.com";
    private static final String SPIFFE_ID = "spiffe://example.org/workload";
    private static final String ISSUER = "https://example.org";

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
        WorkloadJsonWebToken svid = new TestJwtSvid(token, ISSUER, SPIFFE_ID, Set.of(AUDIENCE),
                Instant.now().plusSeconds(60));
        var spiffeClient = new StubSpiffeClient(svid);
        try (var provider = new SpiffeClientAssertionProvider(spiffeClient, AUDIENCE, vertx)) {
            String firstResult = awaitAssertion(provider);
            String secondResult = awaitAssertion(provider);
            assertNotNull(firstResult);
            assertEquals(firstResult, secondResult);
            // explicit audience: the token is requested with it, the stub throws if the default audiences method is called
            assertEquals(1, spiffeClient.fetchCount());
        }
    }

    @Test
    public void testGetClientAssertionWithDefaultAudiences() {
        String token = createSpiffeSvidToken();
        WorkloadJsonWebToken svid = new TestJwtSvid(token, ISSUER, SPIFFE_ID, Set.of(AUDIENCE),
                Instant.now().plusSeconds(60));
        AtomicInteger defaultAudiencesFetchCount = new AtomicInteger();
        SpiffeClient spiffeClient = new StubSpiffeClient(svid) {
            @Override
            public Uni<WorkloadJsonWebToken> getWorkloadJsonWebToken() {
                defaultAudiencesFetchCount.incrementAndGet();
                return Uni.createFrom().item(svid);
            }
        };
        // null audience: the default audiences configured for the SPIFFE client are used
        try (var provider = new SpiffeClientAssertionProvider(spiffeClient, null, vertx)) {
            assertEquals(token, awaitAssertion(provider));
            assertEquals(token, awaitAssertion(provider));
            assertEquals(1, defaultAudiencesFetchCount.get());
            // the explicit audience methods must not be called
            assertEquals(0, ((StubSpiffeClient) spiffeClient).fetchCount());
        }
    }

    @Test
    public void testHasDefaultAudiences() {
        String property = "quarkus.spiffe-client.audiences";
        assertFalse(SpiffeClientAssertionProvider.hasDefaultAudiences());
        System.setProperty(property, AUDIENCE + ",https://other.example.com");
        try {
            assertTrue(SpiffeClientAssertionProvider.hasDefaultAudiences());
        } finally {
            System.clearProperty(property);
        }
        assertFalse(SpiffeClientAssertionProvider.hasDefaultAudiences());
    }

    @Test
    public void testTokenRefreshOnExpiry() {
        String firstToken = createSpiffeSvidToken();
        String secondToken = createSpiffeSvidToken();
        SpiffeClient spiffeClient = new StubSpiffeClient(null) {
            @Override
            public Uni<WorkloadJsonWebToken> getWorkloadJsonWebToken(String audience) {
                int idx = fetchCount.getAndIncrement();
                String token = idx == 0 ? firstToken : secondToken;
                return Uni.createFrom().item(new TestJwtSvid(token, ISSUER, SPIFFE_ID, Set.of(audience),
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
        WorkloadJsonWebToken svid = new TestJwtSvid(createShortLivedSpiffeSvidToken(), ISSUER, SPIFFE_ID,
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

    private static WorkloadJsonWebToken createJwtSvid() {
        return new TestJwtSvid(createSpiffeSvidToken(), ISSUER, SPIFFE_ID, Set.of(AUDIENCE),
                Instant.now().plusSeconds(4));
    }

    private record TestJwtSvid(String token, String issuer, String subject, Set<String> audience,
            Instant expiry) implements WorkloadJsonWebToken {
    }

    private static String awaitAssertion(SpiffeClientAssertionProvider provider) {
        return provider.getClientAssertion().await().atMost(Duration.ofSeconds(5));
    }

    private static class StubSpiffeClient implements SpiffeClient {

        private final WorkloadJsonWebToken svid;
        protected final AtomicInteger fetchCount = new AtomicInteger();

        private StubSpiffeClient(WorkloadJsonWebToken svid) {
            this.svid = svid;
        }

        int fetchCount() {
            return fetchCount.get();
        }

        @Override
        public Uni<WorkloadCertificateDocument> getWorkloadCertificate() {
            throw new IllegalStateException("not implemented for this test");
        }

        @Override
        public Uni<WorkloadJsonWebToken> getWorkloadJsonWebToken() {
            throw new IllegalStateException("not implemented for this test");
        }

        @Override
        public Uni<WorkloadJsonWebToken> getWorkloadJsonWebToken(String audience) {
            fetchCount.incrementAndGet();
            return Uni.createFrom().item(svid);
        }

        @Override
        public Uni<WorkloadJsonWebToken> getWorkloadJsonWebToken(Set<String> audiences) {
            fetchCount.incrementAndGet();
            return Uni.createFrom().item(svid);
        }
    }
}
