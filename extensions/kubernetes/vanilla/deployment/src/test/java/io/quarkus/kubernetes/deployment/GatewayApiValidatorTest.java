package io.quarkus.kubernetes.deployment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GatewayApiValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = { "api", "1", "my-rule", "v1.api", "a" })
    void validRuleNames(String ruleName) {
        assertDoesNotThrow(() -> GatewayApiValidator.validateRuleName(ruleName));
    }

    @ParameterizedTest
    @ValueSource(strings = { "Api", "my_rule", "-api", "api-", "v1..api", "" })
    void invalidRuleNames(String ruleName) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> GatewayApiValidator.validateRuleName(ruleName));
        assertTrue(ex.getMessage().contains("SectionName"));
    }

    @Test
    void ruleNameExceedingMaxLengthIsRejected() {
        String tooLong = "a".repeat(254);
        assertThrows(IllegalArgumentException.class, () -> GatewayApiValidator.validateRuleName(tooLong));
    }

    @ParameterizedTest
    @ValueSource(strings = { "0s", "180s", "500ms", "1h30m", "2m30s", "1h2m3s4ms" })
    void validTimeouts(String duration) {
        assertDoesNotThrow(() -> GatewayApiValidator.validateTimeouts(
                "quarkus.kubernetes.gateway.http.timeouts",
                timeouts(duration, null)));
        assertDoesNotThrow(() -> GatewayApiValidator.validateTimeouts(
                "quarkus.kubernetes.gateway.http.timeouts",
                timeouts(null, duration)));
    }

    @ParameterizedTest
    @ValueSource(strings = { "180", "3d", "-5s", "abc", "PT3M", " 10s", "" })
    void invalidTimeoutsFormat(String duration) {
        assertThrows(IllegalArgumentException.class, () -> GatewayApiValidator.validateTimeouts(
                "quarkus.kubernetes.gateway.http.timeouts",
                timeouts(duration, null)));
        assertThrows(IllegalArgumentException.class, () -> GatewayApiValidator.validateTimeouts(
                "quarkus.kubernetes.gateway.http.timeouts",
                timeouts(null, duration)));
    }

    @Test
    void backendRequestShorterOrEqualToRequestIsAllowed() {
        assertDoesNotThrow(() -> GatewayApiValidator.validateTimeouts(
                "quarkus.kubernetes.gateway.http.timeouts",
                timeouts("180s", "120s")));
        assertDoesNotThrow(() -> GatewayApiValidator.validateTimeouts(
                "quarkus.kubernetes.gateway.http.timeouts",
                timeouts("180s", "180s")));
        assertDoesNotThrow(() -> GatewayApiValidator.validateTimeouts(
                "quarkus.kubernetes.gateway.http.timeouts",
                timeouts("1m", "30s")));
    }

    @Test
    void disabledRequestTimeoutAllowsAnyValidBackendRequest() {
        assertDoesNotThrow(() -> GatewayApiValidator.validateTimeouts(
                "quarkus.kubernetes.gateway.http.timeouts",
                timeouts("0s", "30s")));
        assertDoesNotThrow(() -> GatewayApiValidator.validateTimeouts(
                "quarkus.kubernetes.gateway.http.timeouts",
                timeouts("0s", "0s")));
    }

    @Test
    void backendRequestLongerThanRequestIsRejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> GatewayApiValidator.validateTimeouts(
                        "quarkus.kubernetes.gateway.http.timeouts",
                        timeouts("30s", "60s")));
        assertTrue(ex.getMessage().contains("backend-request"));
    }

    @Test
    void disabledBackendRequestWithFiniteRequestIsRejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> GatewayApiValidator.validateTimeouts(
                        "quarkus.kubernetes.gateway.http.timeouts",
                        timeouts("30s", "0s")));
        assertTrue(ex.getMessage().contains("backend-request"));
    }

    private static GatewayConfig.Http.TimeoutsConfig timeouts(String request, String backendRequest) {
        return new GatewayConfig.Http.TimeoutsConfig() {
            @Override
            public Optional<String> request() {
                return Optional.ofNullable(request);
            }

            @Override
            public Optional<String> backendRequest() {
                return Optional.ofNullable(backendRequest);
            }
        };
    }
}
