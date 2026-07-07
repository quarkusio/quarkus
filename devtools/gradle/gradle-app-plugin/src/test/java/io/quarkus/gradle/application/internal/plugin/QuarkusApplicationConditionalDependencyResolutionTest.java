package io.quarkus.gradle.application.internal.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;

import java.io.IOException;
import java.nio.file.Path;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

class QuarkusApplicationConditionalDependencyResolutionTest extends QuarkusApplicationModelResolutionTestSupport {

    @Test
    void unrelatedTaskDoesNotResolveConditionalDependencyConfigurations() throws IOException {
        writeFile(testProjectDir.resolve("settings.gradle"), "rootProject.name = 'conditional-laziness'\n");
        writeFile(testProjectDir.resolve("build.gradle"), """
                plugins {
                    id 'io.quarkus.application'
                }

                configurations.matching {
                    it.name == 'quarkusApplicationConditionalRuntimeClasspathConfiguration' ||
                            it.name == 'quarkusApplicationDevConditionalRuntimeClasspathConfiguration' ||
                            it.name == 'quarkusApplicationTestConditionalRuntimeClasspathConfiguration' ||
                            it.name == 'quarkusApplicationContinuousTestConditionalRuntimeClasspathConfiguration' ||
                            it.name == 'quarkusApplicationLocalConditionalDevDependenciesConfiguration' ||
                            it.name == 'quarkusApplicationContinuousTestLocalConditionalDevDependenciesConfiguration'
                }.configureEach {
                    incoming.beforeResolve {
                        throw new RuntimeException("${name} must not resolve for unrelated tasks")
                    }
                }

                tasks.register('unrelated') {
                    doLast {
                        println 'unrelated task ran'
                    }
                }
                """);

        BuildResult result = buildResultWithIsolatedProjects("unrelated");

        assertTaskOutcomes(result, SUCCESS, ":unrelated");
        assertThat(result.getOutput()).contains("unrelated task ran");
    }

    @Test
    void conditionalDependencyValueSourcesIgnoreLocalExtensionRuntimeJarDescriptors() throws IOException {
        writeLocalExtensionApplication("",
                """
                        tasks.named('extensionDescriptor') {
                            doLast {
                                def descriptor = extensionPropertiesFile.get().asFile
                                descriptor << '\\nconditional-dependencies=org.poison:should-not-resolve::jar:1.0'
                                descriptor << '\\nconditional-dev-dependencies=org.poison:should-not-resolve-dev::jar:1.0\\n'
                            }
                        }
                        """,
                "");

        BuildResult result = buildResultWithIsolatedProjects(":app:resolveRuntimeClasspath", ":app:resolveDevRuntimeClasspath");

        assertTaskOutcomes(result, SUCCESS,
                ":app:resolveRuntimeClasspath",
                ":app:resolveDevRuntimeClasspath");
        assertThat(result.getOutput())
                .contains("runtimeFile=runtime-ext-1.0.jar")
                .contains("devRuntimeFile=runtime-ext-1.0.jar")
                .doesNotContain("org.poison:should-not-resolve")
                .doesNotContain("org.poison:should-not-resolve-dev");
    }

    @Test
    void resolvesConditionalDependenciesDeclaredByLocalExtensionVariant() throws IOException {
        writeSyntheticConditionalExtensionRepository(testProjectDir.resolve("repo"));
        writeLocalExtensionApplication("""
                conditionalDependencies = [
                    'org.acme:satisfied-extension::jar:1.0',
                    'org.acme:missing-extension::jar:1.0'
                ]
                """, "", "implementation 'org.condition:present:1.0'");

        BuildResult result = buildResultWithIsolatedProjects(":app:resolveRuntimeClasspath", ":app:resolveDevRuntimeClasspath");

        assertTaskOutcomes(result, SUCCESS,
                ":app:resolveRuntimeClasspath",
                ":app:resolveDevRuntimeClasspath");
        assertThat(result.getOutput())
                .contains("Configuration cache entry stored")
                .contains("runtimeFile=satisfied-extension-1.0.jar")
                .contains("devRuntimeFile=satisfied-extension-1.0.jar")
                .doesNotContain("runtimeFile=missing-extension-1.0.jar")
                .doesNotContain("devRuntimeFile=missing-extension-1.0.jar");

        BuildResult cachedResult = buildResultWithIsolatedProjects(":app:resolveRuntimeClasspath",
                ":app:resolveDevRuntimeClasspath");
        assertTaskOutcomes(cachedResult, SUCCESS,
                ":app:resolveRuntimeClasspath",
                ":app:resolveDevRuntimeClasspath");
        assertThat(cachedResult.getOutput())
                .contains("Reusing configuration cache.")
                .contains("runtimeFile=satisfied-extension-1.0.jar")
                .contains("devRuntimeFile=satisfied-extension-1.0.jar");
    }

