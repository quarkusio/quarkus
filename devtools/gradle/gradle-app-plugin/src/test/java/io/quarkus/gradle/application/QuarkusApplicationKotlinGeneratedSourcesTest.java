package io.quarkus.gradle.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.FROM_CACHE;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;

import java.io.IOException;
import java.nio.file.Path;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.gradle.testing.BaseGradleTest;

class QuarkusApplicationKotlinGeneratedSourcesTest extends BaseGradleTest {

    @Test
    void compilesKotlinMainAndTestAgainstGeneratedSourcesWhenKotlinIsAppliedBeforeApplicationPlugin() throws IOException {
        writeKotlinApplication(true, false);

        BuildResult result = buildResultWithIsolatedProjects("compileTestKotlin");

        assertGeneratedKotlinCompilation(result);
    }

    @Test
    void compilesKotlinMainAndTestAgainstGeneratedSourcesWhenKotlinIsAppliedAfterApplicationPlugin() throws IOException {
        writeKotlinApplication(false, false);

        BuildResult result = buildResultWithIsolatedProjects("compileTestKotlin");

        assertGeneratedKotlinCompilation(result);
    }

    @Test
    void compilesKaptMainAndTestStubsAgainstGeneratedSourcesWhenKaptIsAppliedBeforeApplicationPlugin()
            throws IOException {
        writeKotlinApplication(true, true);

        BuildResult result = buildResultWithIsolatedProjects("kaptGenerateStubsKotlin", "kaptGenerateStubsTestKotlin");

        assertGeneratedKaptStubs(result);
    }

