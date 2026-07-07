package io.quarkus.gradle.application.internal.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;

import java.io.IOException;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

class QuarkusApplicationLocalOutputResolutionTest extends QuarkusApplicationModelResolutionTestSupport {

    @Test
    void deploymentClasspathUsesLocalExtensionDeploymentVariantWithIsolatedProjects() throws IOException {
        writeLocalExtensionApplication();

        BuildResult result = buildResultWithIsolatedProjects(":app:resolveDeploymentClasspath");

        assertTaskOutcomes(result, SUCCESS, ":deployment-ext:jar", ":app:resolveDeploymentClasspath");
        assertThat(result.getOutput())
                .contains("deploymentFile=deployment-ext-1.0.jar")
                .doesNotContain("Could not find org.acme:runtime-ext-deployment:1.0");
    }

    @Test
    void includedBuildExtensionVariantsSupplyDeploymentAndConditionalDependencies() throws IOException {
        writeIncludedBuildExtensionApplication();

        BuildResult result = buildResultWithIsolatedProjects(
                ":app:resolveRuntimeClasspath",
                ":app:resolveDevRuntimeClasspath",
                ":app:resolveDeploymentClasspath");

        assertTaskOutcomes(result, SUCCESS,
                ":app:resolveRuntimeClasspath",
                ":app:resolveDevRuntimeClasspath",
                ":app:resolveDeploymentClasspath");
        assertThat(result.getOutput())
                .contains("runtimeFile=satisfied-extension-1.0.jar")
                .doesNotContain("runtimeFile=missing-extension-1.0.jar")
                .doesNotContain("runtimeFile=parent-extension-dev-1.0.jar")
                .contains("devRuntimeFile=satisfied-extension-1.0.jar")
                .contains("devRuntimeFile=parent-extension-dev-1.0.jar")
                .doesNotContain("devRuntimeFile=missing-extension-1.0.jar")
                .contains("deploymentFile=deployment-ext-1.0.jar")
                .doesNotContain("Could not find org.acme:runtime-ext-deployment:1.0");

        BuildResult cachedResult = buildResultWithIsolatedProjects(
                ":app:resolveRuntimeClasspath",
                ":app:resolveDevRuntimeClasspath",
                ":app:resolveDeploymentClasspath");
        assertThat(cachedResult.getOutput()).contains("Configuration cache entry reused.");
        assertTaskOutcomes(cachedResult, SUCCESS,
                ":app:resolveRuntimeClasspath",
                ":app:resolveDevRuntimeClasspath",
                ":app:resolveDeploymentClasspath");
    }

    @Test
    void reportsOutputOnlyWorkspaceModulesForPlainProjectDependencies() throws IOException {
        writePlainProjectDependencyApplication();

        BuildResult result = buildResultWithIsolatedProjects(
                ":app:quarkusApplicationShowModel",
                ":app:quarkusApplicationShowDevModel",
                ":app:quarkusApplicationShowTestModel");

        assertTaskOutcomes(result, SUCCESS,
                ":lib:compileJava",
                ":lib:processResources",
                ":app:quarkusApplicationModel",
                ":app:quarkusApplicationDevModel",
                ":app:quarkusApplicationTestModel",
                ":app:quarkusApplicationShowModel",
                ":app:quarkusApplicationShowDevModel",
                ":app:quarkusApplicationShowTestModel");
        assertProjectDependencyReport("quarkus-application-model.txt", false);
        assertProjectDependencyReport("quarkus-application-dev-model.txt", true);
        assertProjectDependencyReport("quarkus-application-test-model.txt", true);

        BuildResult cachedResult = buildResultWithIsolatedProjects(
                ":app:quarkusApplicationShowModel",
                ":app:quarkusApplicationShowDevModel",
                ":app:quarkusApplicationShowTestModel");
        assertThat(cachedResult.getOutput()).contains("Configuration cache entry reused.");
        assertTaskOutcomes(cachedResult, SUCCESS,
                ":app:quarkusApplicationShowModel",
                ":app:quarkusApplicationShowDevModel",
                ":app:quarkusApplicationShowTestModel");
    }

