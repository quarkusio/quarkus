package io.quarkus.gradle.application.internal.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.FROM_CACHE;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE;

import java.io.IOException;
import java.nio.file.Path;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import io.quarkus.gradle.testing.BaseGradleTest;

class QuarkusApplicationNativeTestCompilationFunctionalTest extends BaseGradleTest {

    @Test
    void compilesNamedNativeTestAgainstSharedGeneratedAndExplicitlyIncludedTestSources() throws IOException {
        writeApplication();

        BuildResult firstResult = buildResultWithIsolatedProjects(
                "compileQuarkusParityNativeTestJava", BUILD_CACHE);

        assertTaskOutcomes(firstResult, SUCCESS, ":quarkusApplicationGenerateTestCode");
        assertThat(firstResult.task(":compileIntegrationTestJava").getOutcome()).isIn(SUCCESS, FROM_CACHE);
        assertThat(firstResult.task(":compileQuarkusParityNativeTestJava").getOutcome()).isIn(SUCCESS, FROM_CACHE);
        assertThat(testProjectDir.resolve(Path.of(
                "build", "classes", "java", "quarkusParityNativeTest",
                "org", "acme", "generated", "GeneratedTest.class"))).isRegularFile();
        assertThat(testProjectDir.resolve(Path.of(
                "build", "classes", "java", "quarkusParityNativeTest",
                "org", "acme", "NamedNativeTestCompilation.class"))).isRegularFile();
        assertThat(testProjectDir.resolve(Path.of(
                "build", "classes", "java", "integrationTest",
                "org", "acme", "included", "IncludedTestSupport.class"))).isRegularFile();

        BuildResult secondResult = buildResultWithIsolatedProjects(
                "compileQuarkusParityNativeTestJava", BUILD_CACHE);

        assertConfigurationCacheReused(secondResult);
        assertTaskOutcomes(secondResult, UP_TO_DATE,
                ":quarkusApplicationGenerateTestCode",
                ":compileIntegrationTestJava",
                ":compileQuarkusParityNativeTestJava");
    }

    private void writeApplication() throws IOException {
        writeFile("settings.gradle", "rootProject.name = 'named-native-test-compilation'\n");
        writeFile("gradle.properties", "version = 1.0\n");
        writeFile("build.gradle", """
                import java.nio.file.Files

                import org.gradle.api.plugins.jvm.JvmTestSuite

                plugins {
                    id 'io.quarkus.application'
                }

                repositories {
                    mavenCentral()
                }

                def integrationTest = testing.suites.register('integrationTest', JvmTestSuite)

                quarkusApplication {
                    codegen {
                        providers = ['custom-test']
                    }
                    builds {
                        nativeExecutable('parity')
                    }
                }

                testing.suites.named('quarkusParityNativeTest', JvmTestSuite) {
                    includeTestsFrom integrationTest
                }

                tasks.named('quarkusApplicationGenerateTestCode') {
                    doLast {
                        def sourcePackage = generatedOutputDirectory.get().asFile.toPath()
                                .resolve('custom-test/org/acme/generated')
                        Files.createDirectories(sourcePackage)
                        Files.writeString(sourcePackage.resolve('GeneratedTest.java'), '''
                            package org.acme.generated;

                            public final class GeneratedTest {
                                public static String value() {
                                    return "generated";
                                }
                            }
                            '''.stripIndent())
                    }
                }
                """);
        writeFile("src/main/java/org/acme/Application.java", """
                package org.acme;

                public final class Application {
                }
                """);
        writeFile("src/integrationTest/java/org/acme/included/IncludedTestSupport.java", """
                package org.acme.included;

                public final class IncludedTestSupport {

                    private IncludedTestSupport() {
                    }

                    public static String value() {
                        return "-included";
                    }
                }
                """);
        writeFile("src/quarkusParityNativeTest/java/org/acme/NamedNativeTestCompilation.java", """
                package org.acme;

                import org.acme.generated.GeneratedTest;
                import org.acme.included.IncludedTestSupport;

                final class NamedNativeTestCompilation {

                    String value() {
                        return GeneratedTest.value() + IncludedTestSupport.value();
                    }
                }
                """);
    }
}
