package io.quarkus.deployment.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.TestNativeConfig;

class NativeImageReflectConfigStepTest {

    private static final String CLASS_NAME = "org.acme.Foo";

    @Test
    void publicMethodsRegistersAllPublicMethodsNotAllDeclaredMethods() {
        String json = generateReflectConfigJson(List.of(
                ReflectiveClassBuildItem.builder(CLASS_NAME).constructors(false).publicMethods().build()));

        assertThat(json).contains("\"allPublicMethods\":true");
        assertThat(json).doesNotContain("allDeclaredMethods");
    }

    @Test
    void registeringSameClassWithMethodsAndPublicMethodsMergesBothFlags() {
        // Simulates two independent build steps registering the same class differently - the resulting
        // reflect-config.json entry must be the union of both, not just the last one applied.
        String json = generateReflectConfigJson(List.of(
                ReflectiveClassBuildItem.builder(CLASS_NAME).constructors(false).publicMethods().build(),
                ReflectiveClassBuildItem.builder(CLASS_NAME).constructors(false).methods().build()));

        assertThat(json).contains("\"allPublicMethods\":true");
        assertThat(json).contains("\"allDeclaredMethods\":true");
    }

    private static String generateReflectConfigJson(List<ReflectiveClassBuildItem> reflectiveClassBuildItems) {
        NativeImageReflectConfigStep step = new NativeImageReflectConfigStep();
        List<GeneratedResourceBuildItem> produced = new ArrayList<>();

        step.generateReflectConfig(produced::add, new TestNativeConfig("mandrel"),
                List.of(), List.of(),
                reflectiveClassBuildItems,
                List.of(), List.of(), List.of());

        assertThat(produced).hasSize(1);
        assertThat(produced.get(0).getName()).isEqualTo("META-INF/native-image/reflect-config.json");
        return new String(produced.get(0).getData(), StandardCharsets.UTF_8).replaceAll("\\s+", "");
    }
}