    @Test
    void reportsOutputOnlyWorkspaceModulesForIncludedBuildDependencies() throws IOException {
        writeIncludedBuildProjectDependencyApplication();

        BuildResult result = buildResultWithIsolatedProjects("quarkusApplicationShowDevModel");

        assertTaskOutcomes(result, SUCCESS,
                ":quarkusApplicationDevModel",
                ":quarkusApplicationShowDevModel");
        assertThat(testProjectDir.resolve(
                "build/reports/quarkus/application-model/quarkus-application-dev-model.txt"))
                .content()
                .contains("org.acme:library::jar:1.0")
                .contains("<project>/library-build/build/classes/java/main")
                .contains("<project>/library-build/build/resources/main")
                .contains("workspace-module=org.acme:library:1.0")
                .contains("module-directory=<none>")
                .contains("flags=direct, runtime-cp, deployment-cp, workspace-module, reloadable");

        BuildResult cachedResult = buildResultWithIsolatedProjects("quarkusApplicationShowDevModel");
        assertThat(cachedResult.getOutput()).contains("Configuration cache entry reused.");
        assertTaskOutcomes(cachedResult, SUCCESS, ":quarkusApplicationShowDevModel");
    }

    @Test
    void deploymentClasspathIgnoresLocalExtensionRuntimeJarDeploymentDescriptor() throws IOException {
        writeLocalExtensionApplication("deploymentArtifact = 'org.poison:wrong-deployment:1.0'\n");

        BuildResult result = buildResultWithIsolatedProjects(":app:resolveDeploymentClasspath");

        assertTaskOutcomes(result, SUCCESS, ":deployment-ext:jar", ":app:resolveDeploymentClasspath");
        assertThat(result.getOutput())
                .contains("deploymentFile=deployment-ext-1.0.jar")
                .doesNotContain("org.poison:wrong-deployment");
    }

    private void writePlainProjectDependencyApplication() throws IOException {
        writeFile(testProjectDir.resolve("settings.gradle"), """
                rootProject.name = 'plain-project-model'
                include 'app', 'lib'
                """);
        writeFile(testProjectDir.resolve("app/build.gradle"), """
                plugins {
                    id 'io.quarkus.application'
                }

                group = 'org.acme'
                version = '1.0'

                dependencies {
                    implementation project(':lib')
                }
                """);
        writeFile(testProjectDir.resolve("app/src/main/java/org/acme/App.java"), """
                package org.acme;

                public final class App {
                }
                """);
        writeFile(testProjectDir.resolve("lib/build.gradle"), """
                plugins {
                    id 'java-library'
                }

                group = 'org.acme'
                version = '1.0'
                """);
        writeFile(testProjectDir.resolve("lib/src/main/java/org/acme/Library.java"), """
                package org.acme;

                public final class Library {
                }
                """);
        writeFile(testProjectDir.resolve("lib/src/main/resources/library.properties"), "library=true\n");
    }

