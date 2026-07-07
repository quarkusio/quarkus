package io.quarkus.gradle.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class QuarkusApplicationContinuousTestingParityTest extends ContinuousBuildTestSupport {

    @Test
    void observesResourceChangesAndPreservesCompileOnlyDependencies() throws Exception {
        var project = new ContinuousTestingJavaParityProject(testProjectDir);
        project.writeApplication();

        Path receipt = testProjectDir.resolve(Path.of("build", "quarkus-continuous-test", "iteration.properties"));
        Path sourceMarker = testProjectDir.resolve("build/parity/source.txt");
        Path mainResourceMarker = testProjectDir.resolve("build/parity/main-resource.txt");
        Path testResourceMarker = testProjectDir.resolve("build/parity/test-resource.txt");
        Path closeReceipt = testProjectDir.resolve("build/quarkus-continuous-test/session-closed.txt");

        try (var build = startContinuousBuild("quarkusApplicationContinuousTest", "--no-quarkus-debug")) {
            build.await("initial resource and compile-only baseline", BUILD_START_TIMEOUT,
                    () -> fileContains(receipt, "sessionReady=true")
                            && fileContains(sourceMarker, "initial-source")
                            && fileContains(mainResourceMarker, "initial-main-resource")
                            && fileContains(testResourceMarker, "initial-test-resource"));

            project.writeGreetingData("changed-source");
            build.await("Lombok-processed source change", RELOAD_TIMEOUT,
                    () -> fileContains(sourceMarker, "changed-source"));

            project.writeApplicationProperties("changed-main-resource");
            build.await("main resource change", RELOAD_TIMEOUT,
                    () -> fileContains(mainResourceMarker, "changed-main-resource"));

            project.writeTestResource("changed-test-resource");
            build.await("test resource change", RELOAD_TIMEOUT,
                    () -> fileContains(testResourceMarker, "changed-test-resource"));
        }

        assertThat(closeReceipt).hasContent("closed\n");
    }

    @Test
    void observesKotlinKaptAndKspGeneratedTestChanges() throws Exception {
        var project = new ContinuousTestingKotlinParityProject(testProjectDir);
        project.writeApplication();
        project.writeTest("initial-kotlin", "initial-kapt", "initial-ksp");

        Path receipt = testProjectDir.resolve(Path.of("build", "quarkus-continuous-test", "iteration.properties"));
        Path kotlinMarker = testProjectDir.resolve("build/parity/kotlin.txt");
        Path kaptMarker = testProjectDir.resolve("build/parity/kapt.txt");
        Path kspMarker = testProjectDir.resolve("build/parity/ksp.txt");
        Path closeReceipt = testProjectDir.resolve("build/quarkus-continuous-test/session-closed.txt");

        try (var build = startContinuousBuild("quarkusApplicationContinuousTest", "--no-quarkus-debug")) {
            build.await("initial Kotlin, KAPT, and KSP baseline", BUILD_START_TIMEOUT,
                    () -> fileContains(receipt, "sessionReady=true")
                            && fileContains(kotlinMarker, "initial-kotlin")
                            && fileContains(kaptMarker, "initial-kapt")
                            && fileContains(kspMarker, "initial-ksp"));

            project.writeTest("changed-kotlin", "initial-kapt", "initial-ksp");
            build.await("Kotlin test change", RELOAD_TIMEOUT,
                    () -> fileContains(kotlinMarker, "changed-kotlin"));

            project.writeTest("changed-kotlin", "changed-kapt", "initial-ksp");
            build.await("KAPT-generated test change", RELOAD_TIMEOUT,
                    () -> fileContains(kaptMarker, "changed-kapt"));

            project.writeTest("changed-kotlin", "changed-kapt", "changed-ksp");
            build.await("KSP-generated test change", RELOAD_TIMEOUT,
                    () -> fileContains(kspMarker, "changed-ksp"));
        }

        assertThat(closeReceipt).hasContent("closed\n");
    }
}
