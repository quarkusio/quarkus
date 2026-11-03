package io.quarkus.it.ratelimit;

import static io.quarkus.it.ratelimit.RedisRateLimitHttpTest.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.http.ratelimit.RateLimit;
import io.quarkus.http.ratelimit.RateLimitPhase;
import io.quarkus.http.ratelimit.RateLimitPolicy;
import io.quarkus.http.ratelimit.runtime.internal.config.DefaultRateLimitPolicy;
import io.quarkus.http.ratelimit.runtime.internal.gcra.Gcra;
import io.quarkus.http.ratelimit.runtime.internal.keys.RateLimitKeys;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.redis.client.Response;

/**
 * Inspects the Redis state behind the HTTP decisions, and checks the shipped Lua script against {@link Gcra}. JVM only:
 * it injects the data sources.
 */
@QuarkusTest
public class RedisRateLimitInspectionTest {

    private static final String PREFIX = "quarkus:http-rate-limit:http-rate-limit-redis:";
    private static final String SCRIPT = "META-INF/http-rate-limit/try-consume.lua";
    private static final String TIME_CALL = "redis.call('TIME')";
    // the api rule: 3 permits per minute
    private static final long API_TAU_US = 60_000_000;
    // the lease rule: 6 permits per minute, lease-size 2
    private static final long LEASE_T_US = 10_000_000;
    private static final long LEASE_TAU_US = 60_000_000;

    @Inject
    RedisDataSource redis;

    @Test
    void stateLivesInRedisWithTtl() {
        String client = UUID.randomUUID().toString();
        get("/api/hello", client).then().statusCode(200);

        String key = apiKey(client);
        Response tat = redis.execute("GET", key);
        assertThat(tat).isNotNull();
        assertThat(Long.parseLong(tat.toString())).isPositive();
        assertThat(redis.execute("PTTL", key).toLong()).isPositive();
    }

    @Test
    void stateSurvivesWithoutLocalMemory() {
        String client = UUID.randomUUID().toString();
        redis.execute("SET", apiKey(client), Long.toString(redisNowMicros() + API_TAU_US), "PX", "60000");

        get("/api/hello", client).then().statusCode(429);
    }

    /**
     * A TAT 30 days ahead, written by a clock that jumped or under a larger policy, costs the key one emission interval
     * (20 s for 3 per minute), not 30 days, and the cap is stored.
     */
    @Test
    void debtAboveTheBurstToleranceIsCapped() {
        String client = UUID.randomUUID().toString();
        long thirtyDays = 30L * 24 * 3600 * 1_000_000;
        redis.execute("SET", apiKey(client), Long.toString(redisNowMicros() + thirtyDays), "PX",
                Long.toString(thirtyDays / 1_000));

        get("/api/hello", client).then().statusCode(429).header("Retry-After", "20");
        assertThat(Long.parseLong(redis.execute("GET", apiKey(client)).toString()) - redisNowMicros())
                .isBetween(0L, API_TAU_US);
        assertThat(redis.execute("PTTL", apiKey(client)).toLong()).isBetween(1L, API_TAU_US / 1_000);
    }

    @Test
    void noscriptIsRecovered() {
        String client = UUID.randomUUID().toString();
        redis.execute("SCRIPT", "FLUSH");

        long evalBefore = calls("eval");
        get("/api/hello", client).then().statusCode(200);
        long evalAfterFirst = calls("eval");
        long evalshaAfterFirst = calls("evalsha");
        // the EVALSHA failed with NOSCRIPT and the call was re-sent once as EVAL, which cached the script again
        assertThat(evalAfterFirst - evalBefore).isEqualTo(1);

        get("/api/hello", client).then().statusCode(200);
        assertThat(calls("eval")).isEqualTo(evalAfterFirst);
        assertThat(calls("evalsha")).isEqualTo(evalshaAfterFirst + 1);
    }

