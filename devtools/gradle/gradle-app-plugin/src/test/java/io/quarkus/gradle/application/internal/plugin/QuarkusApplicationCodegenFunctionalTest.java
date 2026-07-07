package io.quarkus.gradle.application.internal.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import io.quarkus.gradle.testing.BaseGradleTest;

class QuarkusApplicationCodegenFunctionalTest extends BaseGradleTest {

    @Test
    void applicationModelsDependOnSameProjectKordampJandexTask() throws IOException {
        writeApplicationWithJandexTask("jandex");

        BuildResult result = buildResultWithIsolatedProjects("verifyJandexModelWiring");

        assertTaskOutcomes(result, SUCCESS,
                ":jandex",
                ":quarkusApplicationModel",
                ":quarkusApplicationDevModel",
                ":verifyJandexModelWiring");
    }

    @Test
    void applicationModelsDependOnSameProjectVlsiJandexTask() throws IOException {
        writeApplicationWithJandexTask("processJandexIndex");

        BuildResult result = buildResultWithIsolatedProjects("verifyJandexModelWiring");

        assertTaskOutcomes(result, SUCCESS,
                ":processJandexIndex",
                ":quarkusApplicationModel",
                ":quarkusApplicationDevModel",
                ":verifyJandexModelWiring");
    }

    @Test
    void compilesGeneratedSourcesWithPlainProjectDependencyAndIsolatedProjects() throws IOException {
        writeMultiProjectCodegenApplication();

        BuildResult result = buildResultWithIsolatedProjects(":app:compileJava");

        assertTaskOutcomes(result, SUCCESS,
                ":lib:compileJava",
                ":app:quarkusApplicationCodegenModel",
                ":app:quarkusApplicationGenerateCode",
                ":app:compileJava");
        assertThat(result.task(":app:quarkusApplicationModelPomClosure")).isNull();
        assertThat(testProjectDir.resolve(Path.of("app", "build", "generated", "sources", "quarkus-application", "main",
                "custom-main", "org", "acme", "generated", "GeneratedFromLib.java"))).isRegularFile();
        assertThat(testProjectDir.resolve(Path.of("app", "build", "classes", "java", "main",
                "org", "acme", "generated", "GeneratedFromLib.class"))).isRegularFile();
        assertThat(testProjectDir.resolve(Path.of("app", "build", "classes", "java", "main",
                "org", "acme", "App.class"))).isRegularFile();

        BuildResult secondResult = buildResultWithIsolatedProjects(":app:compileJava");

        assertThat(secondResult.getOutput()).contains("Configuration cache entry reused.");
        assertTaskOutcomes(secondResult, UP_TO_DATE,
                ":lib:compileJava",
                ":app:quarkusApplicationCodegenModel",
                ":app:quarkusApplicationGenerateCode",
                ":app:compileJava");
    }

    @Test
    void compilesGeneratedSourcesFromCustomProviderDirectoriesWithConfigurationCacheAndIsolatedProjects()
            throws IOException {
        writeStubbedCodegenApplication();

        BuildResult result = buildResultWithIsolatedProjects("compileTestJava");

        assertTaskOutcomes(result, SUCCESS,
                ":quarkusApplicationCodegenModel",
                ":quarkusApplicationTestCodegenModel",
                ":quarkusApplicationGenerateCode",
                ":quarkusApplicationGenerateTestCode",
                ":compileJava",
                ":compileTestJava");
        assertThat(testProjectDir.resolve(Path.of("build", "generated", "sources", "quarkus-application", "main",
                "custom-main", "org", "acme", "generated", "GeneratedMain.java"))).isRegularFile();
        assertThat(testProjectDir.resolve(Path.of("build", "generated", "sources", "quarkus-application", "test",
                "custom-test", "org", "acme", "generated", "GeneratedTest.java"))).isRegularFile();
        assertThat(testProjectDir.resolve(Path.of("build", "classes", "java", "main",
                "org", "acme", "generated", "GeneratedMain.class"))).isRegularFile();
        assertThat(testProjectDir.resolve(Path.of("build", "classes", "java", "test",
                "org", "acme", "GeneratedSourceUsage.class"))).isRegularFile();

        BuildResult secondResult = buildResultWithIsolatedProjects("compileTestJava");

        assertThat(secondResult.getOutput()).contains("Configuration cache entry reused.");
        assertTaskOutcomes(secondResult, UP_TO_DATE,
                ":quarkusApplicationCodegenModel",
                ":quarkusApplicationTestCodegenModel",
                ":quarkusApplicationGenerateCode",
                ":quarkusApplicationGenerateTestCode",
                ":compileJava",
                ":compileTestJava");
    }