    @Test
    void resolvesConditionalDevDependenciesDeclaredByLocalExtensionVariantOnlyInDevMode() throws IOException {
        writeSyntheticConditionalDevExtensionRepository(testProjectDir.resolve("repo"));
        writeLocalExtensionApplication("""
                conditionalDevDependencies = ['org.acme:parent-extension-dev::jar:1.0']
                """);

        BuildResult result = buildResultWithIsolatedProjects(
                ":app:resolveRuntimeClasspath",
                ":app:resolveDevRuntimeClasspath",
                ":app:resolveContinuousTestRuntimeClasspath");

        assertTaskOutcomes(result, SUCCESS,
                ":app:resolveRuntimeClasspath",
                ":app:resolveDevRuntimeClasspath",
                ":app:resolveContinuousTestRuntimeClasspath");
        assertThat(result.getOutput())
                .doesNotContain("runtimeFile=parent-extension-dev-1.0.jar")
                .contains("devRuntimeFile=parent-extension-dev-1.0.jar")
                .contains("continuousTestRuntimeFile=parent-extension-dev-1.0.jar");
    }

    @Test
    void resolvesConditionSatisfiedRuntimeExtensionFromSyntheticDescriptors() throws IOException {
        writeSyntheticConditionalExtensionRepository(testProjectDir.resolve("repo"));
        writeFile(testProjectDir.resolve("settings.gradle"), "rootProject.name = 'conditional-resolution'\n");
        writeFile(testProjectDir.resolve("build.gradle"), """
                import org.gradle.api.DefaultTask
                import org.gradle.api.file.ConfigurableFileCollection
                import org.gradle.api.file.RegularFileProperty
                import org.gradle.api.tasks.Classpath
                import org.gradle.api.tasks.OutputFile
                import org.gradle.api.tasks.TaskAction

                plugins {
                    id 'io.quarkus.application'
                }

                repositories {
                    maven {
                        url = uri('repo')
                    }
                }

                dependencies {
                    implementation 'org.acme:parent-extension:1.0'
                    implementation 'org.condition:present:1.0'
                }

                abstract class WriteClasspath extends DefaultTask {
                    @Classpath
                    abstract ConfigurableFileCollection getClasspath()

                    @OutputFile
                    abstract RegularFileProperty getOutputFile()

                    @TaskAction
                    void write() {
                        outputFile.get().asFile.text = classpath.files*.name.sort().join('\\n') + '\\n'
                    }
                }

                tasks.register('writeRuntimeClasspath', WriteClasspath) {
                    classpath.from(configurations.named('quarkusApplicationRuntimeClasspathConfiguration'))
                    outputFile.set(layout.buildDirectory.file('resolved-runtime.txt'))
                }
                """);

        BuildResult result = buildResultWithIsolatedProjects("writeRuntimeClasspath");

        assertTaskOutcomes(result, SUCCESS, ":writeRuntimeClasspath");
        assertThat(testProjectDir.resolve("build/resolved-runtime.txt"))
                .content()
                .contains("parent-extension-1.0.jar")
                .contains("present-1.0.jar")
                .contains("satisfied-extension-1.0.jar")
                .doesNotContain("missing-extension-1.0.jar");
    }

