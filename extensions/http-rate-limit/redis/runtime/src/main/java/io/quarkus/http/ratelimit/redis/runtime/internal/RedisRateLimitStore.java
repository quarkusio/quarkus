package io.quarkus.http.ratelimit.redis.runtime.internal;

import static java.time.temporal.ChronoUnit.MICROS;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import io.quarkus.http.ratelimit.RateLimitDecision;
import io.quarkus.http.ratelimit.RateLimitPolicy;
import io.quarkus.http.ratelimit.RateLimitStore;
import io.quarkus.http.ratelimit.runtime.internal.config.HttpRateLimitRuntimeConfig;
import io.quarkus.http.ratelimit.runtime.internal.gcra.Gcra;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.smallrye.mutiny.Uni;
import io.vertx.redis.client.Response;

/**
 * Keeps one Redis STRING per key, {@code keyPrefix + policy.id() + ":" + key}, holding the GCRA theoretical arrival
 * time in epoch microseconds, and decides with {@code try-consume.lua}, which reproduces
 * {@link io.quarkus.http.ratelimit.runtime.internal.gcra.Gcra#tryConsume} on Redis's clock.
 * <p>
 * The key does not depend on the policy's values, so a rolling change of permits or window keeps the stored TAT: it is
 * time debt and is reinterpreted under the new emission interval, which GCRA tolerates.
 * <p>
 * Every call is {@code EVALSHA}. On {@code NOSCRIPT} the same call is re-sent once as {@code EVAL}, which is routed by
 * its key to the shard that owns it (in cluster mode too) and caches the script there. {@code SCRIPT LOAD} is never
 * used: it carries no key, so in a cluster it would reach an arbitrary node and the owning shard would keep answering
 * {@code NOSCRIPT}.
 * <p>
 * The call has no timeout of its own; the guard around the store bounds it.
 * <p>
 * This is the built-in {@code redis} store. The deployment module registers it only when a rule uses it, as a
 * synthetic {@code @Identifier("redis")} bean backed by a service, built from the {@code ReactiveRedisDataSource} of
 * the client named by the build time {@code quarkus.http.rate-limit.redis.client-name}, or of the default client.
 */
public final class RedisRateLimitStore implements RateLimitStore {

    static final String SCRIPT_RESOURCE = "META-INF/http-rate-limit/try-consume.lua";

    private static final String SCRIPT = load();
    private static final String SHA = sha1(SCRIPT);

    private final ReactiveRedisDataSource dataSource;
    private final String keyPrefix;

    public RedisRateLimitStore(ReactiveRedisDataSource dataSource, HttpRateLimitRuntimeConfig config) {
        this.dataSource = dataSource;
        this.keyPrefix = config.redis().keyPrefix();
    }

    @Override
    public Uni<RateLimitDecision> tryConsume(String key, long cost, RateLimitPolicy policy) {
        Gcra.checkCost(cost, policy);
        String redisKey = keyPrefix + policy.id() + ":" + key;
        String t = Long.toString(policy.emissionIntervalMicros());
        String burst = Long.toString(policy.burst());
        String c = Long.toString(cost);
        return dataSource.execute("EVALSHA", SHA, "1", redisKey, t, burst, c)
                .onFailure(new Predicate<>() {
                    @Override
                    public boolean test(Throwable failure) {
                        return isNoScript(failure);
                    }
                })
                .recoverWithUni(new Supplier<>() {
                    @Override
                    public Uni<? extends Response> get() {
                        return dataSource.execute("EVAL", SCRIPT, "1", redisKey, t, burst, c);
                    }
                })
                .map(new Function<>() {
                    @Override
                    public RateLimitDecision apply(Response response) {
                        return decision(response);
                    }
                });
    }

    private static RateLimitDecision decision(Response response) {
        if (response.get(0).toLong() == 1) {
            return RateLimitDecision.allowed(response.get(1).toLong());
        }
        return RateLimitDecision.rejected(Duration.of(response.get(2).toLong(), MICROS));
    }

    private static boolean isNoScript(Throwable failure) {
        String message = failure.getMessage();
        return message != null && message.startsWith("NOSCRIPT");
    }

    private static String load() {
        try (InputStream in = RedisRateLimitStore.class.getClassLoader().getResourceAsStream(SCRIPT_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException(SCRIPT_RESOURCE + " is missing");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String sha1(String script) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-1").digest(script.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
