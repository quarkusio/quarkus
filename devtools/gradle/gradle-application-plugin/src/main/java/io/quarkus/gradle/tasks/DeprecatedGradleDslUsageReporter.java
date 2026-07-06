package io.quarkus.gradle.tasks;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.gradle.api.logging.Logger;

final class DeprecatedGradleDslUsageReporter implements Serializable {
    private static final long serialVersionUID = 1L;

    static final String MIGRATION_GUIDE_URL = "https://quarkus.io/version/3.37/guides/gradle-tooling#quarkus-4-gradle-dsl-migration";
    static final String REPORT_PATH = "reports/quarkus/deprecated-gradle-dsl.txt";

    private final Map<String, Usage> usages = new LinkedHashMap<>();

    synchronized void record(String api, String replacement) {
        Usage usage = new Usage(api, replacement, Thread.currentThread().getStackTrace());
        usages.putIfAbsent(usage.key(), usage);
    }

    synchronized void report(Logger logger, File reportFile) {
        if (usages.isEmpty()) {
            return;
        }

        List<Usage> usageSnapshot = new ArrayList<>(usages.values());
        writeReport(reportFile, usageSnapshot);

        StringBuilder warning = new StringBuilder();
        warning.append(
                "Deprecated Quarkus Gradle DSL/API usage detected. The following APIs are deprecated for removal in Quarkus 4:");
        for (Usage usage : usageSnapshot) {
            warning.append(System.lineSeparator()).append("  - ").append(usage.api());
            warning.append(" (").append(usage.replacement()).append(")");
        }
        warning.append(System.lineSeparator()).append("Migration guide: ").append(MIGRATION_GUIDE_URL);
        warning.append(System.lineSeparator()).append("Call-site diagnostics: ").append(reportFile);
        logger.warn(warning.toString());
    }

    private static void writeReport(File reportFile, List<Usage> usages) {
        try {
            Files.createDirectories(reportFile.toPath().getParent());
            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(reportFile.toPath(), StandardCharsets.UTF_8))) {
                writer.println("Deprecated Quarkus Gradle DSL/API usage detected");
                writer.println();
                writer.println("The following APIs are deprecated for removal in Quarkus 4:");
                for (Usage usage : usages) {
                    writer.printf("- %s%n", usage.api());
                    writer.printf("  Replacement: %s%n", usage.replacement());
                }
                writer.println();
                writer.printf("Migration guide: %s%n", MIGRATION_GUIDE_URL);
                writer.println();
                writer.println("Call-site stack traces:");
                for (Usage usage : usages) {
                    writer.println();
                    writer.printf("%s%n", usage.api());
                    for (StackTraceElement frame : usage.stackTrace()) {
                        writer.printf("\tat %s%n", frame);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write deprecated Quarkus Gradle DSL diagnostics to " + reportFile, e);
        }
    }

    private record Usage(String api, String replacement, StackTraceElement[] stackTrace) implements Serializable {

        private static final long serialVersionUID = 1L;

        String key() {
            return api + '\n' + List.of(stackTrace);
        }
    }
}
