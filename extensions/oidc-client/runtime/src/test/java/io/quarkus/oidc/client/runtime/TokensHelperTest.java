package io.quarkus.oidc.client.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.Tokens;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;

class TokensHelperTest {

    private static final Duration TIME_SKEW = Duration.ofSeconds(10);

    private static Tokens tokens(String accessToken, long expiresInSecs) {
        final long nowSecs = System.currentTimeMillis() / 1000;
        return new Tokens(accessToken, nowSecs + expiresInSecs, TIME_SKEW, null, null, new JsonObject(), "client");
    }

    /**
     * While a proactive refresh is in flight, callers must keep receiving the token being replaced
     * rather than blocking on the refresh: it is still valid, because the refresh was started
     * early by the refresh token time skew.
     */
    @Test
    void testStillValidTokensServedWhileRefreshIsInFlight() throws Exception {
        // Expires in 8s: inside the 10s skew, so it is marked for refresh immediately, yet with
        // comfortably more life left than the margin required to keep serving it. That is exactly
        // the situation this covers, and it keeps the test clear of the boundary in both
        // directions.
        Tokens current = tokens("current", 8);
        assertFalse(current.isAccessTokenExpired());
        assertTrue(current.isAccessTokenWithinRefreshInterval());

        BlockingOidcClient client = new BlockingOidcClient(tokens("refreshed", 300));
        TokensHelper helper = new TokensHelper();
        helper.initTokens(new ImmediateOidcClient(current), Map.of());

        // Triggers the refresh, which then blocks.
        Uni<Tokens> refreshing = helper.getTokens(client, Map.of(), false);
        refreshing.subscribe().with(t -> {
        }, t -> {
        });

        // Wait until the refresh is genuinely in flight, rather than assuming it is by now.
        client.awaitInFlight();

        // A second caller arriving during that refresh must not wait for it.
        Tokens served = helper.getTokens(client, Map.of(), false).await().atMost(Duration.ofSeconds(2));
        assertSame(current, served, "the still-valid previous tokens should have been served");

        // And the refresh itself must not have been duplicated.
        assertEquals(1, client.acquisitions.get());

        client.release.countDown();
    }

    /**
     * Expired tokens are never served, even though a refresh is already in flight: the caller
     * waits for it, exactly as a caller waits when it starts a refresh itself. This is the same
     * rule applied when no refresh is running -- an expired access token is not usable.
     */
    @Test
    void testExpiredTokensNotServedWhileRefreshIsInFlight() throws Exception {
        // Already expired, so there is nothing safe to reuse.
        Tokens current = tokens("current", -1);
        assertTrue(current.isAccessTokenExpired());

        BlockingOidcClient client = new BlockingOidcClient(tokens("refreshed", 300));
        TokensHelper helper = new TokensHelper();
        helper.initTokens(new ImmediateOidcClient(current), Map.of());

        helper.getTokens(client, Map.of(), false).subscribe().with(t -> {
        }, t -> {
        });
        client.awaitInFlight();

        // Release the refresh so the waiting caller can complete, then confirm it waited for the
        // new tokens rather than being handed the expired ones.
        client.release.countDown();
        Tokens served = helper.getTokens(client, Map.of(), false).await().atMost(Duration.ofSeconds(5));
        assertEquals("refreshed", served.getAccessToken(),
                "expired tokens must not be served; the refreshed ones should be");
    }

