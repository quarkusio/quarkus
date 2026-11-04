package io.quarkus.http.ratelimit.runtime.internal.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Checks everything about {@link HttpRateLimitRuntimeConfig} that can be decided from the configuration alone, against
 * the rules of {@link HttpRateLimitBuildTimeConfig}. It does not throw and does not look up beans. What is fixed at build
 * time (paths, methods, phases, keys and stores) is checked by the build.
 */
public final class HttpRateLimitConfigValidator {

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

    private static void validateRule(RateLimitRuleConfig rule, List<String> errors) {
        String prefix = "Rate limit rule '" + rule.name() + "' ";
        if (rule.maxKeys() < 1) {
            errors.add(prefix + "max-keys must be at least 1");
        }
        validateLimit(prefix, rule, errors);
    }

    private static void validateLimit(String prefix, RateLimitRuleConfig rule, List<String> errors) {
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
            return;
        }
        try {
            DefaultRateLimitPolicy.of(rule);
        } catch (IllegalArgumentException e) {
            errors.add(e.getMessage());
        }
    }

    private static boolean isPositive(Duration duration) {
        return !duration.isNegative() && !duration.isZero();
    }
}
