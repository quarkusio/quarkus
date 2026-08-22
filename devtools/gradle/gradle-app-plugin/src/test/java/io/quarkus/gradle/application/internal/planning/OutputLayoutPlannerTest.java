package io.quarkus.gradle.application.internal.planning;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.quarkus.gradle.application.model.QuarkusApplicationBuildDescriptor;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;

class OutputLayoutPlannerTest {

    private final OutputLayoutPlanner outputLayoutPlanner = new OutputLayoutPlanner();
    private final CompatibilityMaterializationPlanner materializationPlanner = new CompatibilityMaterializationPlanner();

    @Test
    void usesNamedOutputRootAndReusableDependencyFragmentWhereCompatible() {
        var layout = outputLayoutPlanner.plan(Path.of("build"),
                QuarkusApplicationBuildDescriptor.of("app", QuarkusApplicationBuildType.FAST_JAR));

        assertThat(layout.rootDirectory()).isEqualTo(Path.of("build/quarkus-builds/app/package"));
        assertThat(layout.generatedDirectory()).isEqualTo(Path.of("build/quarkus-builds/app/package/gen"));
        assertThat(layout.appDirectory()).isEqualTo(Path.of("build/quarkus-builds/app/package/app"));
        assertThat(layout.dependencyFragmentDirectory()).contains(Path.of("build/quarkus-build/dep"));
    }

    @Test
    void omitsReusableDependencyFragmentForShapesThatNeedDifferentLayout() {
        var layout = outputLayoutPlanner.plan(Path.of("build"),
                QuarkusApplicationBuildDescriptor.of("uber", QuarkusApplicationBuildType.UBER_JAR));

        assertThat(layout.dependencyFragmentDirectory()).isEmpty();
    }

    @Test
    void usesExplicitOperationOutputRoot() {
        var layout = outputLayoutPlanner.plan(Path.of("build"),
                QuarkusApplicationBuildDescriptor.of("app", QuarkusApplicationBuildType.FAST_JAR),
                Path.of("build/quarkus-builds/app/image-build"));

        assertThat(layout.rootDirectory()).isEqualTo(Path.of("build/quarkus-builds/app/image-build"));
        assertThat(layout.generatedDirectory()).isEqualTo(Path.of("build/quarkus-builds/app/image-build/gen"));
        assertThat(layout.appDirectory()).isEqualTo(Path.of("build/quarkus-builds/app/image-build/app"));
        assertThat(layout.dependencyFragmentDirectory()).contains(Path.of("build/quarkus-build/dep"));
    }

    @Test
    void plansLegacyMaterializationSeparately() {
        var fastJar = materializationPlanner.plan(Path.of("build"),
                QuarkusApplicationBuildDescriptor.of("app", QuarkusApplicationBuildType.FAST_JAR));
        var legacyJar = materializationPlanner.plan(Path.of("build"),
                QuarkusApplicationBuildDescriptor.of("legacy", QuarkusApplicationBuildType.LEGACY_JAR));
        var nativeOutput = materializationPlanner.plan(Path.of("build"),
                QuarkusApplicationBuildDescriptor.of("native1", QuarkusApplicationBuildType.NATIVE_EXECUTABLE));

        assertThat(fastJar.legacyOutputPaths()).containsExactly(Path.of("build/quarkus-app"));
        assertThat(legacyJar.legacyOutputPaths()).containsExactly(Path.of("build/lib"));
        assertThat(nativeOutput.legacyOutputPaths()).containsExactly(Path.of("build/native-sources"));
    }
}