    @Test
    void switchesSharedGeneratedSourcesBetweenNormalAndDevelopmentCodegen() throws IOException {
        writeStubbedCodegenApplication();
        Files.writeString(testProjectDir.resolve("build.gradle"), """

                tasks.named('quarkusApplicationGenerateDevCode').configure {
                    doLast {
                        def sourceFile = generatedOutputDirectory.get().asFile.toPath()
                                .resolve('custom-main/org/acme/generated/GeneratedMain.java')
                        Files.writeString(sourceFile, '''
                            package org.acme.generated;

                            public final class GeneratedMain {
                                public static String value() {
                                    return "development";
                                }
                            }
                            '''.stripIndent())
                    }
                }
                """, StandardOpenOption.APPEND);
        Path generatedSource = testProjectDir.resolve(Path.of("build", "generated", "sources",
                "quarkus-application", "main", "custom-main", "org", "acme", "generated", "GeneratedMain.java"));

        BuildResult normal = buildResultWithIsolatedProjects("compileJava");
        assertTaskOutcomes(normal, SUCCESS,
                ":quarkusApplicationGenerateCode",
                ":compileJava");
        assertThat(generatedSource).content().contains("return \"main\"");

        BuildResult development = buildResultWithIsolatedProjects(
                "quarkusApplicationGenerateDevCode", "compileJava");
        assertTaskOutcomes(development, SUCCESS,
                ":quarkusApplicationGenerateDevCode",
                ":compileJava");
        assertThat(generatedSource).content().contains("return \"development\"");

        BuildResult normalAgain = buildResultWithIsolatedProjects("compileJava");
        assertTaskOutcomes(normalAgain, SUCCESS,
                ":quarkusApplicationGenerateCode",
                ":compileJava");
        assertThat(generatedSource).content().contains("return \"main\"");

        BuildResult developmentAgain = buildResultWithIsolatedProjects(
                "quarkusApplicationGenerateDevCode", "compileJava");
        assertConfigurationCacheReused(developmentAgain);
        assertTaskOutcomes(developmentAgain, SUCCESS,
                ":quarkusApplicationGenerateDevCode",
                ":compileJava");
        assertThat(generatedSource).content().contains("return \"development\"");
    }

    @Test
    void compilesRealAvroGeneratedSourcesWithConfigurationCacheAndIsolatedProjects() throws IOException {
        writeRealAvroCodegenApplication();

        BuildResult result = buildResultWithIsolatedProjects("compileJava");

        assertTaskOutcomes(result, SUCCESS,
                ":quarkusApplicationCodegenModel",
                ":quarkusApplicationGenerateCode",
                ":compileJava");
        assertThat(containsFileNamed(testProjectDir.resolve(Path.of("build", "generated", "sources",
                "quarkus-application", "main")), "Greeting.java")).isTrue();
        assertThat(testProjectDir.resolve(Path.of("build", "classes", "java", "main",
                "org", "acme", "AvroUsage.class"))).isRegularFile();
        assertThat(testProjectDir.resolve(Path.of("build", "classes", "java", "main",
                "org", "acme", "quarkus", "hello", "Greeting.class"))).isRegularFile();
    }

    @Test
    void regeneratesGrpcSourcesWhenDependencyProtoChanges() throws IOException {
        writeMultiProjectGrpcCodegenApplication();

        BuildResult result = buildResultWithIsolatedProjects(":app:compileJava");

        assertTaskOutcomes(result, SUCCESS,
                ":lib:processResources",
                ":lib:jar",
                ":app:quarkusApplicationCodegenModel",
                ":app:quarkusApplicationGenerateCode",
                ":app:compileJava");
        Path generatedGreeting = findFileNamed(testProjectDir.resolve(Path.of("app", "build", "generated", "sources",
                "quarkus-application", "main")), "Greeting.java");
        assertThat(Files.readString(generatedGreeting)).doesNotContain("getSalutation");

        writeGrpcProto(true);

        BuildResult secondResult = buildResultWithIsolatedProjects(":app:compileJava");

        assertThat(secondResult.getOutput()).contains("Configuration cache entry reused.");
        assertTaskOutcomes(secondResult, Map.of(
                ":lib:processResources", SUCCESS,
                ":lib:jar", SUCCESS,
                ":app:quarkusApplicationCodegenModel", SUCCESS,
                ":app:quarkusApplicationGenerateCode", SUCCESS,
                ":app:compileJava", SUCCESS));
        assertThat(Files.readString(generatedGreeting)).contains("getSalutation");
    }

