package io.quarkus.gradle.application.internal.config;

import java.util.Map;
import java.util.TreeMap;

import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;

public final class EffectiveConfigDiagnosticsFormatter {

    private EffectiveConfigDiagnosticsFormatter() {
    }

    public static String format(String taskPath, String buildName, QuarkusApplicationBuildType buildType, String profile,
            EffectiveConfigPlan plan, boolean showValues) {
        StringBuilder diagnostics = new StringBuilder();
        diagnostics.append("Effective Quarkus configuration for named build '")
                .append(escape(buildName))
                .append("' (")
                .append(buildType)
                .append(", profile '")
                .append(escape(profile))
                .append("'):");
        appendDiagnostics(diagnostics, plan, showValues);
        if (plan.externallyProvidedValuesOmitted() > 0) {
            diagnostics.append("\n    <")
                    .append(plan.externallyProvidedValuesOmitted())
                    .append(" system-property/environment ")
                    .append(plan.externallyProvidedValuesOmitted() == 1 ? "winner" : "winners")
                    .append(" omitted>");
        }

        diagnostics.append(showValues ? "\nBuild-owned forced values:" : "\nBuild-owned forced keys:");
        appendEntries(diagnostics, plan.descriptorShapeValues(), showValues);

        diagnostics.append("\nConfiguration sources:");
        if (plan.configSourceNames().isEmpty()) {
            diagnostics.append("\n    <none>");
        } else {
            for (String sourceName : plan.configSourceNames()) {
                diagnostics.append("\n    ").append(escape(sourceName));
            }
        }
        if (showValues) {
            diagnostics.append(
                    "\nSystem-property and environment winners remain omitted to avoid capturing ambient values.");
        } else {
            diagnostics.append("\nValues are hidden. To include captured non-ambient values, run:\n    ./gradlew ")
                    .append(escape(taskPath))
                    .append(" --show-values");
        }
        return diagnostics.toString();
    }

    private static void appendDiagnostics(StringBuilder target, EffectiveConfigPlan plan, boolean showValues) {
        if (plan.diagnostics().isEmpty()) {
            target.append("\n    <none>");
            return;
        }
        Map<String, EffectiveConfigDiagnostic> entries = new TreeMap<>();
        for (EffectiveConfigDiagnostic entry : plan.diagnostics()) {
            entries.putIfAbsent(entry.key(), entry);
        }
        entries.forEach((key, entry) -> {
            target.append("\n    ").append(escape(key));
            if (showValues) {
                target.append('=').append(escape(entry.value()));
            }
            target.append("    source=").append(escape(entry.source()));
        });
    }

    private static void appendEntries(StringBuilder target, Map<String, String> entries, boolean showValues) {
        if (entries.isEmpty()) {
            target.append("\n    <none>");
            return;
        }
        new TreeMap<>(entries).forEach((key, value) -> {
            target.append("\n    ").append(escape(key));
            if (showValues) {
                target.append('=').append(escape(value));
            }
        });
    }

    static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\\' -> escaped.append("\\\\");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (Character.isISOControl(character)) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