    @Test
    void resolvesConditionalRuntimeExtensionSatisfiedByProjectDependencyRuntimeClasspath() throws IOException {
        writeSyntheticConditionalExtensionRepository(testProjectDir.resolve("repo"));
        writeFile(testProjectDir.resolve("settings.gradle"), """
                rootProject.name = 'conditional-project-runtime-resolution'
                include 'app', 'lib'
                """);
        writeFile(testProjectDir.resolve("lib/build.gradle"), """
                plugins {
                    id 'java-library'
                }

                repositories {
                    maven {
                        url = uri('../repo')
                    }
                }

                dependencies {
                    implementation 'org.condition:present:1.0'
                }
                """);
        writeFile(testProjectDir.resolve("app/build.gradle"), """
                import org.gradle.api.DefaultTask
                import org.gradle.api.file.ConfigurableFileCollection
                import org.gradle.api.file.RegularFileProperty
                import org.gradle.api.tasks.Classpath
                import org.gradle.api.tasks.OutputFile
                import org.gradle.api.tasks.TaskAction

                plugins {
                    id 'io.quarkus.application'
                }

                repositories {
                    maven {
                        url = uri('../repo')
                    }
                }

                dependencies {
                    implementation project(':lib')
                    implementation 'org.acme:parent-extension:1.0'
                }

                abstract class WriteClasspath extends DefaultTask {
                    @Classpath
                    abstract ConfigurableFileCollection getClasspath()

                    @OutputFile
                    abstract RegularFileProperty getOutputFile()

                    @TaskAction
                    void write() {
                        outputFile.get().asFile.text = classpath.files*.name.sort().join('\\n') + '\\n'
                    }
                }

                tasks.register('writeRuntimeClasspath', WriteClasspath) {
                    classpath.from(configurations.named('quarkusApplicationRuntimeClasspathConfiguration'))
                    outputFile.set(layout.buildDirectory.file('resolved-runtime.txt'))
                }

                tasks.register('writeDevRuntimeClasspath', WriteClasspath) {
                    classpath.from(configurations.named('quarkusApplicationDevRuntimeClasspathConfiguration'))
                    outputFile.set(layout.buildDirectory.file('resolved-dev-runtime.txt'))
                }
                """);

        BuildResult result = buildResultWithIsolatedProjects(":app:writeRuntimeClasspath", ":app:writeDevRuntimeClasspath");

        assertTaskOutcomes(result, SUCCESS,
                ":app:writeRuntimeClasspath",
                ":app:writeDevRuntimeClasspath");
        assertThat(testProjectDir.resolve("app/build/resolved-runtime.txt"))
                .content()
                .contains("parent-extension-1.0.jar")
                .contains("present-1.0.jar")
                .contains("satisfied-extension-1.0.jar")
                .doesNotContain("missing-extension-1.0.jar");
        assertThat(testProjectDir.resolve("app/build/resolved-dev-runtime.txt"))
                .content()
                .contains("parent-extension-1.0.jar")
                .contains("present-1.0.jar")
                .contains("satisfied-extension-1.0.jar")
                .doesNotContain("missing-extension-1.0.jar");
    }

    @Test
    void resolvesConditionalDevDependenciesOnlyInDevRuntimeClasspath() throws IOException {
        writeSyntheticConditionalDevExtensionRepository(testProjectDir.resolve("repo"));
        writeFile(testProjectDir.resolve("settings.gradle"), "rootProject.name = 'conditional-dev-resolution'\n");
        writeFile(testProjectDir.resolve("build.gradle"), """
                import org.gradle.api.DefaultTask
                import org.gradle.api.file.ConfigurableFileCollection
                import org.gradle.api.file.RegularFileProperty
                import org.gradle.api.tasks.Classpath
                import org.gradle.api.tasks.OutputFile
                import org.gradle.api.tasks.TaskAction

                plugins {
                    id 'io.quarkus.application'
                }

                repositories {
                    maven {
                        url = uri('repo')
                    }
                }

                dependencies {
                    implementation 'org.acme:parent-extension:1.0'
                }

                abstract class WriteClasspath extends DefaultTask {
                    @Classpath
                    abstract ConfigurableFileCollection getClasspath()

                    @OutputFile
                    abstract RegularFileProperty getOutputFile()

                    @TaskAction
                    void write() {
                        outputFile.get().asFile.text = classpath.files*.name.sort().join('\\n') + '\\n'
                    }
                }

                tasks.register('writeRuntimeClasspath', WriteClasspath) {
                    classpath.from(configurations.named('quarkusApplicationRuntimeClasspathConfiguration'))
                    outputFile.set(layout.buildDirectory.file('resolved-runtime.txt'))
                }

                tasks.register('writeDevRuntimeClasspath', WriteClasspath) {
                    classpath.from(configurations.named('quarkusApplicationDevRuntimeClasspathConfiguration'))
                    outputFile.set(layout.buildDirectory.file('resolved-dev-runtime.txt'))
                }
                """);

        BuildResult result = buildResultWithIsolatedProjects("writeRuntimeClasspath", "writeDevRuntimeClasspath");

        assertTaskOutcomes(result, SUCCESS, ":writeRuntimeClasspath", ":writeDevRuntimeClasspath");
        assertThat(testProjectDir.resolve("build/resolved-runtime.txt"))
                .content()
                .contains("parent-extension-1.0.jar")
                .doesNotContain("parent-extension-dev-1.0.jar");
        assertThat(testProjectDir.resolve("build/resolved-dev-runtime.txt"))
                .content()
                .contains("parent-extension-1.0.jar")
                .contains("parent-extension-dev-1.0.jar");
    }

    private static void writeSyntheticConditionalDevExtensionRepository(Path repository) throws IOException {
        writeMavenArtifact(repository, "org.acme", "parent-extension", "1.0",
                """
                        conditional-dev-dependencies=org.acme\\:parent-extension-dev\\:\\:jar\\:1.0
                        deployment-artifact=org.acme\\:parent-extension-deployment\\:1.0
                        """);
        writeMavenArtifact(repository, "org.acme", "parent-extension-dev", "1.0", null);
    }
}