    /**
     * Short-lived tokens are reused on the same terms as any other, so the optimisation still
     * applies when the skew -- and therefore the remaining lifetime once a refresh starts -- is
     * small. An extra safety margin here would have silently excluded exactly these deployments.
     */
    @Test
    void testShortLivedTokensStillServedWhileRefreshIsInFlight() throws Exception {
        // A 3s skew means a refresh starts once fewer than 3s of life remain, so 2s remaining
        // both triggers the refresh and leaves the tokens usable while it runs.
        final long nowSecs = System.currentTimeMillis() / 1000;
        Tokens current = new Tokens("current", nowSecs + 2, Duration.ofSeconds(3), null, null,
                new JsonObject(), "client");
        assertFalse(current.isAccessTokenExpired());
        assertTrue(current.isAccessTokenWithinRefreshInterval());

        BlockingOidcClient client = new BlockingOidcClient(tokens("refreshed", 300));
        TokensHelper helper = new TokensHelper();
        helper.initTokens(new ImmediateOidcClient(current), Map.of());

        helper.getTokens(client, Map.of(), false).subscribe().with(t -> {
        }, t -> {
        });
        client.awaitInFlight();

        Tokens served = helper.getTokens(client, Map.of(), false).await().atMost(Duration.ofSeconds(2));
        assertSame(current, served, "short-lived tokens with life left should still be served");

        client.release.countDown();
    }

    /**
     * An OidcClient whose token acquisition blocks until released, so that the in-flight refresh
     * window can be observed deterministically.
     */
    private static final class BlockingOidcClient implements OidcClient {

        private final CountDownLatch release = new CountDownLatch(1);
        /** Counts down once acquisition has actually begun, so tests never guess at timing. */
        private final CountDownLatch started = new CountDownLatch(1);
        private final AtomicInteger acquisitions = new AtomicInteger();
        private final Tokens newTokens;

        BlockingOidcClient(Tokens newTokens) {
            this.newTokens = newTokens;
        }

        /** Blocks until a token request is genuinely in flight. */
        void awaitInFlight() throws InterruptedException {
            assertTrue(started.await(10, TimeUnit.SECONDS), "token acquisition should have started");
        }

        @Override
        public Uni<Tokens> getTokens() {
            return getTokens(Map.of());
        }

        @Override
        public Uni<Tokens> getTokens(Map<String, String> additionalGrantParameters) {
            acquisitions.incrementAndGet();
            // Blocks on a separate thread so the refresh stays genuinely in flight while the
            // test thread makes its second call; running it on the subscribing thread would
            // complete the refresh before that call happens.
            return Uni.createFrom().item(() -> {
                try {
                    started.countDown();
                    release.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return newTokens;
            }).runSubscriptionOn(Executors.newSingleThreadExecutor());
        }

        @Override
        public Uni<Tokens> refreshTokens(String refreshToken) {
            return refreshTokens(refreshToken, Map.of());
        }

        @Override
        public Uni<Tokens> refreshTokens(String refreshToken, Map<String, String> additionalGrantParameters) {
            return getTokens(additionalGrantParameters);
        }

        @Override
        public Uni<Boolean> revokeAccessToken(String accessToken) {
            return Uni.createFrom().item(true);
        }

        @Override
        public Uni<Boolean> revokeAccessToken(String accessToken, Map<String, String> additionalParameters) {
            return Uni.createFrom().item(true);
        }

        @Override
        public void close() {
            // no-op in the tests
        }
    }

    /** An OidcClient that returns a fixed set of tokens without blocking. */
    private record ImmediateOidcClient(Tokens tokens) implements OidcClient {

        @Override
        public Uni<Tokens> getTokens() {
            return Uni.createFrom().item(tokens);
        }

        @Override
        public Uni<Tokens> getTokens(Map<String, String> additionalGrantParameters) {
            return Uni.createFrom().item(tokens);
        }

        @Override
        public Uni<Tokens> refreshTokens(String refreshToken) {
            return Uni.createFrom().item(tokens);
        }

        @Override
        public Uni<Tokens> refreshTokens(String refreshToken, Map<String, String> additionalGrantParameters) {
            return Uni.createFrom().item(tokens);
        }

        @Override
        public Uni<Boolean> revokeAccessToken(String accessToken) {
            return Uni.createFrom().item(true);
        }

        @Override
        public Uni<Boolean> revokeAccessToken(String accessToken, Map<String, String> additionalParameters) {
            return Uni.createFrom().item(true);
        }

        @Override
        public void close() {
            // no-op in the test
        }
    }
}
