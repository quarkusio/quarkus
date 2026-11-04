package io.quarkus.http.ratelimit.runtime.internal.rules;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.http.ratelimit.RateLimit;
import io.quarkus.http.ratelimit.RateLimitKeyResolver;
import io.quarkus.http.ratelimit.RateLimitPhase;
import io.quarkus.http.ratelimit.RateLimitResolver;
import io.quarkus.http.ratelimit.runtime.internal.RateLimitBuildTimeInfo;
import io.quarkus.http.ratelimit.runtime.internal.ThrottledLoggers;
import io.quarkus.http.ratelimit.runtime.internal.config.HttpRateLimitBuildTimeConfig;
import io.quarkus.http.ratelimit.runtime.internal.config.HttpRateLimitConfigValidator;
import io.quarkus.http.ratelimit.runtime.internal.config.HttpRateLimitRuntimeConfig;
import io.quarkus.http.ratelimit.runtime.internal.config.RateLimitRuleConfig;
import io.quarkus.http.ratelimit.runtime.internal.keys.KeyResolvers;
import io.quarkus.http.ratelimit.runtime.internal.store.StoreRegistry;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.vertx.http.runtime.VertxHttpBuildTimeConfig;
import io.smallrye.common.annotation.Identifier;

/**
 * Turns the configuration into one {@link RuleEvaluator} per configured rule. Fails startup on an
 * invalid configuration.
 */
@Singleton
public class RateLimitRules {

    private static final Logger LOG = Logger.getLogger(RateLimitRules.class);

    /**
     * Evaluation order: static windows ascending, then dynamic rules, ties by rule name, so that the narrowest rule refuses a
     * request before a
     * wider one spends a permit on it.
     */
    private static final Comparator<RuleMembership> EVALUATION_ORDER = new Comparator<>() {
        @Override
        public int compare(RuleMembership a, RuleMembership b) {
            Duration windowA = a.evaluator().configuration().limit().map(RateLimit::window).orElse(null);
            Duration windowB = b.evaluator().configuration().limit().map(RateLimit::window).orElse(null);
            int byWindow = Comparator.nullsLast(Comparator.<Duration> naturalOrder()).compare(windowA, windowB);
            if (byWindow != 0) {
                return byWindow;
            }
            return a.evaluator().rule().compareTo(b.evaluator().rule());
        }
    };

    private final Map<RateLimitPhase, PhaseIndex> phases;
    private final Set<String> exemptPaths;

    @Inject
    public RateLimitRules(HttpRateLimitBuildTimeConfig buildConfig,
            HttpRateLimitRuntimeConfig config,
            RateLimitBuildTimeInfo info, VertxHttpBuildTimeConfig httpConfig,
            Instance<StoreRegistry> storeRegistry,
            @Any Instance<RateLimitKeyResolver> resolvers,
            @Any Instance<RateLimitResolver> limitResolvers,
            ThrottledLoggers loggers) {
        if (!config.enabled()) {
            // a disabled extension validates nothing and builds no evaluators, stores or probes
            this.phases = indexPhases(List.of());
            this.exemptPaths = Set.of();
            return;
        }
        // resolved only now: a disabled extension creates no store registry
        StoreRegistry stores = storeRegistry.get();
        List<RateLimitRuleConfig> ruleConfigs = validate(buildConfig, config);
        this.exemptPaths = Set.copyOf(info.controlPlanePaths());
        // the summary first: building the evaluators logs the detail of each rule
        logRules(ruleConfigs);
        List<RuleMembership> memberships = buildEvaluators(ruleConfigs, stores, resolvers, limitResolvers,
                config, httpConfig.rootPath(), loggers);
        this.phases = indexPhases(memberships);
    }

    /**
     * @return the evaluators of the phase, in evaluation order
     */
    public List<RuleEvaluator> evaluators(RateLimitPhase phase) {
        return phases.get(phase).evaluators();
    }

    /**
     * @return the matcher of the phase; its matches are in evaluation order
     */
    public RateLimitPathMatcher<RuleMembership> matcher(RateLimitPhase phase) {
        return phases.get(phase).matcher();
    }

    /**
     * @return exact paths that are never rate limited, compared with the canonical request path,
     *         {@code HttpSecurityUtils.normalizePath(RoutingContext.normalizedPath())}
     */
    public Set<String> exemptPaths() {
        return exemptPaths;
    }

    /**
     * @return the rules of the valid configuration
     */
    private static List<RateLimitRuleConfig> validate(HttpRateLimitBuildTimeConfig buildConfig,
            HttpRateLimitRuntimeConfig config) {
        List<String> errors = HttpRateLimitConfigValidator.validate(buildConfig, config);
        if (!errors.isEmpty()) {
            throw new ConfigurationException(String.join("\n", errors));
        }
        return RateLimitRuleConfig.all(buildConfig, config);
    }

