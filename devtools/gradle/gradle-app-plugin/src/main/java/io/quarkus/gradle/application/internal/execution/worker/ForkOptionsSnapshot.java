package io.quarkus.gradle.application.internal.execution.worker;

import java.util.List;
import java.util.Map;

import org.gradle.process.JavaForkOptions;

import io.quarkus.gradle.application.dsl.QuarkusApplicationForkOptions;

public record ForkOptionsSnapshot(
        List<String> jvmArgs,
        Map<String, String> systemProperties,
        Map<String, String> environment,
        String minHeapSize,
        String maxHeapSize,
        boolean enableAssertions,
        boolean debug,
        String defaultCharacterEncoding) {

    public static ForkOptionsSnapshot from(QuarkusApplicationForkOptions options) {
        return new ForkOptionsSnapshot(
                List.copyOf(options.getJvmArgs().get()),
                Map.copyOf(options.getSystemProperties().get()),
                Map.copyOf(options.getEnvironment().get()),
                options.getMinHeapSize().getOrNull(),
                options.getMaxHeapSize().getOrNull(),
                options.getEnableAssertions().get(),
                options.getDebug().get(),
                options.getDefaultCharacterEncoding().getOrNull());
    }

    public void applyTo(JavaForkOptions options) {
        options.jvmArgs(jvmArgs);
        options.systemProperties(systemProperties);
        options.environment(environment);
        if (minHeapSize != null) {
            options.setMinHeapSize(minHeapSize);
        }
        if (maxHeapSize != null) {
            options.setMaxHeapSize(maxHeapSize);
        }
        options.setEnableAssertions(enableAssertions);
        options.setDebug(debug);
        if (defaultCharacterEncoding != null) {
            options.setDefaultCharacterEncoding(defaultCharacterEncoding);
        }
    }
}