    @Test
    void compilesKaptMainAndTestStubsAgainstGeneratedSourcesWhenKaptIsAppliedAfterApplicationPlugin()
            throws IOException {
        writeKotlinApplication(false, true);

        BuildResult result = buildResultWithIsolatedProjects("kaptGenerateStubsKotlin", "kaptGenerateStubsTestKotlin");

        assertGeneratedKaptStubs(result);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void namedNativeSuiteCompilesKotlinAgainstSharedGeneratedTestSources(
            boolean kotlinBeforeApplicationPlugin) throws IOException {
        writeKotlinApplication(kotlinBeforeApplicationPlugin, false);

        BuildResult result = buildResultWithIsolatedProjects("compileQuarkusParityNativeTestKotlin", BUILD_CACHE);

        assertTaskOutcomes(result, SUCCESS,
                ":quarkusApplicationCodegenModel",
                ":quarkusApplicationTestCodegenModel",
                ":quarkusApplicationGenerateCode",
                ":quarkusApplicationGenerateTestCode");
        assertThat(result.task(":compileKotlin").getOutcome()).isIn(SUCCESS, FROM_CACHE);
        assertThat(result.task(":compileQuarkusParityNativeTestKotlin").getOutcome()).isIn(SUCCESS, FROM_CACHE);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void namedNativeSuiteCompilesKaptStubsAgainstSharedGeneratedTestSources(
            boolean kotlinBeforeApplicationPlugin) throws IOException {
        writeKotlinApplication(kotlinBeforeApplicationPlugin, true);

        BuildResult result = buildResultWithIsolatedProjects(
                "kaptGenerateStubsQuarkusParityNativeTestKotlin", BUILD_CACHE);

        assertTaskOutcomes(result, SUCCESS,
                ":quarkusApplicationCodegenModel",
                ":quarkusApplicationTestCodegenModel",
                ":quarkusApplicationGenerateCode",
                ":quarkusApplicationGenerateTestCode");
        assertThat(result.task(":kaptGenerateStubsQuarkusParityNativeTestKotlin").getOutcome())
                .isIn(SUCCESS, FROM_CACHE);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void developmentCodegenRunsBeforeKotlinMainCompilationForBothPluginApplicationOrders(
            boolean kotlinBeforeApplicationPlugin) throws IOException {
        writeKotlinApplication(kotlinBeforeApplicationPlugin, false);

        BuildResult result = buildResultWithIsolatedProjects(
                "quarkusApplicationGenerateDevCode", "compileKotlin");

        assertTaskOutcomes(result, SUCCESS,
                ":quarkusApplicationGenerateCode",
                ":quarkusApplicationGenerateDevCode",
                ":compileKotlin");
        assertTasksOrdered(result, ":quarkusApplicationGenerateDevCode", ":compileKotlin");
        assertThat(developmentGeneratedMain()).content().contains("VALUE = \"development\"");
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void developmentCodegenRunsBeforeKaptMainStubsForBothPluginApplicationOrders(
            boolean kotlinBeforeApplicationPlugin) throws IOException {
        writeKotlinApplication(kotlinBeforeApplicationPlugin, true);

        BuildResult result = buildResultWithIsolatedProjects(
                "quarkusApplicationGenerateDevCode", "kaptGenerateStubsKotlin");

        assertTaskOutcomes(result, SUCCESS,
                ":quarkusApplicationGenerateCode",
                ":quarkusApplicationGenerateDevCode",
                ":kaptGenerateStubsKotlin");
        assertTasksOrdered(result, ":quarkusApplicationGenerateDevCode", ":kaptGenerateStubsKotlin");
        assertThat(developmentGeneratedMain()).content().contains("VALUE = \"development\"");
    }

    @Test
    void kspWithSourcesJarDoesNotCreateGeneratedSourceCycle() throws IOException {
        writeKspSourcesJarApplication();

        BuildResult result = buildResultWithIsolatedProjects("sourcesJar", BUILD_CACHE);

        assertThat(result.task(":sourcesJar").getOutcome()).isIn(SUCCESS, FROM_CACHE);
        assertThat(result.task(":kspKotlin")).isNotNull();
        assertThat(result.task(":quarkusApplicationGenerateCode")).isNull();
        assertThat(testProjectDir.resolve("build/libs/ksp-sources-cycle-1.0-sources.jar")).isRegularFile();
    }

    private void writeKotlinApplication(boolean kotlinBeforeApplicationPlugin, boolean kapt) throws IOException {
        String kotlinVersion = System.getProperty("kotlin_version", "2.4.0");
        writeFile(testProjectDir.resolve("settings.gradle"), """
                pluginManagement {
                    repositories {
                        mavenCentral()
                        gradlePluginPortal()
                    }
                    plugins {
                        id 'org.jetbrains.kotlin.jvm' version '%1$s'
                        id 'org.jetbrains.kotlin.kapt' version '%1$s'
                    }
                }

                rootProject.name = 'kotlin-generated-sources'
                """.formatted(kotlinVersion));
        writeFile(testProjectDir.resolve("build.gradle"), buildFile(kotlinBeforeApplicationPlugin, kapt, kotlinVersion));
        writeFile(testProjectDir.resolve("src/main/kotlin/org/acme/KotlinMain.kt"), """
                package org.acme

                import org.acme.generated.GeneratedMain

                class KotlinMain {
                    fun value(): String = GeneratedMain.value()
                }
                """);
        writeFile(testProjectDir.resolve("src/test/kotlin/org/acme/KotlinTestUsage.kt"), """
                package org.acme

                import org.acme.generated.GeneratedTest

                class KotlinTestUsage {
                    fun value(): String = KotlinMain().value() + GeneratedTest.value()
                }
                """);
        writeFile(testProjectDir.resolve(
                "src/quarkusParityNativeTest/kotlin/org/acme/KotlinNamedNativeTestUsage.kt"), """
                        package org.acme

                        import org.acme.generated.GeneratedTest

                        class KotlinNamedNativeTestUsage {
                            fun value(): String = KotlinMain().value() + GeneratedTest.value()
                        }
                        """);
        if (kapt) {
            writeFile(testProjectDir.resolve("src/main/kotlin/org/acme/KaptMain.kt"), """
                    package org.acme

                    import org.acme.generated.GeneratedMain

                    @Deprecated(GeneratedMain.VALUE)
                    class KaptMain(val generated: GeneratedMain)
                    """);
            writeFile(testProjectDir.resolve("src/test/kotlin/org/acme/KaptTestUsage.kt"), """
                    package org.acme

                    import org.acme.generated.GeneratedTest

                    @Deprecated(GeneratedTest.VALUE)
                    class KaptTestUsage(val generated: GeneratedTest)
                    """);
            writeFile(testProjectDir.resolve(
                    "src/quarkusParityNativeTest/kotlin/org/acme/KaptNamedNativeTestUsage.kt"), """
                            package org.acme

                            import org.acme.generated.GeneratedTest

                            @Deprecated(GeneratedTest.VALUE)
                            class KaptNamedNativeTestUsage(val generated: GeneratedTest)
                            """);
        }
    }

    private void writeKspSourcesJarApplication() throws IOException {
        String kotlinVersion = System.getProperty("kotlin_version", "2.4.0");
        String kspVersion = System.getProperty("ksp_version", "2.3.9");
        writeFile(testProjectDir.resolve("settings.gradle"), """
                pluginManagement {
                    repositories {
                        mavenCentral()
                        gradlePluginPortal()
                    }
                    plugins {
                        id 'org.jetbrains.kotlin.jvm' version '%1$s'
                        id 'com.google.devtools.ksp' version '%2$s'
                    }
                }

                rootProject.name = 'ksp-sources-cycle'
                """.formatted(kotlinVersion, kspVersion));
        writeFile(testProjectDir.resolve("build.gradle"), """
                plugins {
                    id 'org.jetbrains.kotlin.jvm'
                    id 'com.google.devtools.ksp'
                    id 'io.quarkus.application'
                }

                version = '1.0'

                repositories {
                    mavenCentral()
                }

                dependencies {
                    implementation 'org.jetbrains.kotlin:kotlin-stdlib:%1$s'
                }

                java {
                    withSourcesJar()
                }

                def generatedSourceRoot = 'generated/sources/quarkus-application'
                def java = extensions.getByType(org.gradle.api.plugins.JavaPluginExtension)
                if (java.sourceSets.named('main').get().java.srcDirs.any {
                    it.path.replace(File.separator, '/').contains(generatedSourceRoot)
                }) {
                    throw new GradleException('main source set must not contain Quarkus generated sources')
                }
                """.formatted(kotlinVersion));
        writeFile(testProjectDir.resolve("src/main/kotlin/org/acme/KspApplication.kt"), """
                package org.acme

                class KspApplication
                """);
    }

    private static String buildFile(boolean kotlinBeforeApplicationPlugin, boolean kapt, String kotlinVersion) {
        String kotlinPlugin = "    id 'org.jetbrains.kotlin.jvm'\n";
        String kaptPlugin = kapt ? "    id 'org.jetbrains.kotlin.kapt'\n" : "";
        String applicationPlugin = "    id 'io.quarkus.application'\n";
        String plugins = kotlinBeforeApplicationPlugin
                ? kotlinPlugin + kaptPlugin + applicationPlugin
                : applicationPlugin + kotlinPlugin + kaptPlugin;
        return """
                import java.nio.file.Files

                plugins {
                %1$s}

                version = '1.0'

                repositories {
                    mavenCentral()
                }

                dependencies {
                    implementation 'org.jetbrains.kotlin:kotlin-stdlib:%2$s'
                }

                quarkusApplication {
                    builds {
                        nativeExecutable('parity')
                    }
                }

                def generatedSourceRoot = 'generated/sources/quarkus-application'
                def java = extensions.getByType(org.gradle.api.plugins.JavaPluginExtension)
                if (java.sourceSets.named('main').get().java.srcDirs.any {
                    it.path.replace(File.separator, '/').contains(generatedSourceRoot)
                }) {
                    throw new GradleException('main source set must not contain Quarkus generated sources')
                }
                if (java.sourceSets.named('test').get().java.srcDirs.any {
                    it.path.replace(File.separator, '/').contains(generatedSourceRoot)
                }) {
                    throw new GradleException('test source set must not contain Quarkus generated sources')
                }
                def assertGeneratedSourceWiring = { task, String sourceSegment ->
                    def generatedSourcePath = "${generatedSourceRoot}/${sourceSegment}"
                    if (!task.inputs.files.files.any {
                        it.path.replace(File.separator, '/').contains(generatedSourcePath)
                    }) {
                        throw new GradleException("${task.path} must include ${generatedSourcePath}")
                    }
                }
                ['compileKotlin', 'kaptGenerateStubsKotlin'].each { taskName ->
                    tasks.matching { it.name == taskName }.configureEach {
                        doFirst {
                            assertGeneratedSourceWiring(it, 'main/custom-main')
                        }
                    }
                }
                ['compileTestKotlin', 'kaptGenerateStubsTestKotlin'].each { taskName ->
                    tasks.matching { it.name == taskName }.configureEach {
                        doFirst {
                            assertGeneratedSourceWiring(it, 'test/custom-test')
                        }
                    }
                }
                tasks.named('quarkusApplicationGenerateCode').configure {
                    doLast {
                        def sourcePackage = generatedOutputDirectory.get().asFile.toPath()
                                .resolve('custom-main/org/acme/generated')
                        Files.createDirectories(sourcePackage)
                        Files.writeString(sourcePackage.resolve('GeneratedMain.java'), '''
                            package org.acme.generated;

                            public final class GeneratedMain {
                                public static final String VALUE = "main";

                                public static String value() {
                                    return VALUE;
                                }
                            }
                            '''.stripIndent())
                    }
                }
                tasks.named('quarkusApplicationGenerateDevCode').configure {
                    doLast {
                        def sourcePackage = generatedOutputDirectory.get().asFile.toPath()
                                .resolve('custom-main/org/acme/generated')
                        Files.createDirectories(sourcePackage)
                        Files.writeString(sourcePackage.resolve('GeneratedMain.java'), '''
                            package org.acme.generated;

                            public final class GeneratedMain {
                                public static final String VALUE = "development";

                                public static String value() {
                                    return VALUE;
                                }
                            }
                            '''.stripIndent())
                    }
                }
                tasks.named('quarkusApplicationGenerateTestCode').configure {
                    doLast {
                        def sourcePackage = generatedOutputDirectory.get().asFile.toPath()
                                .resolve('custom-test/org/acme/generated')
                        Files.createDirectories(sourcePackage)
                        Files.writeString(sourcePackage.resolve('GeneratedTest.java'), '''
                            package org.acme.generated;

                            public final class GeneratedTest {
                                public static final String VALUE = "test";

                                public static String value() {
                                    return GeneratedMain.value() + "-" + VALUE;
                                }
                            }
                            '''.stripIndent())
                    }
                }
                """.formatted(plugins, kotlinVersion);
    }

    private Path developmentGeneratedMain() {
        return testProjectDir.resolve(Path.of(
                "build", "generated", "sources", "quarkus-application", "main",
                "custom-main", "org", "acme", "generated", "GeneratedMain.java"));
    }

    private static void assertGeneratedKotlinCompilation(BuildResult result) {
        assertTaskOutcomes(result, SUCCESS,
                ":quarkusApplicationCodegenModel",
                ":quarkusApplicationTestCodegenModel",
                ":quarkusApplicationGenerateCode",
                ":quarkusApplicationGenerateTestCode",
                ":compileKotlin",
                ":compileTestKotlin");
    }

    private static void assertGeneratedKaptStubs(BuildResult result) {
        assertTaskOutcomes(result, SUCCESS,
                ":quarkusApplicationCodegenModel",
                ":quarkusApplicationTestCodegenModel",
                ":quarkusApplicationGenerateCode",
                ":quarkusApplicationGenerateTestCode",
                ":kaptGenerateStubsKotlin",
                ":kaptGenerateStubsTestKotlin");
    }

}
