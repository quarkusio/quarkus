package io.quarkus.http.ratelimit;

/**
 * The point in request processing at which a rate limit rule is evaluated.
 */
public enum RateLimitPhase {

    /**
     * Before authentication; the {@link RateLimitContext} carries no identity.
     */
    PRE_AUTHENTICATION("pre-authentication"),
    /**
     * After authentication; the {@link RateLimitContext} carries the identity, an anonymous one when the request is not
     * authenticated.
     */
    POST_AUTHENTICATION("post-authentication");

    private final String configName;

    RateLimitPhase(String configName) {
        this.configName = configName;
    }

    /**
     * @return {@code "pre-authentication"} or {@code "post-authentication"}: the metrics tag value and Redis key
     *         segment.
     */
    public String configName() {
        return configName;
    }
}
