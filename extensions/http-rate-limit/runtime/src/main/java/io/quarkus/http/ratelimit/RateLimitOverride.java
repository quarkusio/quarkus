package io.quarkus.http.ratelimit;

import java.time.Duration;
import java.util.Optional;

import io.smallrye.mutiny.Uni;

/**
 * Gives some keys of a rule a limit other than the configured one. A rule applies the same limit to every key; an
 * override is how different clients get different limits, for example plans or tiers: every API key gets 100 requests
 * a minute, but the key of a client on a paid plan gets 1000, looked up from wherever the application keeps its plans.
 * <p>
 * Implement it as a CDI bean annotated {@code @Identifier("<name>")} and select it for a rule with
 * {@code quarkus.http.rate-limit."rule".override=<name>}, or for every rule with
 * {@code quarkus.http.rate-limit.override}. It is called for every request the rule evaluates, after the key is
 * resolved, and returns the limit for that key, or nothing to keep the configured one. The key keeps its state when its
 * limit changes: that state is the time the key owes, not a count of used permits, so raising {@code permits} for a key
 * that has used up its burst lets it through only once the debt fits the new limit.
 * <p>
 * Runs on the event loop; must not block, and must answer within 250 ms. If the override fails, does not answer in
 * time, or returns a limit that would be invalid as configuration ({@code permits} or {@code burst} below 1,
 * {@code window / permits} under 1 microsecond, or {@code (window / permits) * burst} over 30 days), the key has the
 * configured limit, and a warning is logged at most once per warning interval per rule.
 */
public interface RateLimitOverride {

    /**
     * @param context the request being evaluated
     * @param key the resolved key; a key longer than 256 characters, or one that starts with
     *        {@code sha256:} or contains a brace, arrives as {@code sha256:} followed by the hex SHA-256 of the resolved key
     * @return the limit to use for this key, or empty to use the configured limit
     */
    Uni<Optional<Limit>> override(RateLimitContext context, String key);

    /**
     * The limit that replaces the configured one.
     */
    interface Limit {

        /**
         * @param permits the number of permits replenished per {@code window}
         * @param window the replenishment window
         * @return a limit whose burst defaults to {@code permits}
         */
        static Limit of(long permits, Duration window) {
            return new OverrideLimit(permits, window, Optional.empty());
        }

        /**
         * @param permits the number of permits replenished per {@code window}
         * @param window the replenishment window
         * @param burst the burst size
         * @return a limit with an explicit burst
         */
        static Limit of(long permits, Duration window, long burst) {
            return new OverrideLimit(permits, window, Optional.of(burst));
        }

        /**
         * @return the number of permits replenished per {@link #window()}
         */
        long permits();

        /**
         * @return the replenishment window
         */
        Duration window();

        /**
         * @return the burst size, defaults to {@code permits}
         */
        Optional<Long> burst();
    }
}
