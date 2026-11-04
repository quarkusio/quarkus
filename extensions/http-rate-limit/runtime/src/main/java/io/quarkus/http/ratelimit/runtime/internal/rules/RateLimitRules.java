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

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkus.http.ratelimit.RateLimitKeyResolver;
import io.quarkus.http.ratelimit.RateLimitPhase;
import io.quarkus.http.ratelimit.runtime.internal.config.DefaultRateLimitPolicy;
import io.quarkus.http.ratelimit.runtime.internal.config.HttpRateLimitBuildTimeConfig;
import io.quarkus.http.ratelimit.runtime.internal.config.HttpRateLimitConfigValidator;
import io.quarkus.http.ratelimit.runtime.internal.config.HttpRateLimitRuntimeConfig;
import io.quarkus.http.ratelimit.runtime.internal.config.RateLimitRuleConfig;
import io.quarkus.http.ratelimit.runtime.internal.keys.KeyResolvers;
import io.quarkus.http.ratelimit.runtime.internal.store.LocalRateLimitStore;
import io.quarkus.runtime.configuration.ConfigurationException;

/**
 * Turns the configuration into one {@link RuleEvaluator} per configured rule. Fails startup on an
 * invalid configuration.
 */
@Singleton
public class RateLimitRules {

    /**
     * Evaluation order: window ascending, ties by rule name, so that the narrowest rule refuses a request before a
     * wider one spends a permit on it.
     */
    private static final Comparator<RuleMembership> EVALUATION_ORDER = new Comparator<>() {
        @Override
        public int compare(RuleMembership a, RuleMembership b) {
            Duration windowA = a.evaluator().policy().window();
            Duration windowB = b.evaluator().policy().window();
            int byWindow = windowA.compareTo(windowB);
            if (byWindow != 0) {
                return byWindow;
            }
            return a.evaluator().rule().compareTo(b.evaluator().rule());
        }
    };

    private final Map<RateLimitPhase, PhaseIndex> phases;

    @Inject
    public RateLimitRules(HttpRateLimitBuildTimeConfig buildConfig,
            HttpRateLimitRuntimeConfig config,
            LocalRateLimitStore local,
            @Any Instance<RateLimitKeyResolver> resolvers) {
        if (!config.enabled()) {
            // a disabled extension validates nothing and builds no evaluators
            this.phases = indexPhases(List.of());
            return;
        }
        List<RateLimitRuleConfig> ruleConfigs = validate(buildConfig, config);
        List<RuleMembership> memberships = buildEvaluators(ruleConfigs, local, resolvers);
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

    private static List<RuleMembership> buildEvaluators(List<RateLimitRuleConfig> rules, LocalRateLimitStore local,
            Instance<RateLimitKeyResolver> resolvers) {
        List<RuleMembership> memberships = new ArrayList<>();
        for (RateLimitRuleConfig rule : rules) {
            DefaultRateLimitPolicy policy = DefaultRateLimitPolicy.of(rule);
            // builds the rule's cache now, so that the RateLimitLocalStoreCustomizers run at startup
            local.prepare(policy);
            RuleEvaluator evaluator = new RuleEvaluator(policy,
                    KeyResolvers.forKey(rule.key(), resolvers), local);
            Set<String> methods = Set.of();
            if (rule.methods().isPresent()) {
                Set<String> upperCase = new HashSet<>();
                for (String m : rule.methods().get()) {
                    upperCase.add(m.toUpperCase(Locale.ROOT));
                }
                methods = Set.copyOf(upperCase);
            }
            memberships.add(new RuleMembership(evaluator, rule.paths(), methods));
        }
        return memberships;
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