    /**
     * The claim costs are read from the stored TAT: each grant moves it by cost x T, a request served from a held
     * chunk leaves it alone. Sequential requests, T = 10 s and a lease time of 2 T, so no chunk expires and the
     * sequence follows from LeasingRateLimitStore's sizing, min(target, lease-size, tail cap, burst) with
     * tail cap = max(1, (last remaining + held) / 2), the target starting at 1 and doubling (up to lease-size) when a
     * chunk is used up:
     * <ol>
     * <li>no state: target 1: claim 1 (remaining 5), served; the chunk is used up, target 2</li>
     * <li>tail cap 5 / 2 = 2: claim 2 (remaining 3), one served</li>
     * <li>served from the chunk; a chunk of 2 never prefetches (left &lt;= 2 / 4 = 0 only when it is gone)</li>
     * <li>tail cap 3 / 2 = 1: claim 1 (remaining 2)</li>
     * <li>tail cap 2 / 2 = 1: claim 1 (remaining 1)</li>
     * <li>tail cap max(1, 1 / 2) = 1: claim 1 (remaining 0)</li>
     * <li>claim 1 refused: 429, the TAT is unchanged</li>
     * </ol>
     * The tail cap, not the lease size, sizes every claim once the store reports 3 or fewer left.
     */
    @Test
    void leaseClaimsAreSizedByDemandAndTail() {
        String client = UUID.randomUUID().toString();
        String key = PREFIX + "lease/pre-authentication:header:" + RateLimitKeys.sha256(client);
        long evalshaBefore = calls("evalsha");

        get("/lease/hello", client).then().statusCode(200);
        long tat = Long.parseLong(redis.execute("GET", key).toString());
        assertThat(tat - redisNowMicros()).isBetween(0L, LEASE_T_US);
        List<Long> costs = new ArrayList<>();
        costs.add(1L);
        for (int i = 1; i < 6; i++) {
            get("/lease/hello", client).then().statusCode(200);
            long next = Long.parseLong(redis.execute("GET", key).toString());
            assertThat((next - tat) % LEASE_T_US).as("request %d", i + 1).isZero();
            costs.add((next - tat) / LEASE_T_US);
            tat = next;
        }
        assertThat(costs).containsExactly(1L, 2L, 0L, 1L, 1L, 1L);
        assertThat(tat - redisNowMicros()).isBetween(LEASE_TAU_US - LEASE_T_US, LEASE_TAU_US);

        get("/lease/hello", client).then().statusCode(429);
        assertThat(Long.parseLong(redis.execute("GET", key).toString())).isEqualTo(tat);
        // five granted claims and the refused one
        assertThat(calls("evalsha") - evalshaBefore).isEqualTo(6);
    }

    @Test
    void scriptMatchesJavaGcra() {
        String harness = harness();
        // 5 per minute
        parity(harness, policy(5, Duration.ofMinutes(1)), canonical(policy(5, Duration.ofMinutes(1)), 1));
        // the quantisation case: T = 1.5 µs, rounded up to 2 µs, tau = 200 µs
        RateLimitPolicy quantised = policy(100, Duration.ofNanos(150_000));
        assertThat(quantised.emissionIntervalMicros()).isEqualTo(2);
        assertThat(quantised.burstToleranceMicros()).isEqualTo(200);
        parity(harness, quantised, canonical(quantised, 1));
        // non-divisible
        parity(harness, policy(7, Duration.ofMinutes(1)), canonical(policy(7, Duration.ofMinutes(1)), 1));
        // the lease case: costs 5 and 1 interleaved
        parity(harness, policy(20, Duration.ofMinutes(1)), canonical(policy(20, Duration.ofMinutes(1)), 5));
        // random walks over every vector
        Random random = new Random(42);
        for (RateLimitPolicy policy : List.of(policy(5, Duration.ofMinutes(1)), quantised,
                policy(7, Duration.ofMinutes(1)), policy(20, Duration.ofMinutes(1)))) {
            List<long[]> steps = new ArrayList<>();
            long t = policy.emissionIntervalMicros();
            for (int i = 0; i < 300; i++) {
                steps.add(new long[] { random.nextLong(2 * t + 1), random.nextLong(policy.burst() + 1) });
            }
            parity(harness, policy, steps);
        }
    }

    @Test
    void costAboveBurstIsAnError() {
        RateLimitPolicy policy = policy(5, Duration.ofMinutes(1));
        assertThatThrownBy(() -> redis.execute("EVAL", productionScript(), "1", PREFIX + "parity:" + UUID.randomUUID(),
                Long.toString(policy.emissionIntervalMicros()), Long.toString(policy.burst()), "6"))
                .hasMessageContaining("cost");
    }

    @Test
    void tatNeverExceedsNowPlusTauInRedis() {
        // T = 1 ms: the calls come faster than the refill, so the bucket keeps hitting its cap
        RateLimitPolicy policy = policy(100, Duration.ofMillis(100));
        long tau = policy.burstToleranceMicros();
        String script = productionScript();
        String key = PREFIX + "invariant:" + UUID.randomUUID();
        Random random = new Random(7);
        for (int i = 0; i < 10_000; i++) {
            long cost = random.nextInt(10) == 0 ? random.nextLong(policy.burst() + 1) : random.nextLong(6);
            redis.execute("EVAL", script, "1", key, Long.toString(policy.emissionIntervalMicros()),
                    Long.toString(policy.burst()), Long.toString(cost));
            Response tat = redis.execute("GET", key);
            if (tat != null) {
                assertThat(Long.parseLong(tat.toString()) - redisNowMicros()).as("step %d", i).isLessThanOrEqualTo(tau);
            }
        }
    }

