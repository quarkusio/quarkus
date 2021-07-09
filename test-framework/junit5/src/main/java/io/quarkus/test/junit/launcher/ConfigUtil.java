package io.quarkus.test.junit.launcher;

import static io.quarkus.test.junit.IntegrationTestUtil.DEFAULT_WAIT_TIME_SECONDS;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalLong;

import org.eclipse.microprofile.config.Config;

public final class ConfigUtil {

    private ConfigUtil() {
    }

    public static List<String> argLineValue(Config config) {
        List<String> strings = config.getOptionalValues("quarkus.test.arg-line", String.class)
                .orElse(config.getOptionalValues("quarkus.test.argLine", String.class) // legacy value
                        .orElse(Collections.emptyList()));
        if (strings.isEmpty()) {
            return strings;
        }
        List<String> sanitizedString = new ArrayList<>(strings.size());
        for (String s : strings) {
            String trimmed = s.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            sanitizedString.add(trimmed);
        }
        return sanitizedString;
    }

    public static Duration waitTimeValue(Config config) {
        return Duration.ofSeconds(config.getValue("quarkus.test.wait-time", OptionalLong.class)
                .orElse(config.getValue("quarkus.test.jar-wait-time", OptionalLong.class) // legacy value
                        .orElse(DEFAULT_WAIT_TIME_SECONDS)));
    }
}
