package io.quarkus.test.common;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.Config;

import io.quarkus.runtime.configuration.QuarkusConfigFactory;

public final class TestConfigUtil {

    public static final long DEFAULT_WAIT_TIME_SECONDS = 60;

    private TestConfigUtil() {
    }

    public static List<String> argLineValues(String argLine) {
        if (argLine == null || argLine.isBlank()) {
            return List.of();
        }
        String[] parts = argLine.split("\\s+");
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

    public static String runTarget(Config config) {
        return config.getOptionalValue("quarkus.run.target", String.class)
                .orElse(null);
    }

    /**
     * Clean up config left over from badly-behaving previous tests.
     * <p>
     * Tests may call ConfigProvider.getConfig() directly or indirectly,
     * triggering lazy initialization in QuarkusConfigFactory#getConfigFor,
     * and if they don't call QuarkusConfigFactory.setConfig(null) afterward,
     * they may leak that config.
     * As it's quite hard to guarantee that all tests clean up after them,
     * especially if they don't use any Quarkus*Test extensions,
     * we call this cleanup here in Quarkus*Test extensions as an additional safeguard.
     * <p>
     * Note this must be called quite early in extensions in order to be effective:
     * things like TestHTTPResourceManager#getUri make use of config and may be called very early,
     * upon test instantiation, so cleaning up in beforeEach() won't cut it for example.
     */
    public static void cleanUp() {
        try {
            QuarkusConfigFactory.setConfig(null);
        } catch (Throwable ignored) {
        }
    }
}
