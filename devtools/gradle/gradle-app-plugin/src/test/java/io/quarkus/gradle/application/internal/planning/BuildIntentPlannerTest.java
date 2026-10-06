package io.quarkus.gradle.application.internal.planning;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.gradle.application.model.QuarkusApplicationBuildDescriptor;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;
import io.quarkus.gradle.application.model.QuarkusApplicationImageBuilder;
import io.quarkus.gradle.application.model.QuarkusApplicationImageDescriptor;

class BuildIntentPlannerTest {

    private final BuildIntentPlanner planner = new BuildIntentPlanner();

    @Test
    void normalPackageDoesNotAddImageIntent() {
        var intent = planner.packageIntent(
                QuarkusApplicationBuildDescriptor.of("app", QuarkusApplicationBuildType.FAST_JAR),
                Map.of("quarkus.foo", "bar"),
                Map.of("quarkus.native.builder-image", "builder"));

        assertThat(intent.forcedProperties()).containsEntry("quarkus.foo", "bar")
                .containsEntry("quarkus.native.builder-image", "builder")
                .doesNotContainKeys("quarkus.container-image.build", "quarkus.container-image.push");
    }

    @Test
    void nativeSourcesAddsNativeIntent() {
        var intent = planner.packageIntent(
                QuarkusApplicationBuildDescriptor.of("sources", QuarkusApplicationBuildType.NATIVE_SOURCES),
                Map.of(),
                Map.of());

        assertThat(intent.forcedProperties()).containsEntry("quarkus.native.enabled", "true")
                .containsEntry("quarkus.native.sources-only", "true");
    }

    @Test
    void imageBuildAndPushIntentAreTaskSelected() {
        var image = new QuarkusApplicationImageDescriptor("quay.io/acme/app", "1.0",
                QuarkusApplicationImageBuilder.JIB);

        assertThat(planner.imageBuildIntent(image).forcedProperties())
                .containsEntry("quarkus.container-image.build", "true")
                .containsEntry("quarkus.container-image.builder", "jib")
                .doesNotContainKey("quarkus.container-image.push");
        assertThat(planner.imagePushIntent(image).forcedProperties())
                .containsEntry("quarkus.container-image.build", "true")
                .containsEntry("quarkus.container-image.push", "true")
                .containsEntry("quarkus.container-image.builder", "jib");
    }

    @Test
    void imageIntentOmitsAbsentBuilder() {
        var image = new QuarkusApplicationImageDescriptor("quay.io/acme/app", "1.0", null);

        assertThat(planner.imageBuildIntent(image).forcedProperties())
                .containsEntry("quarkus.container-image.build", "true")
                .doesNotContainKey("quarkus.container-image.builder");
    }
}
