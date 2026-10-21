package io.quarkus.oidc.client.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.Tokens;
import io.quarkus.oidc.client.runtime.InMemoryOidcTestClient.Call;
import io.quarkus.oidc.client.runtime.InMemoryOidcTestClient.Expiry;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;

class TokensHelperTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Map<String, String> NO_PARAMS = Map.of();
    private static final Map<String, String> SCOPE_A = Map.of("scope", "a");
    private static final Map<String, String> SCOPE_B = Map.of("scope", "b");
    private static final Map<String, String> SCOPE_C = Map.of("scope", "c");
    private static final Map<String, String> SCOPE_D = Map.of("scope", "d");

    private final InMemoryOidcTestClient client = new InMemoryOidcTestClient();
    private TokensHelper helper = new TokensHelper();

    @Test
    void testFirstCallAcquiresTokensAndSecondCallIsCacheHit() {
        Tokens first = getTokens(SCOPE_A);
        Tokens second = getTokens(SCOPE_A);

        assertIssuedFor(first, SCOPE_A);
        assertThat(second.getAccessToken()).isEqualTo(first.getAccessToken());
        assertThat(client.requests()).hasSize(1);
        assertThat(client.requests().get(0).isGrant()).isTrue();
        assertThat(client.requests().get(0).params()).isEqualTo(SCOPE_A);
    }

    @Test
    void testInitTokensPrePopulatesCache() {
        helper.initTokens(client, SCOPE_A);
        assertThat(client.requests()).hasSize(1);

        Tokens tokens = getTokens(SCOPE_A);

        assertIssuedFor(tokens, SCOPE_A);
        assertThat(client.requests()).hasSize(1);
    }

    @Test
    void testInitTokensWithoutParametersPrePopulatesCacheForNoParameters() {
        helper.initTokens(client);
        assertThat(client.requests()).hasSize(1);

        Tokens tokens = helper.getTokens(client).await().atMost(TIMEOUT);

        assertIssuedFor(tokens, NO_PARAMS);
        assertThat(client.requests()).hasSize(1);
    }

    @Test
    void testExpiredAccessTokenIsRefreshedWithRefreshTokenAndParameters() {
        client.issue(Expiry.ACCESS_TOKEN_EXPIRED);
        Tokens expired = getTokens(SCOPE_A);
        client.issue(Expiry.VALID);

        Tokens refreshed = getTokens(SCOPE_A);

        assertIssuedFor(refreshed, SCOPE_A);
        assertThat(refreshed.getAccessToken()).isNotEqualTo(expired.getAccessToken());
        assertThat(client.requests()).hasSize(2);
        Call refresh = client.requests().get(1);
        assertThat(refresh.isRefresh()).isTrue();
        assertThat(refresh.refreshToken()).isEqualTo(expired.getRefreshToken());
        assertThat(refresh.params()).isEqualTo(SCOPE_A);

        Tokens cached = getTokens(SCOPE_A);
        assertThat(cached.getAccessToken()).isEqualTo(refreshed.getAccessToken());
        assertThat(client.requests()).hasSize(2);
    }

    @Test
    void testExpiredAccessTokenWithoutRefreshTokenLeadsToNewGrant() {
        client.issue(Expiry.ACCESS_TOKEN_EXPIRED_NO_REFRESH_TOKEN);
        Tokens expired = getTokens(SCOPE_A);
        client.issue(Expiry.VALID);

        Tokens renewed = getTokens(SCOPE_A);

        assertIssuedFor(renewed, SCOPE_A);
        assertThat(renewed.getAccessToken()).isNotEqualTo(expired.getAccessToken());
        assertThat(client.requests()).hasSize(2);
        assertThat(client.requests().get(1).isGrant()).isTrue();
        assertThat(client.requests().get(1).params()).isEqualTo(SCOPE_A);
    }

    @Test
    void testExpiredAccessTokenWithExpiredRefreshTokenLeadsToNewGrant() {
        client.issue(Expiry.ACCESS_AND_REFRESH_TOKENS_EXPIRED);
        Tokens expired = getTokens(SCOPE_A);
        client.issue(Expiry.VALID);

        Tokens renewed = getTokens(SCOPE_A);

        assertIssuedFor(renewed, SCOPE_A);
        assertThat(renewed.getAccessToken()).isNotEqualTo(expired.getAccessToken());
        assertThat(client.requests()).hasSize(2);
        assertThat(client.requests().get(1).isGrant()).isTrue();
        assertThat(client.requests().get(1).params()).isEqualTo(SCOPE_A);
    }

    @Test
    void testAccessTokenWithinRefreshIntervalIsRefreshedProactively() {
        client.issue(Expiry.ACCESS_TOKEN_WITHIN_REFRESH_INTERVAL);
        Tokens aboutToExpire = getTokens(SCOPE_A);
        client.issue(Expiry.VALID);

        Tokens refreshed = getTokens(SCOPE_A);

        assertIssuedFor(refreshed, SCOPE_A);
        assertThat(refreshed.getAccessToken()).isNotEqualTo(aboutToExpire.getAccessToken());
        assertThat(client.requests()).hasSize(2);
        assertThat(client.requests().get(1).isRefresh()).isTrue();
        assertThat(client.requests().get(1).refreshToken()).isEqualTo(aboutToExpire.getRefreshToken());
    }

    @Test
    void testForceNewTokensDiscardsValidCachedTokens() {
        Tokens first = getTokens(SCOPE_A);

        Tokens forced = helper.getTokens(client, SCOPE_A, true).await().atMost(TIMEOUT);

        assertIssuedFor(forced, SCOPE_A);
        assertThat(forced.getAccessToken()).isNotEqualTo(first.getAccessToken());
        assertThat(client.requests()).hasSize(2);
        assertThat(client.requests().get(1).isGrant()).isTrue();
        assertThat(client.requests().get(1).params()).isEqualTo(SCOPE_A);

        Tokens cached = getTokens(SCOPE_A);
        assertThat(cached.getAccessToken()).isEqualTo(forced.getAccessToken());
        assertThat(client.requests()).hasSize(2);
    }

    @Test
    void testCallsWithSameParametersJoinInFlightRequest() throws Exception {
        client.respondLater();
        CompletableFuture<Tokens> first = subscribe(helper.getTokens(client, SCOPE_A, false));
        CompletableFuture<Tokens> second = subscribe(helper.getTokens(client, SCOPE_A, false));
        assertThat(client.requests()).hasSize(1);

        client.completePendingRequests();

        assertIssuedFor(await(first), SCOPE_A);
        assertThat(await(second).getAccessToken()).isEqualTo(await(first).getAccessToken());
        assertThat(client.requests()).hasSize(1);
    }

    @Test
    void testReturnedUniIsMemoized() throws Exception {
        client.respondLater();
        Uni<Tokens> uni = helper.getTokens(client, SCOPE_A, false);
        CompletableFuture<Tokens> first = subscribe(uni);
        CompletableFuture<Tokens> second = subscribe(uni);
        assertThat(client.requests()).hasSize(1);

        client.completePendingRequests();

        assertThat(await(first).getAccessToken()).isEqualTo(await(second).getAccessToken());
        assertThat(client.requests()).hasSize(1);
    }

    @Test
    void testFailedRequestIsPropagatedAndNextCallRequestsTokensAgain() {
        client.failRequests(() -> new IllegalStateException("token endpoint unavailable"));
        assertThatThrownBy(() -> getTokens(SCOPE_A))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("token endpoint unavailable");
        client.succeedRequests();

        Tokens tokens = getTokens(SCOPE_A);

        assertIssuedFor(tokens, SCOPE_A);
        assertThat(client.requests()).hasSize(2);
        assertThat(client.requests().get(1).params()).isEqualTo(SCOPE_A);
    }

    @Test
    void testFailedInFlightRequestFailsAllWaitersAndNextCallRequestsTokensAgain() throws Exception {
        client.respondLater();
        CompletableFuture<Tokens> first = subscribe(helper.getTokens(client, SCOPE_A, false));
        CompletableFuture<Tokens> second = subscribe(helper.getTokens(client, SCOPE_A, false));
        assertThat(client.requests()).hasSize(1);

        client.failPendingRequests(new IllegalStateException("token endpoint unavailable"));

        assertThatThrownBy(() -> await(first)).hasCauseInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> await(second)).hasCauseInstanceOf(IllegalStateException.class);
        client.respondImmediately();
        assertIssuedFor(getTokens(SCOPE_A), SCOPE_A);
        assertThat(client.requests()).hasSize(2);
    }

    @Test
    void testConcurrentCallsWithSameParametersMakeSingleRequest() throws Exception {
        client.respondLater();

        List<Tokens> results = getTokensConcurrently(Collections.nCopies(50, SCOPE_A));

        assertThat(client.requests()).hasSize(1);
        for (Tokens tokens : results) {
            assertIssuedFor(tokens, SCOPE_A);
            assertThat(tokens.getAccessToken()).isEqualTo(results.get(0).getAccessToken());
        }
    }

    @Test
    void testCallWithDifferentParametersDoesNotReturnCachedTokens() {
        Tokens forA = getTokens(SCOPE_A);

        Tokens forB = getTokens(SCOPE_B);

        assertIssuedFor(forA, SCOPE_A);
        assertIssuedFor(forB, SCOPE_B);
        assertThat(client.requests()).hasSize(2);
        assertThat(client.requests().get(1).isGrant()).isTrue();
        assertThat(client.requests().get(1).params()).isEqualTo(SCOPE_B);
    }

    @Test
    void testTokensAreCachedPerParameters() {
        Tokens forA = getTokens(SCOPE_A);
        Tokens forB = getTokens(SCOPE_B);
        assertIssuedFor(forA, SCOPE_A);
        assertIssuedFor(forB, SCOPE_B);
        assertThat(client.requests()).hasSize(2);

        Tokens forAAgain = getTokens(SCOPE_A);
        Tokens forBAgain = getTokens(SCOPE_B);

        assertThat(forAAgain.getAccessToken()).isEqualTo(forA.getAccessToken());
        assertThat(forBAgain.getAccessToken()).isEqualTo(forB.getAccessToken());
        assertThat(client.requests()).hasSize(2);
    }

    @Test
    void testCallWithDifferentParametersDoesNotJoinInFlightRequest() throws Exception {
        client.respondLater();
        CompletableFuture<Tokens> forA = subscribe(helper.getTokens(client, SCOPE_A, false));
        CompletableFuture<Tokens> forB = subscribe(helper.getTokens(client, SCOPE_B, false));

        client.completePendingRequests();

        assertIssuedFor(await(forA), SCOPE_A);
        assertIssuedFor(await(forB), SCOPE_B);
        assertThat(client.requests()).extracting(Call::params).containsExactly(SCOPE_A, SCOPE_B);
    }

    @Test
    void testForceNewTokensDoesNotJoinInFlightRequestWithDifferentParameters() throws Exception {
        client.respondLater();
        CompletableFuture<Tokens> forA = subscribe(helper.getTokens(client, SCOPE_A, true));
        CompletableFuture<Tokens> forB = subscribe(helper.getTokens(client, SCOPE_B, true));

        client.completePendingRequests();

        assertIssuedFor(await(forA), SCOPE_A);
        assertIssuedFor(await(forB), SCOPE_B);
        assertThat(client.requests()).extracting(Call::params).containsExactly(SCOPE_A, SCOPE_B);
    }

    @Test
    void testExpiredTokensAreRefreshedWithTheirOwnRefreshTokenAndParameters() {
        client.issue(Expiry.ACCESS_TOKEN_EXPIRED);
        Tokens expiredForA = getTokens(SCOPE_A);
        client.issue(Expiry.VALID);
        Tokens forB = getTokens(SCOPE_B);
        assertIssuedFor(forB, SCOPE_B);
        assertThat(client.requests()).hasSize(2);

        Tokens refreshedForA = getTokens(SCOPE_A);

        assertIssuedFor(refreshedForA, SCOPE_A);
        assertThat(refreshedForA.getAccessToken()).isNotEqualTo(expiredForA.getAccessToken());
        assertThat(client.requests()).hasSize(3);
        Call refresh = client.requests().get(2);
        assertThat(refresh.isRefresh()).isTrue();
        assertThat(refresh.refreshToken()).isEqualTo(expiredForA.getRefreshToken());
        assertThat(refresh.params()).isEqualTo(SCOPE_A);

        Tokens forBAgain = getTokens(SCOPE_B);
        assertThat(forBAgain.getAccessToken()).isEqualTo(forB.getAccessToken());
        assertThat(client.requests()).hasSize(3);
    }

    @Test
    void testExpiredTokensAreNotRefreshedForDifferentParameters() {
        client.issue(Expiry.ACCESS_TOKEN_EXPIRED);
        Tokens expiredForA = getTokens(SCOPE_A);
        client.issue(Expiry.VALID);

        Tokens forB = getTokens(SCOPE_B);

        assertIssuedFor(forB, SCOPE_B);
        assertThat(client.requests()).hasSize(2);
        Call request = client.requests().get(1);
        assertThat(request.isGrant())
                .as("expected a new grant request for %s but got %s with the refresh token issued for %s",
                        SCOPE_B, request, SCOPE_A)
                .isTrue();
        assertThat(request.params()).isEqualTo(SCOPE_B);
        assertThat(client.calls()).extracting(Call::refreshToken).doesNotContain(expiredForA.getRefreshToken());
    }

    @Test
    void testFailedRequestForDifferentParametersLeavesCachedTokensIntact() {
        Tokens forA = getTokens(SCOPE_A);
        client.failRequests(() -> new IllegalStateException("token endpoint unavailable"));

        assertThatThrownBy(() -> getTokens(SCOPE_B),
                "expected the request for %s to fail instead of returning the tokens cached for %s", SCOPE_B, SCOPE_A)
                .isInstanceOf(IllegalStateException.class);

        client.succeedRequests();
        Tokens forAAgain = getTokens(SCOPE_A);
        assertThat(forAAgain.getAccessToken()).isEqualTo(forA.getAccessToken());
        assertThat(client.requests()).hasSize(2);
    }

    @Test
    void testForceNewTokensCarriesParameters() {
        Tokens forA = helper.getTokens(client, SCOPE_A, true).await().atMost(TIMEOUT);
        Tokens forB = helper.getTokens(client, SCOPE_B, true).await().atMost(TIMEOUT);
        Tokens forAAgain = helper.getTokens(client, SCOPE_A, true).await().atMost(TIMEOUT);

        assertIssuedFor(forA, SCOPE_A);
        assertIssuedFor(forB, SCOPE_B);
        assertIssuedFor(forAAgain, SCOPE_A);
        assertThat(forAAgain.getAccessToken()).isNotEqualTo(forA.getAccessToken());
        assertThat(client.requests()).extracting(Call::params).containsExactly(SCOPE_A, SCOPE_B, SCOPE_A);
        assertThat(client.requests()).allMatch(Call::isGrant);
    }

    @Test
    void testParametersWithEqualContentShareCachedTokens() {
        Map<String, String> immutable = Map.of("scope", "a", "audience", "x");
        Map<String, String> reversed = new LinkedHashMap<>();
        reversed.put("audience", "x");
        reversed.put("scope", "a");
        Map<String, String> sorted = new TreeMap<>(immutable);

        Tokens first = getTokens(immutable);
        Tokens second = getTokens(reversed);
        Tokens third = getTokens(sorted);

        assertIssuedFor(first, immutable);
        assertThat(second.getAccessToken()).isEqualTo(first.getAccessToken());
        assertThat(third.getAccessToken()).isEqualTo(first.getAccessToken());
        assertThat(client.requests()).hasSize(1);
    }

    @Test
    void testEmptyParameterVariantsShareCachedTokens() {
        Tokens first = helper.getTokens(client).await().atMost(TIMEOUT);
        Tokens second = getTokens(Map.of());
        Tokens third = getTokens(Collections.emptyMap());
        Tokens fourth = getTokens(new HashMap<>());

        assertIssuedFor(first, NO_PARAMS);
        assertThat(second.getAccessToken()).isEqualTo(first.getAccessToken());
        assertThat(third.getAccessToken()).isEqualTo(first.getAccessToken());
        assertThat(fourth.getAccessToken()).isEqualTo(first.getAccessToken());
        assertThat(client.requests()).hasSize(1);
    }

    @Test
    void testDifferentValueOrAdditionalParameterIsDifferentCacheEntry() {
        Map<String, String> scopeAWithAudience = Map.of("scope", "a", "audience", "x");

        Tokens forA = getTokens(SCOPE_A);
        Tokens forB = getTokens(SCOPE_B);
        Tokens forAWithAudience = getTokens(scopeAWithAudience);

        assertIssuedFor(forA, SCOPE_A);
        assertIssuedFor(forB, SCOPE_B);
        assertIssuedFor(forAWithAudience, scopeAWithAudience);
        assertThat(client.requests()).extracting(Call::params).containsExactly(SCOPE_A, SCOPE_B, scopeAWithAudience);
    }

    @Test
    void testCacheIsNotAffectedByCallerMutatingParametersAfterCall() {
        Map<String, String> mutable = new HashMap<>(SCOPE_A);
        Tokens forA = getTokens(mutable);
        mutable.put("scope", "b");

        Tokens forAAgain = getTokens(SCOPE_A);
        assertThat(forAAgain.getAccessToken()).isEqualTo(forA.getAccessToken());
        assertThat(client.requests()).hasSize(1);

        Tokens forB = getTokens(SCOPE_B);
        assertIssuedFor(forB, SCOPE_B);
        assertThat(client.requests()).hasSize(2);
        assertThat(client.requests().get(1).params()).isEqualTo(SCOPE_B);
    }

    @Test
    void testInitTokensWithParametersDoesNotServeCallWithDifferentParameters() {
        helper.initTokens(client, SCOPE_A);

        Tokens forB = getTokens(SCOPE_B);

        assertIssuedFor(forB, SCOPE_B);
        assertThat(client.requests()).hasSize(2);
        assertThat(client.requests().get(1).isGrant()).isTrue();
        assertThat(client.requests().get(1).params()).isEqualTo(SCOPE_B);
    }

    @Test
    void testConcurrentCallsWithDifferentParametersMakeOneRequestPerParameters() throws Exception {
        client.respondLater();
        List<Map<String, String>> paramsPerThread = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            paramsPerThread.add(SCOPE_A);
            paramsPerThread.add(SCOPE_B);
        }

        List<Tokens> results = getTokensConcurrently(paramsPerThread);

        for (int i = 0; i < paramsPerThread.size(); i++) {
            assertIssuedFor(results.get(i), paramsPerThread.get(i));
        }
        assertThat(client.requests()).extracting(Call::params).containsExactlyInAnyOrder(SCOPE_A, SCOPE_B);
    }

    @Test
    void testManyDifferentParametersGetTheirOwnTokens() {
        for (int i = 0; i < 100; i++) {
            Map<String, String> params = Map.of("subject_token", "subject-" + i);
            assertIssuedFor(getTokens(params), params);
        }
        assertThat(client.requests()).hasSize(100);
    }

    @Test
    void testTokensWithoutParametersAreNeverEvicted() {
        helper = new TokensHelper(2);
        Tokens noParams = getTokens(NO_PARAMS);
        getTokens(SCOPE_A);
        getTokens(SCOPE_B);
        assertThat(client.requests()).hasSize(3);

        // the cache is full and the tokens without parameters are the least recently used ones,
        // still, the tokens for SCOPE_A are evicted instead
        assertIssuedFor(getTokens(SCOPE_C), SCOPE_C);
        assertThat(client.requests()).hasSize(4);
        assertThat(getTokens(NO_PARAMS).getAccessToken()).isEqualTo(noParams.getAccessToken());
        getTokens(SCOPE_B);
        getTokens(SCOPE_C);
        assertThat(client.requests()).hasSize(4);
        assertIssuedFor(getTokens(SCOPE_A), SCOPE_A);
        assertThat(client.requests()).hasSize(5);
    }

    @Test
    void testTokensWithoutParametersDoNotCountTowardsCacheSize() {
        helper = new TokensHelper(2);
        getTokens(NO_PARAMS);
        getTokens(SCOPE_A);
        getTokens(SCOPE_B);
        assertThat(client.requests()).hasSize(3);

        // two sets of parameters fit into the cache next to the tokens without parameters, nothing is evicted
        getTokens(NO_PARAMS);
        getTokens(SCOPE_A);
        getTokens(SCOPE_B);
        assertThat(client.requests()).hasSize(3);
    }

    @Test
    void testFailedRequestWithoutParametersIsRepeatedByNextCall() {
        client.failRequests(() -> new IllegalStateException("token endpoint unavailable"));
        assertThatThrownBy(() -> getTokens(NO_PARAMS)).isInstanceOf(IllegalStateException.class);
        client.succeedRequests();

        assertIssuedFor(getTokens(NO_PARAMS), NO_PARAMS);
        assertThat(client.requests()).hasSize(2);
    }

    @Test
    void testTokenCacheMaxSizeMustBePositive() {
        assertThatThrownBy(() -> new TokensHelper(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testLeastRecentlyUsedTokensAreEvictedWhenCacheIsFull() {
        helper = new TokensHelper(3);
        getTokens(SCOPE_A);
        getTokens(SCOPE_B);
        getTokens(SCOPE_C);
        assertThat(client.requests()).hasSize(3);

        // cache hits never evict
        getTokens(SCOPE_A);
        getTokens(SCOPE_B);
        getTokens(SCOPE_C);
        assertThat(client.requests()).hasSize(3);

        // the tokens for SCOPE_B are the least recently used now
        getTokens(SCOPE_A);
        assertIssuedFor(getTokens(SCOPE_D), SCOPE_D);
        assertThat(client.requests()).hasSize(4);

        getTokens(SCOPE_A);
        getTokens(SCOPE_C);
        getTokens(SCOPE_D);
        assertThat(client.requests()).hasSize(4);
        assertIssuedFor(getTokens(SCOPE_B), SCOPE_B);
        assertThat(client.requests()).hasSize(5);
        assertThat(client.requests().get(4).isGrant()).isTrue();
    }

    @Test
    void testCacheIsBoundedByDefaultSize() {
        List<Map<String, String>> params = parameterSets(TokensHelper.DEFAULT_TOKEN_CACHE_MAX_SIZE);
        for (Map<String, String> p : params) {
            assertIssuedFor(getTokens(p), p);
        }
        for (Map<String, String> p : params) {
            getTokens(p);
        }
        assertThat(client.requests()).hasSize(params.size());

        Map<String, String> oneMore = Map.of("scope", "one-more");
        assertIssuedFor(getTokens(oneMore), oneMore);
        assertThat(client.requests()).hasSize(params.size() + 1);

        // the least recently used tokens, the ones of the first set of parameters, have been evicted
        for (int i = 1; i < params.size(); i++) {
            getTokens(params.get(i));
        }
        getTokens(oneMore);
        assertThat(client.requests()).hasSize(params.size() + 1);
        assertIssuedFor(getTokens(params.get(0)), params.get(0));
        assertThat(client.requests()).hasSize(params.size() + 2);
    }

    @Test
    void testExpiredTokensAreEvictedBeforeLeastRecentlyUsedTokens() {
        helper = new TokensHelper(3);
        getTokens(SCOPE_A);
        getTokens(SCOPE_B);
        client.issue(Expiry.ACCESS_TOKEN_EXPIRED);
        // the most recently used tokens, but their access token has expired
        Tokens expiredForC = getTokens(SCOPE_C);
        client.issue(Expiry.VALID);
        assertThat(client.requests()).hasSize(3);

        assertIssuedFor(getTokens(SCOPE_D), SCOPE_D);
        assertThat(client.requests()).hasSize(4);

        getTokens(SCOPE_A);
        getTokens(SCOPE_B);
        getTokens(SCOPE_D);
        assertThat(client.requests()).hasSize(4);
        // the expired tokens were evicted together with their refresh token, new tokens are acquired with a grant
        Tokens forC = getTokens(SCOPE_C);
        assertIssuedFor(forC, SCOPE_C);
        assertThat(forC.getAccessToken()).isNotEqualTo(expiredForC.getAccessToken());
        assertThat(client.requests()).hasSize(5);
        assertThat(client.requests().get(4).isGrant()).isTrue();
        assertThat(client.calls()).extracting(Call::refreshToken).doesNotContain(expiredForC.getRefreshToken());
    }

    @Test
    void testFailedRequestDoesNotOccupyCache() {
        helper = new TokensHelper(3);
        getTokens(SCOPE_A);
        getTokens(SCOPE_B);
        Map<String, String> failing = Map.of("scope", "failing");
        client.failRequests(() -> new IllegalStateException("token endpoint unavailable"));
        assertThatThrownBy(() -> getTokens(failing)).isInstanceOf(IllegalStateException.class);
        client.succeedRequests();

        // there is room for one more set of tokens, nothing is evicted
        assertIssuedFor(getTokens(SCOPE_C), SCOPE_C);
        int requests = client.requests().size();
        getTokens(SCOPE_A);
        getTokens(SCOPE_B);
        getTokens(SCOPE_C);
        assertThat(client.requests()).hasSize(requests);
    }

    @Test
    void testRequestInProgressIsNotEvictedBeforeLeastRecentlyUsedTokens() throws Exception {
        helper = new TokensHelper(3);
        getTokens(SCOPE_A);
        getTokens(SCOPE_B);
        client.respondLater();
        CompletableFuture<Tokens> inProgress = subscribe(helper.getTokens(client, SCOPE_C, false));
        CompletableFuture<Tokens> oneMore = subscribe(helper.getTokens(client, SCOPE_D, false));
        client.completePendingRequests();
        assertIssuedFor(await(inProgress), SCOPE_C);
        assertIssuedFor(await(oneMore), SCOPE_D);
        client.respondImmediately();

        int requests = client.requests().size();
        getTokens(SCOPE_B);
        getTokens(SCOPE_C);
        getTokens(SCOPE_D);
        assertThat(client.requests()).hasSize(requests);
        assertIssuedFor(getTokens(SCOPE_A), SCOPE_A);
        assertThat(client.requests()).hasSize(requests + 1);
    }

    @Test
    void testEvictedRequestInProgressStillDeliversItsTokens() throws Exception {
        helper = new TokensHelper(2);
        client.respondLater();
        CompletableFuture<Tokens> forA = subscribe(helper.getTokens(client, SCOPE_A, false));
        CompletableFuture<Tokens> forB = subscribe(helper.getTokens(client, SCOPE_B, false));
        // the least recently used request in progress, the one for SCOPE_A, is evicted
        CompletableFuture<Tokens> forC = subscribe(helper.getTokens(client, SCOPE_C, false));
        assertThat(client.requests()).hasSize(3);

        client.completePendingRequests();
        assertIssuedFor(await(forA), SCOPE_A);
        assertIssuedFor(await(forB), SCOPE_B);
        assertIssuedFor(await(forC), SCOPE_C);

        client.respondImmediately();
        getTokens(SCOPE_B);
        getTokens(SCOPE_C);
        assertThat(client.requests()).hasSize(3);
        assertIssuedFor(getTokens(SCOPE_A), SCOPE_A);
        assertThat(client.requests()).hasSize(4);
    }

    @Test
    void testBurstOfConcurrentMissesEvictsOnlyLeastRecentlyUsedTokens() throws Exception {
        helper = new TokensHelper(5);
        Map<String, String> anchor = Map.of("scope", "anchor");
        getTokens(anchor);
        List<Map<String, String>> previousBurst = parameterSets(4);
        for (Map<String, String> p : previousBurst) {
            getTokens(p);
        }
        for (int round = 0; round < 50; round++) {
            // the anchor tokens become the most recently used ones and must survive the burst of misses which
            // must evict exactly the tokens of the previous burst, the least recently used ones
            int requests = client.requests().size();
            getTokens(anchor);
            assertThat(client.requests()).as("round %d: the anchor tokens must still be cached", round)
                    .hasSize(requests);
            List<Map<String, String>> burst = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                burst.add(Map.of("scope", "burst-" + round + "-" + i));
            }
            client.respondLater();

            List<Tokens> results = getTokensConcurrently(burst);

            client.respondImmediately();
            for (int i = 0; i < burst.size(); i++) {
                assertIssuedFor(results.get(i), burst.get(i));
            }
            assertThat(client.requests()).hasSize(requests + 4);
            previousBurst = burst;
        }

        int requests = client.requests().size();
        getTokens(anchor);
        for (Map<String, String> p : previousBurst) {
            getTokens(p);
        }
        assertThat(client.requests()).hasSize(requests);
        Map<String, String> evicted = Map.of("scope", "burst-0-0");
        assertIssuedFor(getTokens(evicted), evicted);
        assertThat(client.requests()).hasSize(requests + 1);
        assertThat(client.requests().get(requests).isGrant()).isTrue();
    }

    @Test
    void testConcurrentCallsWithMoreParametersThanCacheSizeGetTheirOwnTokens() throws Exception {
        helper = new TokensHelper(3);

        List<Tokens> results = getTokensConcurrently(subjectTokensPerThread(100, 10));

        for (int i = 0; i < results.size(); i++) {
            assertIssuedFor(results.get(i), subjectTokensPerThread(100, 10).get(i));
        }
    }

    @Test
    void testConcurrentCallsWithMoreRequestsInProgressThanCacheSizeGetTheirOwnTokens() throws Exception {
        helper = new TokensHelper(3);
        client.respondLater();

        List<Tokens> results = getTokensConcurrently(subjectTokensPerThread(100, 10));

        for (int i = 0; i < results.size(); i++) {
            assertIssuedFor(results.get(i), subjectTokensPerThread(100, 10).get(i));
        }
    }

    private static List<Map<String, String>> parameterSets(int count) {
        List<Map<String, String>> params = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            params.add(Map.of("scope", "scope-" + i));
        }
        return params;
    }

    private static List<Map<String, String>> subjectTokensPerThread(int threads, int subjectTokens) {
        List<Map<String, String>> params = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            params.add(Map.of("subject_token", "subject-" + (i % subjectTokens)));
        }
        return params;
    }

    private Tokens getTokens(Map<String, String> params) {
        return helper.getTokens(client, params, false).await().atMost(TIMEOUT);
    }

    private static CompletableFuture<Tokens> subscribe(Uni<Tokens> uni) {
        return uni.subscribe().asCompletionStage();
    }

    private static Tokens await(CompletableFuture<Tokens> future)
            throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
    }

    private List<Tokens> getTokensConcurrently(List<Map<String, String>> paramsPerThread) throws Exception {
        try (ExecutorService executor = Executors.newFixedThreadPool(paramsPerThread.size())) {
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch requested = new CountDownLatch(paramsPerThread.size());
            List<Future<Tokens>> futures = new ArrayList<>();
            for (Map<String, String> params : paramsPerThread) {
                futures.add(executor.submit(new Callable<Tokens>() {
                    @Override
                    public Tokens call() throws Exception {
                        start.await();
                        CompletableFuture<Tokens> result = subscribe(helper.getTokens(client, params, false));
                        requested.countDown();
                        return await(result);
                    }
                }));
            }
            start.countDown();
            assertThat(requested.await(TIMEOUT.toSeconds(), TimeUnit.SECONDS)).isTrue();
            client.completePendingRequests();
            List<Tokens> results = new ArrayList<>();
            for (Future<Tokens> future : futures) {
                results.add(future.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS));
            }
            return results;
        }
    }

    private static void assertIssuedFor(Tokens tokens, Map<String, String> params) {
        assertThat(tokens.getAccessToken())
                .as("access token must have been issued for the parameters %s", params)
                .startsWith(InMemoryOidcTestClient.accessTokenPrefix(params));
    }

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
        // A 6s skew means a refresh starts once fewer than 6s of life remain, so 5s remaining
        // both triggers the refresh and leaves the tokens usable while it runs. The expiry is
        // tracked in whole seconds, so the 1s minimum leaves 4s for the test to run before the
        // tokens stop being served.
        Tokens current = tokens("current", 5, Duration.ofSeconds(6), Duration.ofSeconds(1));
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
     * Once a caller has forced the acquisition of new tokens while a refresh is in flight, the tokens
     * being refreshed are known to have been rejected, so they must not be served to anyone else
     * either: every caller arriving afterwards waits for the refresh too.
     */
    @Test
    void testTokensBeingRefreshedNotServedAfterForcedAcquisition() throws Exception {
        Tokens current = tokens("current", 8);
        BlockingOidcClient client = new BlockingOidcClient(tokens("refreshed", 300));
        TokensHelper helper = new TokensHelper();
        helper.initTokens(new ImmediateOidcClient(current), Map.of());

        helper.getTokens(client, Map.of(), false).subscribe().with(t -> {
        }, t -> {
        });
        client.awaitInFlight();

        // Before the forcing, the tokens being refreshed are still served, so what follows is down to it.
        assertThat(helper.getTokens(client, Map.of(), false).await().atMost(Duration.ofSeconds(2))).isSameAs(current);

        final CountDownLatch forcedCompleted = new CountDownLatch(1);
        helper.getTokens(client, Map.of(), true).subscribe().with(t -> forcedCompleted.countDown(),
                t -> forcedCompleted.countDown());

        final AtomicReference<Tokens> served = new AtomicReference<>();
        final CountDownLatch completed = new CountDownLatch(1);
        helper.getTokens(client, Map.of(), false).subscribe().with(t -> {
            served.set(t);
            completed.countDown();
        }, t -> completed.countDown());

        assertThat(completed.await(1, TimeUnit.SECONDS))
                .as("rejected tokens must not be served to callers arriving after the forced acquisition")
                .isFalse();

        client.release.countDown();
        assertThat(forcedCompleted.await(10, TimeUnit.SECONDS)).as("the forcing caller should have completed").isTrue();
        assertThat(completed.await(10, TimeUnit.SECONDS)).as("the caller should have completed").isTrue();
        assertThat(served.get().getAccessToken()).isEqualTo("refreshed");
        assertThat(client.acquisitions.get())
                .as("the forced acquisition should have joined the refresh in flight rather than start another")
                .isEqualTo(1);
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
     * A refresh only starts once the remaining lifespan has dropped below the refresh token time skew, so a
     * minimum which is not less than the skew can never be satisfied. That combination is rejected when the
     * OIDC client is created, rather than adjusted here, so a {@link Tokens} built directly with it simply
     * never reuses the token being refreshed instead of silently applying a different minimum.
     */
    @Test
    void testTokensNotReusedWhenMinRemainingLifespanNotLessThanSkew() throws Exception {
        // A 3s skew with a 10s minimum: a refresh cannot start while more than 3s remain, so the minimum
        // is unreachable.
        Tokens current = tokens("current", 2, Duration.ofSeconds(3), Duration.ofSeconds(10));
        assertFalse(current.isAccessTokenExpired());
        assertTrue(current.isAccessTokenWithinRefreshInterval());
        assertFalse(current.hasMinRemainingAccessTokenLifespan(),
                "a minimum which is not less than the skew can never be satisfied");

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
                "tokens must not be reused when the minimum can never be satisfied");

        client.release.countDown();
        assertTrue(completed.await(10, TimeUnit.SECONDS), "the caller should have completed");
        assertEquals("refreshed", served.get().getAccessToken(),
                "the caller should have waited for the refreshed tokens");
    }

    /**
     * The deprecated constructor predates the minimum remaining lifespan, so tokens created with it
     * must keep the previous behaviour and never be reused while they are being refreshed.
     */
    @Test
    @SuppressWarnings("removal")
    void testDeprecatedConstructorDoesNotConfigureMinRemainingLifespan() {
        final long nowSecs = System.currentTimeMillis() / 1000;
        Tokens tokens = new Tokens("current", nowSecs + 8, TIME_SKEW, "refresh", nowSecs + 300, new JsonObject(),
                "client");
        assertThat(tokens.isAccessTokenExpired()).isFalse();
        assertThat(tokens.isAccessTokenWithinRefreshInterval()).isTrue();
        assertThat(tokens.hasMinRemainingAccessTokenLifespan()).isFalse();
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
     * Tokens whose expiry the provider did not report cannot be shown to have the required lifespan left,
     * so they are not reused while a refresh is in flight.
     */
    @Test
    void testTokensWithUnknownExpiryAreNotReused() {
        Tokens noExpiry = new Tokens("current", null, TIME_SKEW, null, null, new JsonObject(), "client",
                MIN_REMAINING_LIFESPAN);
        assertFalse(noExpiry.hasMinRemainingAccessTokenLifespan(),
                "tokens with an unknown expiry must not be reused while they are being refreshed");
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
