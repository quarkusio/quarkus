package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.http.ratelimit.deployment.HttpRateLimitBuildConfigValidator;
import io.quarkus.http.ratelimit.runtime.internal.config.HttpRateLimitBuildTimeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * Runs the build time validator against configurations built with SmallRye Config. It is a plain unit test, like the
 * validator it tests, which is a deployment class. The build step that runs it is covered by
 * {@link InvalidConfigurationFailsBuildTest}.
 */
public class HttpRateLimitBuildConfigValidatorTest {

    private static final String P = "quarkus.http.rate-limit.";
    private static final Set<String> STORES = Set.of("custom");
    private static final Set<String> KEY_RESOLVERS = Set.of("ip", "global", "principal", "tenant");
    private static final Set<String> OVERRIDES = Set.of("plans");

    /** A valid pre-authentication rule named {@code api} with the given overrides applied on top. */
    private static HttpRateLimitBuildTimeConfig config(String... overrides) {
        Map<String, String> values = new HashMap<>();
        values.put(P + "rules.api.paths", "/api/*");
        values.put(P + "rules.api.phase", "pre-authentication");
        values.put(P + "rules.api.request-key", "ip");
        for (int i = 0; i < overrides.length; i += 2) {
            if (overrides[i + 1] == null) {
                values.remove(overrides[i]);
            } else {
                values.put(overrides[i], overrides[i + 1]);
            }
        }
        return new SmallRyeConfigBuilder().addDefaultInterceptors().withMapping(HttpRateLimitBuildTimeConfig.class)
                .withDefaultValues(values).build().getConfigMapping(HttpRateLimitBuildTimeConfig.class);
    }

    private static List<String> validate(String... overrides) {
        return HttpRateLimitBuildConfigValidator.validate(config(overrides), STORES, KEY_RESOLVERS, OVERRIDES);
    }

    private static String g(String key) {
        return P + "rules.api." + key;
    }

    @Test
    void validConfigurationHasNoErrors() {
        assertThat(validate()).isEmpty();
    }

    @Test
    void postAuthenticationRuleIsValid() {
        assertThat(validate(g("phase"), "post-authentication", g("request-key"), "principal")).isEmpty();
    }

    @Test
    void principalRequestKeyNotAvailableBeforeAuthentication() {
        assertThat(validate(g("request-key"), "principal")).containsExactly(
                "Rate limit rule 'api' request-key 'principal' is not available before authentication; use phase=post-authentication");
    }

    @Test
    void phaseAndRequestKeyDefaultToTheGlobalOnes() {
        assertThat(validate(g("phase"), null, g("request-key"), null)).containsExactly(
                "Rate limit rule 'api' has no phase: set quarkus.http.rate-limit.rules.api.phase or quarkus.http.rate-limit.phase",
                "Rate limit rule 'api' has no request-key: set quarkus.http.rate-limit.rules.api.request-key or quarkus.http.rate-limit.request-key");
        assertThat(
                validate(g("phase"), null, g("request-key"), null, P + "phase", "pre-authentication", P + "request-key", "ip"))
                .isEmpty();
        // the global settings are checked like the rule ones
        assertThat(validate(g("phase"), null, g("request-key"), null, P + "phase", "pre-authentication", P + "request-key",
                "principal")).containsExactly(
                        "Rate limit rule 'api' request-key 'principal' is not available before authentication; use phase=post-authentication");
        // the rule settings win
        assertThat(validate(P + "phase", "post-authentication", P + "request-key", "principal")).isEmpty();
    }

    @Test
    void requestKeyMustMatchAResolver() {
        assertThat(validate(g("request-key"), "custom:unknown")).containsExactly(
                "Rate limit rule 'api' request-key 'custom:unknown' matches no RateLimitKeyResolver bean annotated @Identifier(\"unknown\")");
        assertThat(validate(g("request-key"), "custom:tenant")).isEmpty();
        assertThat(validate(g("request-key"), "header:X-Client")).isEmpty();
        // a bare name is a built-in key: an application's resolver has a namespace of its own, so that a later
        // built-in key cannot take its place
        assertThat(validate(g("request-key"), "tenant")).containsExactly(
                "Rate limit rule 'api' request-key 'tenant' is not a built-in key (ip, global, principal), header:<name> or custom:<identifier>; for the RateLimitKeyResolver bean annotated @Identifier(\"tenant\"), use custom:tenant");
        assertThat(validate(g("request-key"), "header:")).containsExactly(
                "Rate limit rule 'api' request-key 'header:' is not a built-in key (ip, global, principal), header:<name> or custom:<identifier>");
        assertThat(validate(g("request-key"), "custom:")).containsExactly(
                "Rate limit rule 'api' request-key 'custom:' is not a built-in key (ip, global, principal), header:<name> or custom:<identifier>");
    }

    @Test
    void headerNameMustBeAnHttpToken() {
        for (String name : List.of("X Tenant", "X:Tenant", "X/Tenant")) {
            assertThat(validate(g("request-key"), "header:" + name))
                    .containsExactly("quarkus.http.rate-limit.rules.api.request-key: header name '" + name
                            + "' is not an HTTP field name; use header:<name>, for example header:X-Tenant");
        }
        assertThat(validate(g("request-key"), "header:x-tenant_id")).isEmpty();
    }

    @Test
    void overrideMustExist() {
        String unknown = "Rate limit rule 'api' limit-resolver 'nope' matches no RateLimitResolver bean annotated @Identifier(\"nope\")";
        assertThat(validate(g("limit-resolver"), "nope")).containsExactly(unknown);
        assertThat(validate(P + "limit-resolver", "nope")).containsExactly(unknown);
        assertThat(validate(g("limit-resolver"), "plans")).isEmpty();
        assertThat(validate(P + "limit-resolver", "plans")).isEmpty();
        // the rule override wins over the global one
        assertThat(validate(P + "limit-resolver", "nope", g("limit-resolver"), "plans")).isEmpty();
    }

    @Test
    void storeMustExist() {
        String unknown = "Rate limit rule 'api' store 'nope' matches no RateLimitStore bean annotated @Identifier(\"nope\")";
        assertThat(validate(g("store"), "nope")).containsExactly(unknown);
        assertThat(validate(P + "store", "nope")).containsExactly(unknown);
        assertThat(validate(g("store"), "custom")).isEmpty();
        assertThat(validate(P + "store", "custom")).isEmpty();
        // the rule store wins over the global one
        assertThat(validate(P + "store", "nope", g("store"), "local")).isEmpty();
    }

    @Test
    void wildcardMustBeWholeSegment() {
        assertThat(validate(g("paths"), "/api/v*/x")).containsExactly(
                "Rate limit rule 'api' path '/api/v*/x': a wildcard must be a whole path segment ('/a/*/b' or a trailing '/*'), not part of one");
        assertThat(validate(g("paths"), "/a/*/b,/a/*")).isEmpty();
    }

    @Test
    void methodMustBeToken() {
        assertThat(validate(g("methods"), "POST,BAD METHOD")).containsExactly(
                "Rate limit rule 'api' method 'BAD METHOD' is not an HTTP method token");
        assertThat(validate(g("methods"), "post,PATCH")).isEmpty();
    }

}