    /**
     * @return the limit resolver the rule selects, or {@code null} when it selects none
     */
    private static RateLimitResolver limitResolver(RateLimitRuleConfig rule, Instance<RateLimitResolver> limitResolvers) {
        if (rule.limitResolver().isEmpty()) {
            return null;
        }
        return limitResolvers.select(Identifier.Literal.of(rule.limitResolver().get())).get();
    }

    private static List<RuleMembership> buildEvaluators(List<RateLimitRuleConfig> rules,
            StoreRegistry stores,
            Instance<RateLimitKeyResolver> resolvers,
            Instance<RateLimitResolver> limitResolvers,
            HttpRateLimitRuntimeConfig config, String rootPath, ThrottledLoggers loggers) {
        List<RuleMembership> memberships = new ArrayList<>();
        for (RateLimitRuleConfig rule : rules) {
            RuleEvaluator evaluator = new RuleEvaluator(rule,
                    KeyResolvers.forKey(rule.key(), resolvers), limitResolver(rule, limitResolvers),
                    stores.forRule(rule), rule.store(), config, loggers);
            Set<String> methods = Set.of();
            if (rule.methods().isPresent()) {
                Set<String> upperCase = new HashSet<>();
                for (String m : rule.methods().get()) {
                    upperCase.add(m.toUpperCase(Locale.ROOT));
                }
                methods = Set.copyOf(upperCase);
            }
            List<String> paths = new ArrayList<>();
            for (String path : rule.paths()) {
                paths.add(path.startsWith("/") ? path : (rootPath.endsWith("/") ? rootPath : rootPath + "/") + path);
            }
            memberships.add(new RuleMembership(evaluator, paths, methods));
            LOG.debugf("Rate limit rule '%s': paths=%s methods=%s phase=%s request-key=%s limit=%s limit-resolver=%s store=%s"
                    + " max-keys=%d", rule.name(), paths, rule.methods().orElse(Set.of()),
                    rule.phase().configName(), rule.key(), rule.limit().map(Object::toString).orElse("-"),
                    rule.limitResolver().orElse("-"), rule.store(),
                    rule.maxKeys());
        }
        return memberships;
    }

    private static void logRules(List<RateLimitRuleConfig> rules) {
        if (rules.isEmpty()) {
            return;
        }
        int count = rules.size();
        Set<String> names = new TreeSet<>();
        for (RateLimitRuleConfig rule : rules) {
            names.add(rule.name());
        }
        LOG.infof("Rate limiting %d rule%s: %s", count, count == 1 ? "" : "s", String.join(", ", names));
    }

    private static Map<RateLimitPhase, PhaseIndex> indexPhases(List<RuleMembership> memberships) {
        Map<RateLimitPhase, PhaseIndex> phases = new EnumMap<>(RateLimitPhase.class);
        for (RateLimitPhase phase : RateLimitPhase.values()) {
            List<RuleMembership> ordered = new ArrayList<>();
            for (RuleMembership m : memberships) {
                if (m.evaluator().phase() == phase) {
                    ordered.add(m);
                }
            }
            ordered.sort(EVALUATION_ORDER);
            // the matcher returns matches in insertion order, which is therefore the evaluation order
            RateLimitPathMatcher.Builder<RuleMembership> matcher = RateLimitPathMatcher.builder();
            for (RuleMembership membership : ordered) {
                for (String path : membership.paths()) {
                    matcher.add(path, membership);
                }
            }
            List<RuleEvaluator> evaluators = new ArrayList<>(ordered.size());
            for (RuleMembership membership : ordered) {
                evaluators.add(membership.evaluator());
            }
            phases.put(phase, new PhaseIndex(Collections.unmodifiableList(evaluators), matcher.build()));
        }
        return phases;
    }

    /**
     * @param evaluator the evaluator of the rule
     * @param paths the paths of the rule
     * @param methods the upper-case HTTP methods the rule applies to; empty for all
     */
    public record RuleMembership(RuleEvaluator evaluator, List<String> paths, Set<String> methods) {

        /**
         * A rule that lists {@code GET} applies to {@code HEAD} too: the REST layer answers {@code HEAD} with the
         * {@code GET} resource method when there is no {@code HEAD} one.
         */
        public boolean appliesTo(String method) {
            return methods.isEmpty() || methods.contains(method)
                    || ("HEAD".equals(method) && methods.contains("GET"));
        }
    }

    /**
     * The rules of one phase.
     *
     * @param evaluators the evaluators, in evaluation order
     * @param matcher the matcher of their paths; its matches are in evaluation order
     */
    private record PhaseIndex(List<RuleEvaluator> evaluators, RateLimitPathMatcher<RuleMembership> matcher) {
    }
}
