package io.quarkus.http.ratelimit.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import io.quarkus.http.ratelimit.RateLimitPhase;
import io.quarkus.http.ratelimit.runtime.internal.config.HttpRateLimitBuildTimeConfig;
import io.quarkus.http.ratelimit.runtime.internal.config.HttpRateLimitBuildTimeConfig.Rule;
import io.quarkus.http.ratelimit.runtime.internal.config.RateLimitRuleConfig;

/**
 * Checks everything about {@link HttpRateLimitBuildTimeConfig} at build time: the phase, key, limit resolver and store of
 * every rule. It does not throw.
 */
public final class HttpRateLimitBuildConfigValidator {

    // RFC 9110 token
    private static final Pattern TOKEN = Pattern.compile("[!#$%&'*+\\-.^_`|~0-9A-Za-z]+");
    private static final String PREFIX = "quarkus.http.rate-limit.";
    private static final String LOCAL_STORE = "local";
    // a key that KeyResolvers turns into a HeaderKeyResolver, not a bean
    private static final String HEADER_KEY_PREFIX = "header:";
    // a key that names a RateLimitKeyResolver bean of the application, by its @Identifier
    private static final String CUSTOM_KEY_PREFIX = "custom:";
    private static final Set<String> BUILT_IN_KEYS = Set.of("ip", "global", "principal");

    private HttpRateLimitBuildConfigValidator() {
    }

