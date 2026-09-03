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
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.Tokens;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;

class TokensHelperTest {

    private static final Duration TIME_SKEW = Duration.ofSeconds(10);
    /** Reusing the tokens being refreshed is opt-in, so the tests which exercise it configure a minimum. */
    private static final Duration MIN_REMAINING_LIFESPAN = Duration.ofSeconds(2);

    private static Tokens tokens(String accessToken, long expiresInSecs) {
        return tokens(accessToken, expiresInSecs, TIME_SKEW, MIN_REMAINING_LIFESPAN);
    }

    private static Tokens tokens(String accessToken, long expiresInSecs, Duration skew, Duration minRemaining) {
        final long nowSecs = System.currentTimeMillis() / 1000;
        return new Tokens(accessToken, nowSecs + expiresInSecs, skew, null, null, new JsonObject(), "client",
                minRemaining);
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
                new JsonObject(), "client", MIN_REMAINING_LIFESPAN);
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
     * A caller which forces the acquisition of new tokens does so because the tokens it was given
     * were rejected, typically after a 401 when 'refresh-on-unauthorized' is enabled. Handing it
     * the tokens being replaced would just fail again, so it waits for the refresh instead, even
     * though those tokens are still valid and would be served to any other caller.
     */
    @Test
    void testForcedAcquisitionNotServedPreviousTokensWhileRefreshIsInFlight() throws Exception {
        // Still valid, so this is served to ordinary callers: only the forcing is what differs.
        Tokens current = tokens("current", 8);
        assertFalse(current.isAccessTokenExpired());
        assertTrue(current.isAccessTokenWithinRefreshInterval());

        BlockingOidcClient client = new BlockingOidcClient(tokens("refreshed", 300));
        TokensHelper helper = new TokensHelper();
        helper.initTokens(new ImmediateOidcClient(current), Map.of());

        helper.getTokens(client, Map.of(), false).subscribe().with(t -> {
        }, t -> {
        });
        client.awaitInFlight();

        final AtomicReference<Tokens> forced = new AtomicReference<>();
        final CountDownLatch forcedCompleted = new CountDownLatch(1);
        helper.getTokens(client, Map.of(), true).subscribe().with(t -> {
            forced.set(t);
            forcedCompleted.countDown();
        }, t -> forcedCompleted.countDown());

        // The refresh is still held, so the forcing caller must not have been given anything yet.
        // Without this the test would also pass while the rejected tokens are handed back, since
        // both paths eventually produce a value.
        assertFalse(forcedCompleted.await(1, TimeUnit.SECONDS),
                "tokens which have been rejected must not be served to a caller forcing new ones");

        client.release.countDown();
        assertTrue(forcedCompleted.await(10, TimeUnit.SECONDS), "the forcing caller should have completed");
        assertEquals("refreshed", forced.get().getAccessToken(),
                "the forcing caller should have waited for the refreshed tokens");
    }

    /**
     * Tokens which are still valid but too close to expiry are not served while a refresh is in
     * flight: they could expire in transit, or while the target service handles the request. Such a
     * caller waits for the refreshed tokens instead.
     */
    @Test
    void testTokensBelowMinRemainingLifespanNotServedWhileRefreshIsInFlight() throws Exception {
        // 30s skew so the refresh starts early, but only 5s of life left against a 10s minimum:
        // still valid, yet not worth sending.
        Tokens current = tokens("current", 5, Duration.ofSeconds(30), Duration.ofSeconds(10));
        assertFalse(current.isAccessTokenExpired());
        assertTrue(current.isAccessTokenWithinRefreshInterval());
        assertFalse(current.hasMinRemainingAccessTokenLifespan());

        BlockingOidcClient client = new BlockingOidcClient(tokens("refreshed", 300));
        TokensHelper helper = new TokensHelper();
        helper.initTokens(new ImmediateOidcClient(current), Map.of());

        helper.getTokens(client, Map.of(), false).subscribe().with(t -> {
        }, t -> {
        });
        client.awaitInFlight();

        final AtomicReference<Tokens> served = new AtomicReference<>();
        final CountDownLatch completed = new CountDownLatch(1);
        helper.getTokens(client, Map.of(), false).subscribe().with(t -> {
            served.set(t);
            completed.countDown();
        }, t -> completed.countDown());

        // The refresh is still held, so a caller which must not be served the nearly expired tokens
        // has nothing to receive yet.
        assertFalse(completed.await(1, TimeUnit.SECONDS),
                "tokens below the minimum remaining lifespan must not be served");

        client.release.countDown();
        assertTrue(completed.await(10, TimeUnit.SECONDS), "the caller should have completed");
        assertEquals("refreshed", served.get().getAccessToken(),
                "the caller should have waited for the refreshed tokens");
    }