    /**
     * Steps of {@code [advance µs, cost]}: exhaustion and burst + 1 calls at one instant, probes, and the refill
     * boundaries just before and at the time {@code cost} permits are back, then a full refill and exhaustion again.
     */
    private static List<long[]> canonical(RateLimitPolicy policy, long leaseCost) {
        long t = policy.emissionIntervalMicros();
        long tau = policy.burstToleranceMicros();
        List<long[]> steps = new ArrayList<>();
        steps.add(new long[] { 0, 0 });
        for (long i = 0; i <= policy.burst(); i++) {
            // with a lease cost, claims of leaseCost and single permits interleave
            steps.add(new long[] { 0, i % 2 == 0 ? leaseCost : 1 });
        }
        for (long i = 0; i <= policy.burst(); i++) {
            steps.add(new long[] { 0, 1 });
        }
        steps.add(new long[] { 0, 0 });
        for (long cost : new long[] { 1, leaseCost }) {
            steps.add(new long[] { cost * t - 1, cost });
            steps.add(new long[] { 0, 0 });
            steps.add(new long[] { 1, cost });
            steps.add(new long[] { 0, cost });
        }
        steps.add(new long[] { t, 0 });
        steps.add(new long[] { tau, 0 });
        for (long i = 0; i <= policy.burst(); i++) {
            steps.add(new long[] { 0, 1 });
        }
        steps.add(new long[] { tau - 1, 1 });
        steps.add(new long[] { 2 * tau, 0 });
        steps.add(new long[] { 0, policy.burst() });
        steps.add(new long[] { 0, 1 });
        return steps;
    }

    /**
     * Runs the steps through the harness and {@link Gcra} side by side, from an instant near the real epoch time so
     * that the Lua doubles hold realistic magnitudes.
     */
    private void parity(String harness, RateLimitPolicy policy, List<long[]> steps) {
        long t = policy.emissionIntervalMicros();
        long tau = policy.burstToleranceMicros();
        String key = PREFIX + "parity:" + UUID.randomUUID();
        long now = System.currentTimeMillis() * 1_000 + 123_457;
        long tat = Gcra.NO_STATE;
        for (int i = 0; i < steps.size(); i++) {
            now += steps.get(i)[0];
            long cost = steps.get(i)[1];
            Gcra.Outcome expected = Gcra.tryConsume(tat, now, cost, t, tau);
            Response actual = redis.execute("EVAL", harness, "1", key, Long.toString(t), Long.toString(policy.burst()),
                    Long.toString(cost), Long.toString(now / 1_000_000), Long.toString(now % 1_000_000));
            String step = policy.id() + " " + policy.permits() + "/" + policy.window() + " step " + i + " (cost " + cost
                    + ")";
            assertThat(actual.get(0).toLong()).as(step + " allowed").isEqualTo(expected.allowed() ? 1 : 0);
            // a rejection reports the permits available before the call too
            assertThat(actual.get(1).toLong()).as(step + " remaining").isEqualTo(expected.remaining());
            assertThat(actual.get(2).toLong()).as(step + " retry after").isEqualTo(expected.retryAfterNanos());
            if (expected.allowed() && cost > 0) {
                tat = expected.newTat();
                long px = Math.max(1, Math.ceilDiv(tat - now, 1_000));
                assertThat(actual.get(4).toLong()).as(step + " pttl").isBetween(px - 100, px);
            }
            if (tat == Gcra.NO_STATE) {
                assertThat(actual.get(3).toString()).as(step + " tat").isEmpty();
            } else {
                assertThat(Long.parseLong(actual.get(3).toString())).as(step + " tat").isEqualTo(tat);
                assertThat(tat - now).as(step + " tat - now").isLessThanOrEqualTo(tau);
            }
        }
    }

    /**
     * The production script with only {@code redis.call('TIME')} replaced by a clock taken from ARGV[4] and ARGV[5],
     * wrapped in a function so that the stored TAT and its PTTL are read in the same script: Redis freezes the clock
     * that drives key expiry while a script runs, so the key cannot expire on Redis's real clock before it is read.
     * PERSIST then keeps it from expiring between steps, since the injected clock is not Redis's.
     */
    private static String harness() {
        String production = productionScript();
        assertThat(production.split(Pattern.quote(TIME_CALL), -1)).hasSize(2);
        return "local function fixed_time() return {ARGV[4], ARGV[5]} end\n"
                + "local function production()\n"
                + production.replace(TIME_CALL, "fixed_time()")
                + "\nend\n"
                + "local r = production()\n"
                + "local tat = redis.call('GET', KEYS[1])\n"
                + "local pttl = redis.call('PTTL', KEYS[1])\n"
                + "redis.call('PERSIST', KEYS[1])\n"
                + "return {r[1], r[2], r[3], tat or '', pttl}\n";
    }

    private static String productionScript() {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(SCRIPT)) {
            assertThat(in).as(SCRIPT).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static RateLimitPolicy policy(long permits, Duration window) {
        return new DefaultRateLimitPolicy("parity", RateLimitPhase.PRE_AUTHENTICATION, RateLimit.of(permits, window, permits),
                1_000);
    }

    static String apiKey(String client) {
        return PREFIX + "api/pre-authentication:header:" + RateLimitKeys.sha256(client);
    }

    private long redisNowMicros() {
        Response time = redis.execute("TIME");
        return time.get(0).toLong() * 1_000_000 + time.get(1).toLong();
    }

    private long calls(String command) {
        for (String line : redis.execute("INFO", "commandstats").toString().split("\r?\n")) {
            if (line.startsWith("cmdstat_" + command + ":")) {
                for (String field : line.substring(line.indexOf(':') + 1).split(",")) {
                    if (field.startsWith("calls=")) {
                        return Long.parseLong(field.substring("calls=".length()));
                    }
                }
            }
        }
        return 0;
    }
}