    /**
     * @param stores the {@code @Identifier} of every {@code RateLimitStore} bean
     * @param keyResolvers the {@code @Identifier} of every {@code RateLimitKeyResolver} bean, built-in ones included
     * @param limitResolvers the {@code @Identifier} of every {@code RateLimitResolver} bean
     * @return one message per violation; empty when the configuration is valid
     */
    public static List<String> validate(HttpRateLimitBuildTimeConfig config, Set<String> stores, Set<String> keyResolvers,
            Set<String> limitResolvers) {
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
            String store = RateLimitRuleConfig.store(config, rule);
            if (!LOCAL_STORE.equals(store) && !stores.contains(store)) {
                errors.add(prefix + "store '" + store + "' matches no RateLimitStore bean annotated @Identifier(\""
                        + store + "\"); the built-in 'redis' store requires the quarkus-redis-client extension");
            }
            Optional<String> limitResolver = rule.limitResolver().or(() -> config.limitResolver());
            if (limitResolver.isPresent() && !limitResolvers.contains(limitResolver.get())) {
                errors.add(prefix + "limit-resolver '" + limitResolver.get()
                        + "' matches no RateLimitResolver bean annotated @Identifier(\"" + limitResolver.get() + "\")");
            }
        }
        return errors;
    }

    /**
     * @param proactive {@code quarkus.http.auth.proactive}
     * @param mechanisms whether there is an {@code HttpAuthenticationMechanism} bean
     * @return one message per rule that is valid but probably not what was intended
     */
    public static List<String> warnings(HttpRateLimitBuildTimeConfig config, boolean proactive, boolean mechanisms) {
        List<String> warnings = new ArrayList<>();
        for (Map.Entry<String, Rule> entry : config.rules().entrySet()) {
            Rule rule = entry.getValue();
            if (RateLimitRuleConfig.phase(config, rule) != RateLimitPhase.POST_AUTHENTICATION) {
                continue;
            }
            if (!proactive) {
                // HttpAuthenticator.getPathSpecificMechanism reads routing context data that only the REST layer sets
                warnings.add("Rate limit rule '" + entry.getKey()
                        + "' runs after authentication but quarkus.http.auth.proactive=false:"
                        + " authentication is now forced for every request to its paths, using the mechanisms selected"
                        + " by path; mechanisms selected by REST annotations are not known at this point and are not"
                        + " applied");
            }
            if ("principal".equals(RateLimitRuleConfig.key(config, rule)) && !mechanisms) {
                warnings.add("Rate limit rule '" + entry.getKey()
                        + "' uses request-key 'principal' but no HttpAuthenticationMechanism bean"
                        + " is present; every request will be anonymous");
            }
        }
        return warnings;
    }

    /**
     * @param phase the phase of the rule; {@code null} when neither the rule nor the global setting sets one
     * @param key the key of the rule; {@code null} when neither the rule nor the global setting sets one
     */
    private static void validateKey(String name, String prefix, RateLimitPhase phase, String key,
            Set<String> keyResolvers, List<String> errors) {
        if (phase == null) {
            errors.add(prefix + "has no phase: set " + PREFIX + "rules." + name + ".phase or " + PREFIX + "phase");
        }
        if (key == null) {
            errors.add(prefix + "has no request-key: set " + PREFIX + "rules." + name + ".request-key or " + PREFIX
                    + "request-key");
            return;
        }
        if (phase == RateLimitPhase.PRE_AUTHENTICATION && "principal".equals(key)) {
            errors.add(prefix
                    + "request-key 'principal' is not available before authentication; use phase=post-authentication");
        }
        if (BUILT_IN_KEYS.contains(key)) {
            return;
        }
        if (key.startsWith(HEADER_KEY_PREFIX) && key.length() > HEADER_KEY_PREFIX.length()) {
            String header = key.substring(HEADER_KEY_PREFIX.length());
            if (!TOKEN.matcher(header).matches()) {
                errors.add(PREFIX + "rules." + name + ".request-key: header name '" + header
                        + "' is not an HTTP field name; use header:<name>, for example header:X-Tenant");
            }
            return;
        }
        if (key.startsWith(CUSTOM_KEY_PREFIX) && key.length() > CUSTOM_KEY_PREFIX.length()) {
            String identifier = key.substring(CUSTOM_KEY_PREFIX.length());
            if (!keyResolvers.contains(identifier)) {
                errors.add(prefix + "request-key '" + key
                        + "' matches no RateLimitKeyResolver bean annotated @Identifier(\"" + identifier + "\")");
            }
            return;
        }
        String error = prefix + "request-key '" + key
                + "' is not a built-in key (ip, global, principal), header:<name> or custom:<identifier>";
        if (keyResolvers.contains(key)) {
            error += "; for the RateLimitKeyResolver bean annotated @Identifier(\"" + key + "\"), use custom:" + key;
        }
        errors.add(error);
    }

    /**
     * Rule paths include {@code quarkus.http.root-path}, so an absolute path that can match nothing under it, nor under the
     * non-application root path, would never apply.
     *
     * @param rootPath {@code quarkus.http.root-path}
     * @param nonApplicationRootPath {@code quarkus.http.non-application-root-path}, absolute or relative to the root path
     * @return one message per rule path outside both root paths
     */
    public static List<String> validateRootPath(HttpRateLimitBuildTimeConfig config, String rootPath,
            String nonApplicationRootPath) {
        List<String> errors = new ArrayList<>();
        String root = directory(rootPath);
        if (root.equals("/")) {
            return errors;
        }
        String nonApplicationRoot = nonApplicationRootPath.startsWith("/")
                ? directory(nonApplicationRootPath)
                : root + directory(nonApplicationRootPath).substring(1);
        for (Map.Entry<String, Rule> entry : config.rules().entrySet()) {
            for (String path : entry.getValue().paths()) {
                if (path.startsWith("/") && outside(path, root) && outside(path, nonApplicationRoot)) {
                    errors.add("Rate limit rule '" + entry.getKey() + "' path '" + path
                            + "' is outside quarkus.http.root-path '" + root
                            + "', so it matches no request; write it under the root path");
                }
            }
        }
        return errors;
    }

    private static void validatePaths(String prefix, Rule rule, List<String> errors) {
        if (rule.paths().isEmpty()) {
            errors.add(prefix + "paths must be set");
            return;
        }
        for (String path : rule.paths()) {
            if (path.isBlank()) {
                errors.add(prefix + "path '" + path + "' must not be blank");
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

    private static String directory(String path) {
        String withLeading = path.startsWith("/") ? path : "/" + path;
        return withLeading.endsWith("/") ? withLeading : withLeading + "/";
    }

    /**
     * @param directory a path with a leading and a trailing slash
     * @return whether no request path under the directory, nor the directory itself, can match the pattern: an exact
     *         path matches only itself, so it must lie under the directory; for a pattern with a wildcard, the part
     *         before its first wildcard is compared segment by segment, and may also be a parent of the directory
     */
    private static boolean outside(String pattern, String directory) {
        int wildcard = pattern.indexOf('*');
        if (wildcard < 0) {
            return !directory(pattern).startsWith(directory);
        }
        String literal = pattern.substring(0, wildcard);
        return !literal.startsWith(directory) && !directory.startsWith(literal);
    }

}