    /**
     * Tokens with more than the minimum remaining lifespan left are served, so the margin only
     * excludes the tokens it is meant to.
     */
    @Test
    void testTokensAboveMinRemainingLifespanServedWhileRefreshIsInFlight() throws Exception {
        // 20s left against a 10s minimum, inside a 30s skew.
        Tokens current = tokens("current", 20, Duration.ofSeconds(30), Duration.ofSeconds(10));
        assertTrue(current.isAccessTokenWithinRefreshInterval());
        assertTrue(current.hasMinRemainingAccessTokenLifespan());

        BlockingOidcClient client = new BlockingOidcClient(tokens("refreshed", 300));
        TokensHelper helper = new TokensHelper();
        helper.initTokens(new ImmediateOidcClient(current), Map.of());

        helper.getTokens(client, Map.of(), false).subscribe().with(t -> {
        }, t -> {
        });
        client.awaitInFlight();

        Tokens served = helper.getTokens(client, Map.of(), false).await().atMost(Duration.ofSeconds(2));
        assertSame(current, served, "tokens above the minimum remaining lifespan should be served");

        client.release.countDown();
    }

    /**
     * A refresh only starts once the remaining lifespan has dropped below the refresh token time
     * skew, so a minimum larger than the skew would stop the tokens from ever being reused and
     * would silently disable the optimisation for deployments with a small skew. The required
     * margin is therefore capped at the skew.
     */
    @Test
    void testMinRemainingLifespanCappedAtRefreshTokenTimeSkew() throws Exception {
        // A 3s skew with the 10s default minimum: without the cap nothing would ever be served,
        // because a refresh cannot start while more than 3s remain.
        Tokens current = tokens("current", 2, Duration.ofSeconds(3), Duration.ofSeconds(10));
        assertFalse(current.isAccessTokenExpired());
        assertTrue(current.isAccessTokenWithinRefreshInterval());
        assertTrue(current.hasMinRemainingAccessTokenLifespan(),
                "the minimum should be capped at the skew rather than disabling reuse");

        BlockingOidcClient client = new BlockingOidcClient(tokens("refreshed", 300));
        TokensHelper helper = new TokensHelper();
        helper.initTokens(new ImmediateOidcClient(current), Map.of());

        helper.getTokens(client, Map.of(), false).subscribe().with(t -> {
        }, t -> {
        });
        client.awaitInFlight();

        Tokens served = helper.getTokens(client, Map.of(), false).await().atMost(Duration.ofSeconds(2));
        assertSame(current, served, "tokens should still be served when the skew is below the minimum");

        client.release.countDown();
    }

    /**
     * Reusing the tokens being refreshed is opt-in: when no minimum remaining lifespan is configured
     * the callers wait for the refresh, which is the behaviour of an OIDC client that has not enabled
     * this. Without it, upgrading would change how every existing deployment behaves.
     */
    @Test
    void testTokensNotReusedWhenMinRemainingLifespanNotConfigured() throws Exception {
        // Still valid and inside the skew, so the only reason not to serve it is the absent minimum.
        Tokens current = tokens("current", 8, TIME_SKEW, null);
        assertFalse(current.isAccessTokenExpired());
        assertTrue(current.isAccessTokenWithinRefreshInterval());
        assertFalse(current.hasMinRemainingAccessTokenLifespan());

        BlockingOidcClient client = new BlockingOidcClient(tokens("refreshed", 300));
        TokensHelper helper = new TokensHelper();
        helper.initTokens(new ImmediateOidcClient(current), Map.of());

        helper.getTokens(client, Map.of(), false).subscribe().with(t -> {
        }, t -> {
        });
        client.awaitInFlight();

        final AtomicReference<Tokens> served = new AtomicReference<>();
        final CountDownLatch completed = new CountDownLatch(1);
        helper.getTokens(client, Map.of(), false).subscribe().with(t -> {
            served.set(t);
            completed.countDown();
        }, t -> completed.countDown());

        assertFalse(completed.await(1, TimeUnit.SECONDS),
                "tokens must not be reused when no minimum remaining lifespan is configured");

        client.release.countDown();
        assertTrue(completed.await(10, TimeUnit.SECONDS), "the caller should have completed");
        assertEquals("refreshed", served.get().getAccessToken(),
                "the caller should have waited for the refreshed tokens");
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