    @Test
    void regeneratesGrpcSourcesWhenIncludedBuildDependencyProtoChanges() throws IOException {
        Path protoFile = writeIncludedBuildGrpcCodegenApplication();

        BuildResult result = buildResultWithIsolatedProjects("compileJava");

        assertTaskOutcomes(result, SUCCESS,
                ":quarkusApplicationCodegenModel",
                ":quarkusApplicationGenerateCode",
                ":compileJava");
        Path generatedGreeting = findFileNamed(testProjectDir.resolve(Path.of("build", "generated", "sources",
                "quarkus-application", "main")), "Greeting.java");
        assertThat(Files.readString(generatedGreeting)).doesNotContain("getSalutation");

        writeGrpcProto(protoFile, true);

        BuildResult secondResult = buildResultWithIsolatedProjects("compileJava");

        assertThat(secondResult.getOutput()).contains("Configuration cache entry reused.");
        assertTaskOutcomes(secondResult, Map.of(
                ":quarkusApplicationCodegenModel", SUCCESS,
                ":quarkusApplicationGenerateCode", SUCCESS,
                ":compileJava", SUCCESS));
        assertThat(Files.readString(generatedGreeting)).contains("getSalutation");
    }

    private void writeApplicationWithJandexTask(String jandexTaskName) throws IOException {
        writeFile(testProjectDir.resolve("settings.gradle"), "rootProject.name = 'jandex-app'\n");
        writeFile(testProjectDir.resolve("gradle.properties"), "version = 999-SNAPSHOT\n");
        writeFile(testProjectDir.resolve("build.gradle"), """
                import org.gradle.api.GradleException

                plugins {
                    id 'io.quarkus.application'
                }

                repositories {
                    mavenLocal()
                    mavenCentral()
                }

                dependencies {
                    implementation enforcedPlatform("io.quarkus:quarkus-bom:${project.property('version')}")
                    implementation "io.quarkus:quarkus-arc"
                }

                def jandexMarker = layout.buildDirectory.file('jandex/%1$s.marker')

                tasks.register('%1$s') {
                    outputs.file(jandexMarker)
                    doLast {
                        def marker = jandexMarker.get().asFile
                        marker.parentFile.mkdirs()
                        marker.text = '%1$s'
                    }
                }

                tasks.register('verifyJandexModelWiring') {
                    dependsOn tasks.named('quarkusApplicationModel')
                    dependsOn tasks.named('quarkusApplicationDevModel')
                    inputs.file(jandexMarker)
                    doLast {
                        if (!jandexMarker.get().asFile.isFile()) {
                            throw new GradleException('Jandex marker was not produced')
                        }
                    }
                }
                """.formatted(jandexTaskName));
        writeFile(testProjectDir.resolve("src/main/java/org/acme/GreetingService.java"), """
                package org.acme;

                public final class GreetingService {
                    public String hello() {
                        return "hello";
                    }
                }
                """);
    }

