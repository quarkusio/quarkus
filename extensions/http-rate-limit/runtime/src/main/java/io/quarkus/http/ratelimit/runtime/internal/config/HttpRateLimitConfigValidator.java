package io.quarkus.http.ratelimit.runtime.internal.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.http.ratelimit.RateLimit;
import io.quarkus.http.ratelimit.runtime.internal.config.HttpRateLimitRuntimeConfig.Rule;

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

    /**
     * @param rules the rules of a valid configuration
     * @return one message per configuration that is accepted but probably not what was intended
     */
    public static List<String> warnings(List<RateLimitRuleConfig> rules) {
        List<String> warnings = new ArrayList<>();
        for (RateLimitRuleConfig rule : rules) {
            if (rule.leaseSize().isPresent() && rule.limit().isPresent()
                    && rule.leaseSize().get() > rule.limit().get().permits() / 20.0) {
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
        RateLimit policy = rule.limit().orElse(null);
        validateLease(prefix, rule, policy, errors);
    }

    /**
     * @param policy the rule's policy; {@code null} when the limit is invalid, which skips the checks against the burst
     */
    private static void validateLease(String prefix, RateLimitRuleConfig rule, RateLimit policy,
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
            if (rule.leaseSize().get() < 1) {
                errors.add(prefix + "lease-size must be at least 1");
            }
            if (rule.maxOverdraft() < 0) {
                errors.add(prefix + "max-overdraft must not be negative");
            }
            // Dynamic bursts are known only per request; the lease store caps chunks and overdrafts to that burst.
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
