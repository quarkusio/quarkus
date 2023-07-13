package io.quarkus.test.junit.launcher;

import static io.quarkus.test.junit.IntegrationTestUtil.DEFAULT_WAIT_TIME_SECONDS;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.Config;

import io.smallrye.config.SmallRyeConfig;

public final class ConfigUtil {

    private ConfigUtil() {
    }

    public static List<String> argLineValue(Config config) {
        String strValue = config.getOptionalValue("quarkus.test.arg-line", String.class)
                .orElse(config.getOptionalValue("quarkus.test.argLine", String.class) // legacy value
                        .orElse(null));
        if (strValue == null) {
            return Collections.emptyList();
        }
        String[] parts = strValue.split("\\s+");
        List<String> result = new ArrayList<>(parts.length);
        for (String s : parts) {
            String trimmed = s.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            result.add(trimmed);
        }
        return result;
    }

    public static Map<String, String> env(Config config) {
        return ((SmallRyeConfig) config).getOptionalValues("quarkus.test.env", String.class, String.class)
                .orElse(Collections.emptyMap());
    }

    public static Duration waitTimeValue(Config config) {
        return config.getOptionalValue("quarkus.test.wait-time", Duration.class)
                .orElseGet(() -> config.getOptionalValue("quarkus.test.jar-wait-time", Duration.class) // legacy value
                        .orElseGet(() -> Duration.ofSeconds(DEFAULT_WAIT_TIME_SECONDS)));
    }

    public static String integrationTestProfile(Config config) {
        return config.getOptionalValue("quarkus.test.integration-test-profile", String.class)
                .orElseGet(() -> config.getOptionalValue("quarkus.test.native-image-profile", String.class)
                        .orElse(null));
    }

    public static String runTarget(Config config) {
        return config.getOptionalValue("quarkus.run.target", String.class)
                .orElse(null);
    }
}
