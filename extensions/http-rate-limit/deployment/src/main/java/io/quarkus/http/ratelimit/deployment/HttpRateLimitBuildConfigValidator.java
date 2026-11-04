package io.quarkus.http.ratelimit.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import io.quarkus.http.ratelimit.RateLimitPhase;
import io.quarkus.http.ratelimit.runtime.internal.config.HttpRateLimitBuildTimeConfig;
import io.quarkus.http.ratelimit.runtime.internal.config.HttpRateLimitBuildTimeConfig.Rule;
import io.quarkus.http.ratelimit.runtime.internal.config.RateLimitRuleConfig;

/**
 * Checks everything about {@link HttpRateLimitBuildTimeConfig} at build time: the paths, methods, phase and key of every
 * rule. It does not throw.
 */
public final class HttpRateLimitBuildConfigValidator {

    private static final String PREFIX = "quarkus.http.rate-limit.";
    // a key that KeyResolvers turns into a HeaderKeyResolver, not a bean
    private static final String HEADER_KEY_PREFIX = "header:";
    // RFC 9110 token
    private static final Pattern TOKEN = Pattern.compile("[!#$%&'*+\\-.^_`|~0-9A-Za-z]+");

    private HttpRateLimitBuildConfigValidator() {
    }

    /**
     * @param keyResolvers the {@code @Identifier} of every {@code RateLimitKeyResolver} bean, the built-in keys
     * @return one message per violation; empty when the configuration is valid
     */
    public static List<String> validate(HttpRateLimitBuildTimeConfig config, Set<String> keyResolvers) {
        List<String> errors = new ArrayList<>();
        for (Map.Entry<String, Rule> entry : config.rules().entrySet()) {
            String name = entry.getKey();
            Rule rule = entry.getValue();
            String prefix = "Rate limit rule '" + name + "' ";
            validatePaths(prefix, rule, errors);
            if (rule.methods().isPresent()) {
                for (String method : rule.methods().get()) {
                    if (!TOKEN.matcher(method).matches()) {
                        errors.add(prefix + "method '" + method + "' is not an HTTP method token");
                    }
                }
            }
            validateKey(name, prefix, RateLimitRuleConfig.phase(config, rule), RateLimitRuleConfig.key(config, rule),
                    keyResolvers, errors);
        }
        return errors;
    }

    private static void validatePaths(String prefix, Rule rule, List<String> errors) {
        for (String path : rule.paths()) {
            if (!path.startsWith("/")) {
                errors.add(prefix + "path '" + path + "' must start with '/'");
                continue;
            }
            for (String segment : path.split("/")) {
                if (segment.contains("*") && !segment.equals("*")) {
                    errors.add(prefix + "path '" + path
                            + "': a wildcard must be a whole path segment ('/a/*/b' or a trailing '/*'), not part of one");
                    break;
                }
            }
        }
    }

    /**
     * @param phase the phase of the rule; {@code null} when neither the rule nor the global setting sets one
     * @param key the key of the rule; {@code null} when neither the rule nor the global setting sets one
     */
    private static void validateKey(String name, String prefix, RateLimitPhase phase, String key,
            Set<String> keyResolvers, List<String> errors) {
        if (phase == null) {
            errors.add(prefix + "has no phase: set " + PREFIX + name + ".phase or " + PREFIX + "phase");
        }
        if (key == null) {
            errors.add(prefix + "has no request-key: set " + PREFIX + name + ".request-key or " + PREFIX + "request-key");
            return;
        }
        if (phase == RateLimitPhase.PRE_AUTHENTICATION && "principal".equals(key)) {
            errors.add(prefix
                    + "request-key 'principal' is not available before authentication; use phase=post-authentication");
        }
        boolean header = key.startsWith(HEADER_KEY_PREFIX) && key.length() > HEADER_KEY_PREFIX.length();
        if (!header && !keyResolvers.contains(key)) {
            errors.add(prefix + "request-key '" + key + "' is not a built-in key: ip, principal, global or"
                    + " header:<name>");
        }
    }
}