    private void writeMultiProjectCodegenApplication() throws IOException {
        writeFile(testProjectDir.resolve("settings.gradle"), """
                rootProject.name = 'multi-project-codegen-app'
                include 'app', 'lib'
                """);
        writeFile(testProjectDir.resolve("lib/build.gradle"), """
                plugins {
                    id 'java-library'
                }
                """);
        writeFile(testProjectDir.resolve("lib/src/main/java/org/acme/lib/GreetingLibrary.java"), """
                package org.acme.lib;

                public final class GreetingLibrary {
                    public String message() {
                        return "hello";
                    }
                }
                """);
        writeFile(testProjectDir.resolve("app/build.gradle"), """
                import java.nio.file.Files

                plugins {
                    id 'io.quarkus.application'
                }

                version = '1.0'

                dependencies {
                    implementation project(':lib')
                }

                quarkusApplication {
                    codegen {
                        providers = ['custom-main']
                    }
                }

                tasks.named('quarkusApplicationGenerateCode').configure {
                    doLast {
                        def sourcePackage = generatedOutputDirectory.get().asFile.toPath()
                                .resolve('custom-main/org/acme/generated')
                        Files.createDirectories(sourcePackage)
                        Files.writeString(sourcePackage.resolve('GeneratedFromLib.java'), '''
                            package org.acme.generated;

                            import org.acme.lib.GreetingLibrary;

                            public final class GeneratedFromLib {
                                public static String value() {
                                    return new GreetingLibrary().message();
                                }
                            }
                            '''.stripIndent())
                    }
                }
                """);
        writeFile(testProjectDir.resolve("app/src/main/java/org/acme/App.java"), """
                package org.acme;

                import org.acme.generated.GeneratedFromLib;

                public final class App {
                    public String value() {
                        return GeneratedFromLib.value();
                    }
                }
                """);
    }

    private void writeStubbedCodegenApplication() throws IOException {
        writeFile(testProjectDir.resolve("settings.gradle"), "rootProject.name = 'stubbed-codegen-app'\n");
        writeFile(testProjectDir.resolve("build.gradle"), """
                import java.nio.file.Files
                import org.gradle.api.tasks.compile.JavaCompile

                plugins {
                    id 'io.quarkus.application'
                }

                version = '1.0'

                quarkusApplication {
                    codegen {
                        providers = ['custom-main', 'custom-test']
                    }
                }

                tasks.withType(JavaCompile).configureEach {
                    include 'org/**'
                }

                tasks.named('quarkusApplicationGenerateCode').configure {
                    doLast {
                        def sourcePackage = generatedOutputDirectory.get().asFile.toPath()
                                .resolve('custom-main/org/acme/generated')
                        Files.createDirectories(sourcePackage)
                        Files.writeString(sourcePackage.resolve('GeneratedMain.java'), '''
                            package org.acme.generated;

                            public final class GeneratedMain {
                                public static String value() {
                                    return "main";
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
                                public static String value() {
                                    return GeneratedMain.value() + "-test";
                                }
                            }
                            '''.stripIndent())
                    }
                }
                """);
        writeFile(testProjectDir.resolve("src/main/java/org/acme/App.java"), """
                package org.acme;

                import org.acme.generated.GeneratedMain;

                public final class App {
                    public String value() {
                        return GeneratedMain.value();
                    }
                }
                """);
        writeFile(testProjectDir.resolve("src/test/java/org/acme/GeneratedSourceUsage.java"), """
                package org.acme;

                import org.acme.generated.GeneratedTest;

                public final class GeneratedSourceUsage {
                    public String value() {
                        return new App().value() + GeneratedTest.value();
                    }
                }
                """);
    }

    private void writeRealAvroCodegenApplication() throws IOException {
        writeFile(testProjectDir.resolve("settings.gradle"), "rootProject.name = 'real-avro-codegen-app'\n");
        writeFile(testProjectDir.resolve("gradle.properties"), "version = 999-SNAPSHOT\n");
        writeFile(testProjectDir.resolve("build.gradle"), """
                plugins {
                    id 'io.quarkus.application'
                }

                repositories {
                    mavenLocal()
                    mavenCentral()
                }

                dependencies {
                    implementation enforcedPlatform("io.quarkus:quarkus-bom:${project.property('version')}")
                    implementation 'io.quarkus:quarkus-avro'
                }
                """);
        writeFile(testProjectDir.resolve("src/main/avro/greeting.avsc"), """
                {
                  "type": "record",
                  "namespace": "org.acme.quarkus.hello",
                  "name": "Greeting",
                  "fields": [
                    { "name": "message", "type": "string" }
                  ]
                }
                """);
        writeFile(testProjectDir.resolve("src/main/java/org/acme/AvroUsage.java"), """
                package org.acme;

                import org.acme.quarkus.hello.Greeting;

                public final class AvroUsage {
                    public Greeting greeting() {
                        return Greeting.newBuilder().setMessage("hello").build();
                    }
                }
                """);
    }

