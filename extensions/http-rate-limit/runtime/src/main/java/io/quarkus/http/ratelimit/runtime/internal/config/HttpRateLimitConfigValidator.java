package io.quarkus.http.ratelimit.runtime.internal.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.http.ratelimit.RateLimitPolicy;

/**
 * Checks everything about {@link HttpRateLimitRuntimeConfig} that can be decided from the configuration alone, against
 * the rules of {@link HttpRateLimitBuildTimeConfig}. It does not throw and does not look up beans. What is fixed at build
 * time (paths, methods, phases, keys and stores) is checked by the build.
 */
public final class HttpRateLimitConfigValidator {

    private static final String LOCAL_STORE = "local";
    private static final String PREFIX = "quarkus.http.rate-limit.";

    private HttpRateLimitConfigValidator() {
    }

    /**
     * @return one message per violation; empty when the configuration is valid
     */
    public static List<String> validate(HttpRateLimitBuildTimeConfig buildConfig, HttpRateLimitRuntimeConfig runtimeConfig) {
        List<String> errors = new ArrayList<>();
        if (runtimeConfig.warningInterval().compareTo(Duration.ofSeconds(1)) < 0) {
            errors.add(PREFIX + "warning-interval must be at least 1 second");
        }
        validateStores(runtimeConfig, errors);
        for (String name : buildConfig.rules().keySet()) {
            if (runtimeConfig.rules().containsKey(name)) {
                validateRule(RateLimitRuleConfig.of(name, buildConfig, runtimeConfig), errors);
            } else {
                errors.add("Rate limit rule '" + name + "' has no limit: set " + PREFIX + name + ".permits and "
                        + PREFIX + name + ".window");
            }
        }
        for (String name : runtimeConfig.rules().keySet()) {
            if (!buildConfig.rules().containsKey(name)) {
                errors.add("Rate limit rule '" + name + "' is not defined at build time: its paths ("
                        + PREFIX + name + ".paths) must be set when the application is built");
            }
        }
        return errors;
    }

    /**
     * @param rules the rules of a valid configuration
     * @return one message per configuration that is accepted but probably not what was intended
     */
    public static List<String> warnings(List<RateLimitRuleConfig> rules) {
        List<String> warnings = new ArrayList<>();
        for (RateLimitRuleConfig rule : rules) {
            if (rule.leaseSize().isPresent() && rule.leaseSize().get() > rule.permits() / 20.0) {
                warnings.add("Rate limit rule '" + rule.name()
                        + "' lease-size × replicas may strand a large share of permits"
                        + " (lease-size <= permits / 20 is recommended)");
            }
        }
        return warnings;
    }

    private static void validateStores(HttpRateLimitRuntimeConfig config, List<String> errors) {
        if (!isPositive(config.storeTimeout())) {
            errors.add(PREFIX + "store-timeout must be positive");
        }
        if (config.redis().timeout().isPresent() && !isPositive(config.redis().timeout().get())) {
            errors.add(PREFIX + "redis.timeout must be positive");
        }
    }

    private static void validateRule(RateLimitRuleConfig rule, List<String> errors) {
        String prefix = "Rate limit rule '" + rule.name() + "' ";
        if (rule.maxKeys() < 1) {
            errors.add(prefix + "max-keys must be at least 1");
        }
        RateLimitPolicy policy = validateLimit(prefix, rule, errors);
        validateLease(prefix, rule, policy, errors);
    }

    /**
     * @return the rule's policy; {@code null} when the limit is invalid
     */
    private static RateLimitPolicy validateLimit(String prefix, RateLimitRuleConfig rule, List<String> errors) {
        boolean valid = true;
        if (rule.permits() < 1) {
            errors.add(prefix + "permits must be at least 1");
            valid = false;
        }
        if (!isPositive(rule.window())) {
            errors.add(prefix + "window must be positive");
            valid = false;
        }
        if (rule.burst().isPresent() && rule.burst().get() < 1) {
            errors.add(prefix + "burst must be at least 1");
            valid = false;
        }
        if (!valid) {
            return null;
        }
        try {
            return DefaultRateLimitPolicy.of(rule);
        } catch (IllegalArgumentException e) {
            errors.add(e.getMessage());
            return null;
        }
    }

    /**
     * @param policy the rule's policy; {@code null} when the limit is invalid, which skips the checks against the burst
     */
    private static void validateLease(String prefix, RateLimitRuleConfig rule, RateLimitPolicy policy,
            List<String> errors) {
        if (rule.leaseSize().isEmpty()) {
            if (rule.maxOverdraft() != 0) {
                errors.add(prefix + "max-overdraft requires lease-size");
            }
            return;
        }
        if (LOCAL_STORE.equals(rule.store())) {
            errors.add(prefix + "lease-size requires a non-local store");
        }
        if (policy == null) {
            return;
        }
        long burst = policy.burst();
        long leaseSize = rule.leaseSize().get();
        if (leaseSize < 1 || leaseSize > burst) {
            errors.add(prefix + "lease-size must be between 1 and the burst (" + burst + ")");
        }
        long maxOverdraft = rule.maxOverdraft();
        if (maxOverdraft < 0 || maxOverdraft > burst) {
            errors.add(prefix + "max-overdraft must be between 0 and the burst (" + burst + ")");
        }
    }

    private static boolean isPositive(Duration duration) {
        return !duration.isNegative() && !duration.isZero();
    }
}
