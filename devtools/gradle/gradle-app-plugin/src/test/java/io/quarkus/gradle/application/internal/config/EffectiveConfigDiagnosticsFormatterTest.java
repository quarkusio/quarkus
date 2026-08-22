package io.quarkus.gradle.application.internal.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;

class EffectiveConfigDiagnosticsFormatterTest {

    @Test
    void defaultOutputNeverRendersValues() {
        var plan = plan("value-canary", "forced-canary");

        String diagnostics = EffectiveConfigDiagnosticsFormatter.format(
                ":quarkusFastShowEffectiveConfig",
                "fast",
                QuarkusApplicationBuildType.FAST_JAR,
                "prod",
                plan,
                false);

        assertThat(diagnostics)
                .contains("quarkus.example    source=application.properties")
                .contains("Build-owned forced keys:")
                .contains("quarkus.package.jar.type")
                .contains("./gradlew :quarkusFastShowEffectiveConfig --show-values")
                .doesNotContain("value-canary", "forced-canary", "quarkus.example=");
    }

    @Test
    void explicitOutputEscapesControlCharactersAndReportsOmittedAmbientValues() {
        var plan = new EffectiveConfigPlan(
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of("quarkus.package.jar.type", "fast\njar"),
                List.of(new EffectiveConfigDiagnostic(
                        "quarkus.example",
                        "line1\nline2\r\t\\\0",
                        "source\nlabel",
                        250,
                        false)),
                2,
                List.of("source\nlabel"));

        String diagnostics = EffectiveConfigDiagnosticsFormatter.format(
                ":quarkusFastShowEffectiveConfig",
                "fast",
                QuarkusApplicationBuildType.FAST_JAR,
                "prod",
                plan,
                true);

        assertThat(diagnostics)
                .contains("quarkus.example=line1\\nline2\\r\\t\\\\\\u0000    source=source\\nlabel")
                .contains("quarkus.package.jar.type=fast\\njar")
                .contains("<2 system-property/environment winners omitted>")
                .doesNotContain("line1\nline2", "source\nlabel");
    }

    private static EffectiveConfigPlan plan(String value, String forcedValue) {
        return new EffectiveConfigPlan(
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of("quarkus.package.jar.type", forcedValue),
                List.of(new EffectiveConfigDiagnostic(
                        "quarkus.example",
                        value,
                        "application.properties",
                        250,
                        false)),
                0,
                List.of("application.properties"));
    }
}
