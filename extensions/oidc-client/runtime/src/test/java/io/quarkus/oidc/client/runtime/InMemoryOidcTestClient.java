package io.quarkus.oidc.client.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.Tokens;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;

final class InMemoryOidcTestClient implements OidcClient {

    static final String CLIENT_ID = "test-client";

    enum Expiry {
        VALID,
        ACCESS_TOKEN_EXPIRED,
        ACCESS_TOKEN_WITHIN_REFRESH_INTERVAL,
        ACCESS_AND_REFRESH_TOKENS_EXPIRED,
        ACCESS_TOKEN_EXPIRED_NO_REFRESH_TOKEN
    }

    record Call(Operation operation, String refreshToken, Map<String, String> params) {

        enum Operation {
            GRANT,
            REFRESH
        }

        boolean isGrant() {
            return operation == Operation.GRANT;
        }

        boolean isRefresh() {
            return operation == Operation.REFRESH;
        }
    }

    static final class PendingRequest {
        final Call call;
        final CompletableFuture<Tokens> future = new CompletableFuture<>();

        PendingRequest(Call call) {
            this.call = call;
        }
    }

    private final List<Call> calls = new CopyOnWriteArrayList<>();
    private final List<Call> requests = new CopyOnWriteArrayList<>();
    private final List<PendingRequest> pendingRequests = new CopyOnWriteArrayList<>();
    private final Map<String, Map<String, String>> issuedRefreshTokens = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger();
    private volatile boolean respondLater;
    private volatile Expiry expiry = Expiry.VALID;
    private volatile Supplier<Throwable> failure;

    @Override
    public Uni<Tokens> getTokens(Map<String, String> additionalGrantParameters) {
        return record(new Call(Call.Operation.GRANT, null, Map.copyOf(additionalGrantParameters)));
    }

    @Override
    public Uni<Tokens> refreshTokens(String refreshToken, Map<String, String> additionalGrantParameters) {
        return record(new Call(Call.Operation.REFRESH, refreshToken, Map.copyOf(additionalGrantParameters)));
    }

    @Override
    public Uni<Boolean> revokeAccessToken(String accessToken, Map<String, String> additionalParameters) {
        return Uni.createFrom().item(true);
    }

    @Override
    public void close() {
    }

    void issue(Expiry expiry) {
        this.expiry = expiry;
    }

    void respondLater() {
        this.respondLater = true;
    }

    void respondImmediately() {
        this.respondLater = false;
    }

    void failRequests(Supplier<Throwable> failure) {
        this.failure = failure;
    }

    void succeedRequests() {
        this.failure = null;
    }

    void completePendingRequests() {
        completePendingRequests(call -> true);
    }

    void completePendingRequests(Predicate<Call> callFilter) {
        for (PendingRequest pendingRequest : pendingRequests) {
            if (!pendingRequest.future.isDone() && callFilter.test(pendingRequest.call)) {
                pendingRequest.future.complete(issue(pendingRequest.call));
            }
        }
    }

    void failPendingRequests(Throwable failure) {
        for (PendingRequest pendingRequest : pendingRequests) {
            if (!pendingRequest.future.isDone()) {
                pendingRequest.future.completeExceptionally(failure);
            }
        }
    }

    List<Call> calls() {
        return List.copyOf(calls);
    }

    List<Call> requests() {
        return List.copyOf(requests);
    }

    List<PendingRequest> pendingRequests() {
        return List.copyOf(pendingRequests);
    }

    static String accessTokenPrefix(Map<String, String> params) {
        return "access-" + key(params) + "#";
    }

    private static String key(Map<String, String> params) {
        return new TreeMap<>(params).toString();
    }

    private Uni<Tokens> record(Call call) {
        calls.add(call);
        return Uni.createFrom().deferred(new Supplier<Uni<? extends Tokens>>() {
            @Override
            public Uni<? extends Tokens> get() {
                requests.add(call);
                if (respondLater) {
                    PendingRequest pendingRequest = new PendingRequest(call);
                    pendingRequests.add(pendingRequest);
                    return Uni.createFrom().completionStage(pendingRequest.future);
                }
                Supplier<Throwable> currentFailure = failure;
                if (currentFailure != null) {
                    return Uni.createFrom().failure(currentFailure.get());
                }
                return Uni.createFrom().item(issue(call));
            }
        });
    }

    private Tokens issue(Call call) {
        Map<String, String> params = call.isGrant() ? call.params() : issuedRefreshTokens.get(call.refreshToken());
        if (params == null) {
            throw new IllegalStateException("Unknown refresh token: " + call.refreshToken());
        }
        int seq = sequence.incrementAndGet();
        String accessToken = accessTokenPrefix(params) + seq;
        String refreshToken = "refresh-" + key(params) + "#" + seq;
        long nowSecs = System.currentTimeMillis() / 1000;
        long accessTokenExpiresAt = nowSecs + 3600 + seq;
        Long refreshTokenExpiresAt = nowSecs + 7200 + seq;
        Duration refreshTokenTimeSkew = null;
        switch (expiry) {
            case VALID:
                break;
            case ACCESS_TOKEN_EXPIRED:
                accessTokenExpiresAt = nowSecs - 10;
                break;
            case ACCESS_TOKEN_WITHIN_REFRESH_INTERVAL:
                accessTokenExpiresAt = nowSecs + 5;
                refreshTokenTimeSkew = Duration.ofSeconds(60);
                break;
            case ACCESS_AND_REFRESH_TOKENS_EXPIRED:
                accessTokenExpiresAt = nowSecs - 10;
                refreshTokenExpiresAt = nowSecs - 10;
                break;
            case ACCESS_TOKEN_EXPIRED_NO_REFRESH_TOKEN:
                accessTokenExpiresAt = nowSecs - 10;
                refreshToken = null;
                refreshTokenExpiresAt = null;
                break;
        }
        if (refreshToken != null) {
            issuedRefreshTokens.put(refreshToken, params);
        }
        JsonObject grantResponse = new JsonObject().put("access_token", accessToken);
        for (Map.Entry<String, String> param : params.entrySet()) {
            grantResponse.put(param.getKey(), param.getValue());
        }
        return new Tokens(accessToken, accessTokenExpiresAt, refreshTokenTimeSkew, refreshToken, refreshTokenExpiresAt,
                grantResponse, CLIENT_ID, null);
    }
}