    private void writeIncludedBuildExtensionApplication() throws IOException {
        writeSyntheticConditionalExtensionRepository(testProjectDir.resolve("repo"));
        writeMavenArtifact(testProjectDir.resolve("repo"), "org.acme", "satisfied-extension-deployment", "1.0", null);
        writeMavenArtifact(testProjectDir.resolve("repo"), "org.acme", "parent-extension-dev", "1.0", null);
        writeFile(testProjectDir.resolve("settings.gradle"), """
                rootProject.name = 'included-extension-application'
                include 'app'
                includeBuild 'extension-build'
                """);
        writeFile(testProjectDir.resolve("app/build.gradle"), """
                plugins {
                    id 'io.quarkus.application'
                }

                group = 'org.acme'
                version = '1.0'

                repositories {
                    maven {
                        url = uri('../repo')
                    }
                }

                dependencies {
                    implementation 'org.acme:runtime-ext:1.0'
                    implementation 'org.condition:present:1.0'
                }

                tasks.register('resolveDeploymentClasspath') {
                    def deploymentClasspath = configurations.named('quarkusApplicationDeploymentClasspathConfiguration')
                    inputs.files(deploymentClasspath)
                    doLast {
                        deploymentClasspath.get().files*.name.sort().each { println "deploymentFile=${it}" }
                    }
                }

                tasks.register('resolveRuntimeClasspath') {
                    def runtimeClasspath = configurations.named('quarkusApplicationRuntimeClasspathConfiguration')
                    inputs.files(runtimeClasspath)
                    doLast {
                        runtimeClasspath.get().files*.name.sort().each { println "runtimeFile=${it}" }
                    }
                }

                tasks.register('resolveDevRuntimeClasspath') {
                    def runtimeClasspath = configurations.named('quarkusApplicationDevRuntimeClasspathConfiguration')
                    inputs.files(runtimeClasspath)
                    doLast {
                        runtimeClasspath.get().files*.name.sort().each { println "devRuntimeFile=${it}" }
                    }
                }
                """);
        writeFile(testProjectDir.resolve("extension-build/settings.gradle"), """
                rootProject.name = 'extension-build'
                include 'runtime-ext', 'deployment-ext'
                """);
        writeFile(testProjectDir.resolve("extension-build/runtime-ext/build.gradle"), """
                plugins {
                    id 'java'
                    id 'io.quarkus.extension'
                }

                group = 'org.acme'
                version = '1.0'

                quarkusExtension {
                    disableValidation = true
                    deploymentModule = 'deployment-ext'
                    conditionalDependencies = [
                        'org.acme:satisfied-extension::jar:1.0',
                        'org.acme:missing-extension::jar:1.0'
                    ]
                    conditionalDevDependencies = ['org.acme:parent-extension-dev::jar:1.0']
                }
                """);
        writeFile(testProjectDir.resolve("extension-build/runtime-ext/src/main/java/org/acme/runtime/RuntimeExtension.java"),
                """
                        package org.acme.runtime;

                        public final class RuntimeExtension {
                        }
                        """);
        writeFile(testProjectDir.resolve("extension-build/deployment-ext/build.gradle"), """
                plugins {
                    id 'java-library'
                }

                group = 'org.acme'
                version = '1.0'
                """);
        writeFile(testProjectDir.resolve(
                "extension-build/deployment-ext/src/main/java/org/acme/deployment/DeploymentExtension.java"), """
                        package org.acme.deployment;

                        public final class DeploymentExtension {
                        }
                        """);
    }

    private void writeIncludedBuildProjectDependencyApplication() throws IOException {
        writeFile(testProjectDir.resolve("settings.gradle"), """
                rootProject.name = 'included-library-application'
                includeBuild 'library-build'
                """);
        writeFile(testProjectDir.resolve("build.gradle"), """
                plugins {
                    id 'io.quarkus.application'
                }

                group = 'org.acme'
                version = '1.0'

                dependencies {
                    implementation 'org.acme:library:1.0'
                }
                """);
        writeFile(testProjectDir.resolve("src/main/java/org/acme/App.java"), """
                package org.acme;

                public final class App {
                }
                """);
        writeFile(testProjectDir.resolve("library-build/settings.gradle"), "rootProject.name = 'library'\n");
        writeFile(testProjectDir.resolve("library-build/build.gradle"), """
                plugins {
                    id 'java-library'
                }

                group = 'org.acme'
                version = '1.0'
                """);
        writeFile(testProjectDir.resolve("library-build/src/main/java/org/acme/Library.java"), """
                package org.acme;

                public final class Library {
                }
                """);
        writeFile(testProjectDir.resolve("library-build/src/main/resources/library.properties"), "library=true\n");
    }

    private void assertProjectDependencyReport(String reportFileName, boolean reloadable) {
        assertThat(testProjectDir.resolve("app/build/reports/quarkus/application-model/" + reportFileName))
                .content()
                .contains("org.acme:lib::jar:1.0")
                .contains("<build>/lib/build/classes/java/main")
                .contains("<build>/lib/build/resources/main")
                .contains("workspace-module=org.acme:lib:1.0")
                .contains("module-directory=<none>")
                .contains("build-directory=<none>")
                .satisfies(content -> {
                    if (reloadable) {
                        assertThat(content)
                                .contains("flags=direct, runtime-cp, deployment-cp, workspace-module, reloadable")
                                .contains("Reloadable workspace dependencies:\n"
                                        + "    org.acme:app::jar\n"
                                        + "    org.acme:lib::jar");
                    } else {
                        assertThat(content)
                                .contains("flags=direct, runtime-cp, deployment-cp, workspace-module")
                                .doesNotContain("org.acme:lib::jar\nImported platform BOMs:");
                    }
                });
    }
}
