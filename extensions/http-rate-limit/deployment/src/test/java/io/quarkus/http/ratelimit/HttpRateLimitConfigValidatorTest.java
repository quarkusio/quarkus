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
 * the validator is covered by {@link LeaseWithLocalStoreFailsStartupTest}.
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

    private static List<String> warnings(String... overrides) {
        return HttpRateLimitConfigValidator.warnings(List.of(rule(overrides)));
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
        assertThat(defaults.store()).isEqualTo("local");
        assertThat(defaults.maxKeys()).isEqualTo(100_000);
        assertThat(defaults.leaseSize()).isEmpty();
        assertThat(defaults.maxOverdraft()).isZero();

        RateLimitRuleConfig global = rule(g("phase"), null, g("request-key"), null, P + "phase", "post-authentication",
                P + "request-key", "principal", P + "store", "redis", P + "max-keys", "7");
        assertThat(global.phase()).isEqualTo(RateLimitPhase.POST_AUTHENTICATION);
        assertThat(global.key()).isEqualTo("principal");
        assertThat(global.store()).isEqualTo("redis");
        assertThat(global.maxKeys()).isEqualTo(7);

        RateLimitRuleConfig own = rule(P + "phase", "post-authentication", P + "request-key", "principal",
                P + "store", "redis", P + "max-keys", "7",
                g("store"), "custom", g("max-keys"), "8",
                g("lease-size"), "3", g("max-overdraft"), "0");
        assertThat(own.phase()).isEqualTo(RateLimitPhase.PRE_AUTHENTICATION);
        assertThat(own.key()).isEqualTo("ip");
        assertThat(own.store()).isEqualTo("custom");
        assertThat(own.maxKeys()).isEqualTo(8);
        assertThat(own.leaseSize()).contains(3L);
        assertThat(own.maxOverdraft()).isZero();
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
    void leaseRequiresNonLocalStore() {
        assertThat(validate(g("lease-size"), "2")).containsExactly(
                "Rate limit rule 'api' lease-size requires a non-local store");
        assertThat(validate(g("lease-size"), "2", g("store"), "redis")).isEmpty();
        assertThat(validate(g("lease-size"), "2", P + "store", "redis")).isEmpty();
    }

    @Test
    void maxOverdraftRequiresLeaseSize() {
        assertThat(validate(g("max-overdraft"), "1", g("store"), "redis")).containsExactly(
                "Rate limit rule 'api' max-overdraft requires lease-size");
    }

    @Test
    void leaseSizeMustFitBurst() {
        assertThat(validate(g("store"), "redis", g("lease-size"), "11"))
                .containsExactly("Rate limit rule 'api' lease-size must be between 1 and the burst (10)");
        assertThat(validate(g("store"), "redis", g("lease-size"), "0"))
                .containsExactly("Rate limit rule 'api' lease-size must be between 1 and the burst (10)");
        assertThat(validate(g("store"), "redis", g("lease-size"), "4", g("burst"), "3"))
                .containsExactly("Rate limit rule 'api' lease-size must be between 1 and the burst (3)");
    }

    @Test
    void maxOverdraftMustFitBurst() {
        assertThat(validate(g("store"), "redis", g("lease-size"), "2", g("max-overdraft"), "11"))
                .containsExactly("Rate limit rule 'api' max-overdraft must be between 0 and the burst (10)");
        assertThat(validate(g("store"), "redis", g("lease-size"), "2", g("max-overdraft"), "-1"))
                .containsExactly("Rate limit rule 'api' max-overdraft must be between 0 and the burst (10)");
    }

    @Test
    void largeLeaseSizeWarns() {
        assertThat(validate(g("store"), "redis", g("permits"), "100", g("lease-size"), "6")).isEmpty();
        assertThat(warnings(g("store"), "redis", g("permits"), "100", g("lease-size"), "6")).containsExactly(
                "Rate limit rule 'api' lease-size × replicas may strand a large share of permits (lease-size <= permits / 20 is recommended)");
        assertThat(warnings(g("store"), "redis", g("permits"), "100", g("lease-size"), "5")).isEmpty();
    }

    @Test
    void storeSettings() {
        assertThat(validate(P + "store-timeout", "0s")).containsExactly(
                "quarkus.http.rate-limit.store-timeout must be positive");
        assertThat(validate(P + "redis.timeout", "0s")).containsExactly(
                "quarkus.http.rate-limit.redis.timeout must be positive");
        assertThat(validate(P + "redis.timeout", "1s", P + "redis.failure-mode", "open")).isEmpty();
    }

    @Test
    void warningIntervalMustBeAtLeastOneSecond() {
        assertThat(validate("quarkus.http.rate-limit.warning-interval", "500ms"))
                .containsExactly("quarkus.http.rate-limit.warning-interval must be at least 1 second");
        assertThat(validate("quarkus.http.rate-limit.warning-interval", "1s")).isEmpty();
    }
}
