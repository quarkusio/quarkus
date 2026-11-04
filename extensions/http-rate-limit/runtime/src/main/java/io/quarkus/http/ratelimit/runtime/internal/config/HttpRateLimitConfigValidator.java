package io.quarkus.http.ratelimit.runtime.internal.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.http.ratelimit.runtime.internal.config.HttpRateLimitRuntimeConfig.Rule;

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
        validateStores(runtimeConfig, errors);
        if (!isPositive(runtimeConfig.keyResolverTimeout())) {
            errors.add(PREFIX + "key-resolver-timeout must be positive");
        }
        if (!isPositive(runtimeConfig.limitResolverTimeout())) {
            errors.add(PREFIX + "limit-resolver-timeout must be positive");
        }
        for (String name : runtimeConfig.limits().keySet()) {
            try {
                RateLimitRuleConfig.named(runtimeConfig, name);
            } catch (IllegalArgumentException e) {
                errors.add(PREFIX + "limits." + name + ": " + e.getMessage());
            }
        }
        for (String name : buildConfig.rules().keySet()) {
            Rule runtime = runtimeConfig.rules().get(name);
            if (!runtime.enabled()) {
                continue;
            }
            String prefix = "Rate limit rule '" + name + "' ";
            boolean dynamic = RateLimitRuleConfig.limitResolver(buildConfig, buildConfig.rules().get(name), runtime)
                    .isPresent();
            boolean inline = runtime.permits().isPresent() || runtime.window().isPresent() || runtime.burst().isPresent();
            if ((dynamic && (inline || runtime.limit().isPresent())) || (inline && runtime.limit().isPresent())) {
                errors.add(prefix + "select exactly one of inline permits/window, limit, or limit-resolver");
                continue;
            }
            if (!dynamic && runtime.limit().isEmpty() && (runtime.permits().isEmpty() || runtime.window().isEmpty())) {
                errors.add(prefix + "has no complete limit: set " + PREFIX + "rules." + name
                        + ".permits and window, limit, or limit-resolver");
                continue;
            }
            if (runtime.fallbackLimit().isPresent() && !dynamic) {
                errors.add(prefix + "fallback-limit requires a limit-resolver");
                continue;
            }
            try {
                validateRule(RateLimitRuleConfig.of(name, buildConfig, runtimeConfig), errors);
            } catch (IllegalArgumentException e) {
                errors.add(prefix + e.getMessage());
            }
        }
        for (String name : runtimeConfig.rules().keySet()) {
            if (!buildConfig.rules().containsKey(name)) {
                errors.add("Rate limit rule '" + name + "' is not defined at build time: its paths ("
                        + PREFIX + "rules." + name + ".paths) must be set when the application is built");
            }
        }
        return errors;
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
    }

    private static boolean isPositive(Duration duration) {
        return !duration.isNegative() && !duration.isZero();
    }

}
