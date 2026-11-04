package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.http.ratelimit.runtime.internal.config.HttpRateLimitBuildTimeConfig;
import io.quarkus.http.ratelimit.runtime.internal.config.HttpRateLimitConfigValidator;
import io.quarkus.http.ratelimit.runtime.internal.config.HttpRateLimitRuntimeConfig;
import io.quarkus.http.ratelimit.runtime.internal.config.RateLimitRuleConfig;
import io.quarkus.runtime.configuration.DurationConverter;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * Runs the startup validator against configurations built with SmallRye Config, inside a Quarkus application so that the
 * runtime module needs no bytecode library to implement the {@code @ConfigMapping} in unit tests. The startup wiring of
 * the validator is covered by {@link RuleWithoutLimitFailsStartupTest}.
 */
public class HttpRateLimitConfigValidatorTest {

    @RegisterExtension
    static final QuarkusExtensionTest app = new QuarkusExtensionTest();

    private static final String P = "quarkus.http.rate-limit.";

    /**
     * A valid pre-authentication rule named {@code api} with the given overrides applied on top; a {@code null} value
     * removes the key.
     */
    private static SmallRyeConfig config(String... overrides) {
        Map<String, String> values = new HashMap<>();
        values.put(P + "api.paths", "/api/*");
        values.put(P + "api.phase", "pre-authentication");
        values.put(P + "api.request-key", "ip");
        values.put(P + "api.permits", "10");
        values.put(P + "api.window", "1m");
        for (int i = 0; i < overrides.length; i += 2) {
            if (overrides[i + 1] == null) {
                values.remove(overrides[i]);
            } else {
                values.put(overrides[i], overrides[i + 1]);
            }
        }
        return new SmallRyeConfigBuilder().withConverter(Duration.class, 100, new DurationConverter())
                .withMapping(HttpRateLimitBuildTimeConfig.class)
                .withMapping(HttpRateLimitRuntimeConfig.class)
                .withDefaultValues(values).build();
    }

    private static List<String> validate(String... overrides) {
        SmallRyeConfig config = config(overrides);
        return HttpRateLimitConfigValidator.validate(config.getConfigMapping(HttpRateLimitBuildTimeConfig.class),
                config.getConfigMapping(HttpRateLimitRuntimeConfig.class));
    }

    private static RateLimitRuleConfig rule(String... overrides) {
        SmallRyeConfig config = config(overrides);
        return RateLimitRuleConfig.of("api", config.getConfigMapping(HttpRateLimitBuildTimeConfig.class),
                config.getConfigMapping(HttpRateLimitRuntimeConfig.class));
    }

    private static String g(String key) {
        return P + "api." + key;
    }

    @Test
    void validConfigurationHasNoErrors() {
        assertThat(validate()).isEmpty();
    }

    @Test
    void ruleWithoutLimit() {
        assertThat(validate(g("permits"), null, g("window"), null)).containsExactly(
                "Rate limit rule 'api' has no limit: set quarkus.http.rate-limit.api.permits and quarkus.http.rate-limit.api.window");
    }

    @Test
    void ruleNotDefinedAtBuildTime() {
        assertThat(validate(P + "typo.permits", "5", P + "typo.window", "1m")).containsExactly(
                "Rate limit rule 'typo' is not defined at build time: its paths (quarkus.http.rate-limit.typo.paths) must be set when the application is built");
    }

    @Test
    void globalDefaultsApplyUnlessTheRuleSetsItsOwn() {
        RateLimitRuleConfig defaults = rule();
        assertThat(defaults.maxKeys()).isEqualTo(100_000);

        RateLimitRuleConfig global = rule(g("phase"), null, g("request-key"), null, P + "phase", "post-authentication",
                P + "request-key", "principal", P + "max-keys", "7");
        assertThat(global.phase()).isEqualTo(RateLimitPhase.POST_AUTHENTICATION);
        assertThat(global.key()).isEqualTo("principal");
        assertThat(global.maxKeys()).isEqualTo(7);

        RateLimitRuleConfig own = rule(P + "phase", "post-authentication", P + "request-key", "principal",
                P + "max-keys", "7", g("max-keys"), "8");
        assertThat(own.phase()).isEqualTo(RateLimitPhase.PRE_AUTHENTICATION);
        assertThat(own.key()).isEqualTo("ip");
        assertThat(own.maxKeys()).isEqualTo(8);
    }

    @Test
    void permitsMustBePositive() {
        assertThat(validate(g("permits"), "0")).containsExactly(
                "Rate limit rule 'api' permits must be at least 1");
    }

    @Test
    void windowMustBePositive() {
        assertThat(validate(g("window"), "0s")).containsExactly(
                "Rate limit rule 'api' window must be positive");
    }

    @Test
    void burstMustBePositive() {
        assertThat(validate(g("burst"), "0")).containsExactly(
                "Rate limit rule 'api' burst must be at least 1");
    }

    @Test
    void maxKeysMustBePositive() {
        assertThat(validate(g("max-keys"), "0")).containsExactly(
                "Rate limit rule 'api' max-keys must be at least 1");
        assertThat(validate(P + "max-keys", "0")).containsExactly(
                "Rate limit rule 'api' max-keys must be at least 1");
        assertThat(validate(P + "max-keys", "0", g("max-keys"), "1")).isEmpty();
    }

    @Test
    void windowPerPermitBelowOneMicrosecond() {
        assertThat(validate(g("permits"), "2000000", g("window"), "1s"))
                .containsExactly(
                        "Rate limit rule 'api' (pre-authentication): window / permits must be at least 1 microsecond");
    }

    @Test
    void policyOverflowMessageIsSurfacedVerbatim() {
        assertThat(validate(g("window"), "30d", g("permits"), "1",
                g("burst"), "2")).containsExactly(
                        "Rate limit rule 'api' (pre-authentication): (window / permits) * burst must not exceed 30 days");
    }

    @Test
    void warningIntervalMustBeAtLeastOneSecond() {
        assertThat(validate("quarkus.http.rate-limit.warning-interval", "500ms"))
                .containsExactly("quarkus.http.rate-limit.warning-interval must be at least 1 second");
        assertThat(validate("quarkus.http.rate-limit.warning-interval", "1s")).isEmpty();
    }
}
