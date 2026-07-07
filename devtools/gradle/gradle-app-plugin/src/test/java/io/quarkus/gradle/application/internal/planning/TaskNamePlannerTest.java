package io.quarkus.gradle.application.internal.planning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.gradle.application.model.QuarkusApplicationBuildDescriptor;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;
import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentDescriptor;
import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentTarget;
import io.quarkus.gradle.application.model.QuarkusApplicationLaunchDescriptor;

class TaskNamePlannerTest {

    private final TaskNamePlanner planner = new TaskNamePlanner();

    @Test
    void derivesNamesFromRegisteredBuildName() {
        var names = planner.taskNames(QuarkusApplicationBuildDescriptor.of("archive1",
                QuarkusApplicationBuildType.AOT_JAR));

        assertThat(names.build()).isEqualTo("quarkusArchive1Build");
        assertThat(names.showEffectiveConfig()).isEqualTo("quarkusArchive1ShowEffectiveConfig");
        assertThat(names.run()).isEqualTo("quarkusArchive1Run");
        assertThat(names.imageBuild()).isEqualTo("quarkusArchive1ImageBuild");
        assertThat(names.imagePush()).isEqualTo("quarkusArchive1ImagePush");
        assertThat(names.startupArchiveValidation()).isEqualTo("quarkusArchive1StartupArchiveValidation");
        assertThat(names.startupOptimizedImageBuild()).isEqualTo("quarkusArchive1StartupOptimizedImageBuild");
        assertThat(names.startupOptimizedImagePush()).isEqualTo("quarkusArchive1StartupOptimizedImagePush");
        assertThat(names.nativeTest()).isEqualTo("quarkusArchive1NativeTest");
    }

    @Test
    void derivesDeployAndContinuousTestNames() {
        var build = QuarkusApplicationBuildDescriptor.of("app", QuarkusApplicationBuildType.FAST_JAR);
        var deployment = QuarkusApplicationDeploymentDescriptor.of("dev", QuarkusApplicationDeploymentTarget.KUBERNETES);

        assertThat(planner.deployTaskName(build, deployment)).isEqualTo("quarkusAppDeployToDev");
        assertThat(planner.continuousTestTaskName(QuarkusApplicationLaunchDescriptor.continuousTest()))
                .isEqualTo("quarkusContinuousTest");
        assertThat(planner.continuousTestTaskName(QuarkusApplicationLaunchDescriptor.continuousTest("dev")))
                .isEqualTo("quarkusDevContinuousTest");
    }

    @Test
    void rejectsNormalizedBuildNameCollisions() {
        assertThatThrownBy(() -> planner.validateBuildNames(List.of(
                QuarkusApplicationBuildDescriptor.of("native-main", QuarkusApplicationBuildType.FAST_JAR),
                QuarkusApplicationBuildDescriptor.of("nativeMain", QuarkusApplicationBuildType.NATIVE_EXECUTABLE))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("native-main")
                .hasMessageContaining("nativeMain");
    }

    @Test
    void rejectsTaskNameCollisionsWithExistingTasks() {
        assertThatThrownBy(() -> planner.validateTaskNameCollisions(
                List.of(QuarkusApplicationBuildDescriptor.of("app", QuarkusApplicationBuildType.FAST_JAR)),
                List.of("quarkusAppBuild")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quarkusAppBuild");
    }

    @Test
    void ignoresNativeRunTaskNameCollisionsBecauseNativeBuildsDoNotRegisterRunTasks() {
        planner.validateTaskNameCollisions(
                List.of(QuarkusApplicationBuildDescriptor.of("native", QuarkusApplicationBuildType.NATIVE_EXECUTABLE)),
                List.of("quarkusNativeRun"));
    }

    @Test
    void onlyNativeExecutableBuildsReserveNativeTestTaskNames() {
        String nativeTestTaskName = "quarkusApplicationNativeTest";

        assertThatThrownBy(() -> planner.validateTaskNameCollisions(
                List.of(QuarkusApplicationBuildDescriptor.of(
                        "application", QuarkusApplicationBuildType.NATIVE_EXECUTABLE)),
                List.of(nativeTestTaskName)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("application")
                .hasMessageContaining(nativeTestTaskName);

        EnumSet<QuarkusApplicationBuildType> buildTypesWithoutNativeTests = EnumSet
                .allOf(QuarkusApplicationBuildType.class);
        buildTypesWithoutNativeTests.remove(QuarkusApplicationBuildType.NATIVE_EXECUTABLE);
        for (QuarkusApplicationBuildType buildType : buildTypesWithoutNativeTests) {
            assertThatCode(() -> planner.validateTaskNameCollisions(
                    List.of(QuarkusApplicationBuildDescriptor.of("application", buildType)),
                    List.of(nativeTestTaskName)))
                    .as("%s must not reserve a named native-test task", buildType)
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void rejectsDeploymentNameCollisionsWithinBuild() {
        assertThatThrownBy(() -> planner.validateDeploymentNames(List.of(
                QuarkusApplicationDeploymentDescriptor.of("prod-main", QuarkusApplicationDeploymentTarget.KUBERNETES),
                QuarkusApplicationDeploymentDescriptor.of("prodMain", QuarkusApplicationDeploymentTarget.OPENSHIFT))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("prod-main")
                .hasMessageContaining("prodMain");
    }
}