    private void writeMultiProjectGrpcCodegenApplication() throws IOException {
        writeFile(testProjectDir.resolve("settings.gradle"), """
                rootProject.name = 'multi-project-grpc-codegen-app'
                include 'app', 'lib'
                """);
        writeFile(testProjectDir.resolve("gradle.properties"), "quarkusVersion = 999-SNAPSHOT\n");
        writeFile(testProjectDir.resolve("lib/build.gradle"), """
                plugins {
                    id 'java-library'
                }

                group = 'org.acme'
                version = '1.0'
                """);
        writeFile(testProjectDir.resolve("app/build.gradle"), """
                plugins {
                    id 'io.quarkus.application'
                }

                group = 'org.acme'
                version = '1.0'

                repositories {
                    mavenLocal()
                    mavenCentral()
                }

                dependencies {
                    implementation enforcedPlatform("io.quarkus:quarkus-bom:${project.property('quarkusVersion')}")
                    implementation project(':lib')
                    implementation 'io.quarkus:quarkus-grpc'
                }
                """);
        writeFile(testProjectDir.resolve("app/src/main/resources/application.properties"),
                "quarkus.generate-code.grpc.scan-for-proto=org.acme:lib\n");
        writeFile(testProjectDir.resolve("app/src/main/java/org/acme/App.java"), """
                package org.acme;

                import org.acme.proto.Greeting;

                public final class App {
                    public Greeting greeting() {
                        return Greeting.newBuilder().setMessage("hello").build();
                    }
                }
                """);
        writeGrpcProto(false);
    }

    private void writeGrpcProto(boolean includeSalutation) throws IOException {
        writeGrpcProto(testProjectDir.resolve("lib/src/main/resources/proto/greeting.proto"), includeSalutation);
    }

    private Path writeIncludedBuildGrpcCodegenApplication() throws IOException {
        writeFile(testProjectDir.resolve("settings.gradle"), """
                rootProject.name = 'included-build-grpc-codegen-app'
                includeBuild 'lib-build'
                """);
        writeFile(testProjectDir.resolve("gradle.properties"), "quarkusVersion = 999-SNAPSHOT\n");
        writeFile(testProjectDir.resolve("build.gradle"), """
                plugins {
                    id 'io.quarkus.application'
                }

                group = 'org.acme'
                version = '1.0'

                repositories {
                    mavenLocal()
                    mavenCentral()
                }

                dependencies {
                    implementation enforcedPlatform("io.quarkus:quarkus-bom:${project.property('quarkusVersion')}")
                    implementation 'org.acme:lib:1.0'
                    implementation 'io.quarkus:quarkus-grpc'
                }
                """);
        writeFile(testProjectDir.resolve("src/main/resources/application.properties"),
                "quarkus.generate-code.grpc.scan-for-proto=org.acme:lib\n");
        writeFile(testProjectDir.resolve("src/main/java/org/acme/App.java"), """
                package org.acme;

                import org.acme.proto.Greeting;

                public final class App {
                    public Greeting greeting() {
                        return Greeting.newBuilder().setMessage("hello").build();
                    }
                }
                """);
        writeFile(testProjectDir.resolve("lib-build/settings.gradle"), """
                rootProject.name = 'lib-build'
                include 'lib'
                """);
        writeFile(testProjectDir.resolve("lib-build/lib/build.gradle"), """
                plugins {
                    id 'java-library'
                }

                group = 'org.acme'
                version = '1.0'
                """);
        Path protoFile = testProjectDir.resolve("lib-build/lib/src/main/resources/proto/greeting.proto");
        writeGrpcProto(protoFile, false);
        return protoFile;
    }

    private static void writeGrpcProto(Path protoFile, boolean includeSalutation) throws IOException {
        writeFile(protoFile, """
                syntax = "proto3";

                option java_multiple_files = true;
                option java_package = "org.acme.proto";
                option java_outer_classname = "GreetingProto";

                package greeting;

                message Greeting {
                  string message = 1;
                %s}
                """.formatted(includeSalutation ? "  string salutation = 2;\n" : ""));
    }

    private static Path findFileNamed(Path root, String fileName) throws IOException {
        try (var paths = Files.walk(root)) {
            return paths.filter(path -> path.getFileName().toString().equals(fileName))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Could not find " + fileName + " under " + root));
        }
    }

}
