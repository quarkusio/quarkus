package io.quarkus.kubernetes.deployment;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class GatewayApiValidator {

    private static final int SECTION_NAME_MAX_LENGTH = 253;
    private static final Pattern SECTION_NAME_PATTERN = Pattern
            .compile("^[a-z0-9]([-a-z0-9]*[a-z0-9])?(\\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)*$");
    private static final Pattern DURATION_PATTERN = Pattern.compile("^([0-9]{1,5}(h|m|s|ms)){1,4}$");
    private static final Pattern DURATION_SEGMENT_PATTERN = Pattern.compile("([0-9]{1,5})(ms|h|m|s)");

    private GatewayApiValidator() {
    }

    static void validateRuleName(String ruleName) {
        if (ruleName == null || ruleName.isEmpty() || ruleName.length() > SECTION_NAME_MAX_LENGTH
                || !SECTION_NAME_PATTERN.matcher(ruleName).matches()) {
            throw new IllegalArgumentException(
                    "Invalid HTTPRoute rule id '" + ruleName + "' in 'quarkus.kubernetes.gateway.http.rules." + ruleName
                            + "': rule ids are used as Gateway API rule names (SectionName) and must consist of lower case "
                            + "alphanumeric characters, '-' or '.', and must start and end with an alphanumeric character.");
        }
    }

    static void validateTimeouts(String propertyPrefix, GatewayConfig.Http.TimeoutsConfig timeouts) {
        Optional<String> request = timeouts.request();
        Optional<String> backendRequest = timeouts.backendRequest();

        request.ifPresent(value -> validateDuration(propertyPrefix + ".request", value));
        backendRequest.ifPresent(value -> validateDuration(propertyPrefix + ".backend-request", value));

        if (request.isPresent() && backendRequest.isPresent()) {
            long requestMillis = parseDurationMillis(request.get());
            long backendRequestMillis = parseDurationMillis(backendRequest.get());
            if (requestMillis > 0 && (backendRequestMillis == 0 || backendRequestMillis > requestMillis)) {
                throw new IllegalArgumentException(
                        "Invalid HTTPRoute timeouts under '" + propertyPrefix + "': backend-request timeout ('"
                                + backendRequest.get() + "') cannot be longer than request timeout ('"
                                + request.get() + "').");
            }
        }
    }

    private static void validateDuration(String propertyName, String value) {
        if (value == null || !DURATION_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Invalid Gateway API duration '" + value + "' for '" + propertyName
                            + "': expected a duration matching ^([0-9]{1,5}(h|m|s|ms)){1,4}$ (for example, '180s', '500ms', or '0s').");
        }
    }

    static long parseDurationMillis(String duration) {
        long totalMillis = 0;
        Matcher matcher = DURATION_SEGMENT_PATTERN.matcher(duration);
        while (matcher.find()) {
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            totalMillis += switch (unit) {
                case "h" -> value * 3_600_000L;
                case "m" -> value * 60_000L;
                case "s" -> value * 1_000L;
                case "ms" -> value;
                default -> throw new IllegalStateException("Unexpected duration unit: " + unit);
            };
        }
        return totalMillis;
    }
}
