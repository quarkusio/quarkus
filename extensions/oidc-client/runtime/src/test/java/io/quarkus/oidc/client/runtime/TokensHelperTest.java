package io.quarkus.oidc.client.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.client.Tokens;
import io.quarkus.oidc.client.runtime.InMemoryOidcTestClient.Call;
import io.quarkus.oidc.client.runtime.InMemoryOidcTestClient.Expiry;
import io.smallrye.mutiny.Uni;

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
    void testTokenCacheMaxSizeMustNotBeNegative() {
        assertThatThrownBy(() -> new TokensHelper(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testTokensWithParametersAreNotCachedWhenCacheIsDisabled() {
        helper = new TokensHelper(0);
        assertIssuedFor(getTokens(SCOPE_A), SCOPE_A);
        assertIssuedFor(getTokens(SCOPE_A), SCOPE_A);
        assertThat(client.requests()).hasSize(2);
        assertThat(client.requests()).allMatch(InMemoryOidcTestClient.Call::isGrant);
    }

    @Test
    void testTokensWithoutParametersAreCachedWhenCacheIsDisabled() {
        helper = new TokensHelper(0);
        Tokens first = getTokens(NO_PARAMS);
        Tokens second = getTokens(NO_PARAMS);
        assertThat(second.getAccessToken()).isEqualTo(first.getAccessToken());
        assertThat(client.requests()).hasSize(1);
    }

    @Test
    void testInitTokensWithParametersDoesNotPrePopulateCacheWhenCacheIsDisabled() {
        helper = new TokensHelper(0);
        helper.initTokens(client, SCOPE_A);
        assertIssuedFor(getTokens(SCOPE_A), SCOPE_A);
        assertThat(client.requests()).hasSize(2);
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
}
