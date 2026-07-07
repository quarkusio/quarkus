package io.quarkus.gradle.application.internal.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.FROM_CACHE;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import io.quarkus.gradle.testing.BaseGradleTest;

class QuarkusApplicationCompositePackagingFunctionalTest extends BaseGradleTest {

    private static final String LIBRARY_CLASS = "org/acme/library/IncludedGreeting.class";
    private static final String LIBRARY_RESOURCE = "included-library.properties";

    @Test
    void packagesIncludedBuildDependencyInFastAndUberOutputs() throws IOException {
        writeApplication();

        BuildResult firstResult = buildResultWithIsolatedProjects(
                ":quarkusFastBuild", ":quarkusUberBuild", BUILD_CACHE);

        assertThat(firstResult.task(":library-build:compileJava").getOutcome()).isIn(SUCCESS, FROM_CACHE);
        assertTaskOutcomes(firstResult, SUCCESS,
                ":library-build:processResources",
                ":library-build:jar",
                ":quarkusApplicationModel",
                ":quarkusFastBuild",
                ":quarkusUberBuild");
        assertFastPackageContainsIncludedLibrary();
        assertArchiveContains(
                testProjectDir.resolve("build/quarkus-builds/uber/package/composite-package-app-1.0-runner.jar"),
                LIBRARY_CLASS, LIBRARY_RESOURCE);

        BuildResult secondResult = buildResultWithIsolatedProjects(
                ":quarkusFastBuild", ":quarkusUberBuild", BUILD_CACHE);

        assertThat(secondResult.getOutput()).contains("Configuration cache entry reused.");
        assertTaskOutcomes(secondResult, UP_TO_DATE,
                ":library-build:compileJava",
                ":library-build:processResources",
                ":library-build:jar",
                ":quarkusApplicationModel",
                ":quarkusFastBuild",
                ":quarkusUberBuild");
    }

    @Test
    void consumesIncludedApplicationPackageAndLauncherByAttributes() throws IOException {
        writeIncludedApplicationVariantConsumer();

        BuildResult firstResult = buildResultWithIsolatedProjects(":verifyIncludedApplicationPackage");

        assertTaskOutcomes(firstResult, SUCCESS,
                ":application-build:quarkusFastBuild",
                ":copyIncludedApplicationPackage",
                ":verifyIncludedApplicationPackage");
        assertThat(firstResult.getOutput())
                .contains("includedPackageDirectory=package")
                .contains("includedLauncherFile=quarkus-run.jar");
        assertThat(testProjectDir.resolve("build/included-application/quarkus-run.jar")).isRegularFile();

        BuildResult secondResult = buildResultWithIsolatedProjects(":verifyIncludedApplicationPackage");

        assertThat(secondResult.getOutput()).contains("Configuration cache entry reused.");
        assertTaskOutcomes(secondResult, UP_TO_DATE,
                ":application-build:quarkusFastBuild",
                ":copyIncludedApplicationPackage");
        assertTaskOutcomes(secondResult, SUCCESS, ":verifyIncludedApplicationPackage");
    }

    private void assertFastPackageContainsIncludedLibrary() throws IOException {
        Path libraryDirectory = testProjectDir.resolve("build/quarkus-builds/fast/package/lib");
        try (var files = Files.walk(libraryDirectory)) {
            Path libraryJar = files
                    .filter(path -> path.getFileName().toString().endsWith(".library-1.0.jar"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Included-build library JAR not found under " + libraryDirectory));
            assertArchiveContains(libraryJar, LIBRARY_CLASS, LIBRARY_RESOURCE);
        }
    }

    private static void assertArchiveContains(Path archive, String... entries) throws IOException {
        assertThat(archive).isRegularFile();
        try (JarFile jar = new JarFile(archive.toFile())) {
            for (String entry : entries) {
                assertThat(jar.getJarEntry(entry)).as("%s in %s", entry, archive).isNotNull();
            }
        }
    }

    private void writeApplication() throws IOException {
        writeFile("settings.gradle", """
                rootProject.name = 'composite-package-app'
                includeBuild 'library-build'
                """);
        writeFile("gradle.properties", "quarkusVersion = 999-SNAPSHOT\n");
        writeFile("build.gradle", """
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
                    implementation "io.quarkus:quarkus-arc"
                    implementation 'org.acme:library:1.0'
                }

                quarkusApplication {
                    builds {
                        fastJar('fast')
                        uberJar('uber')
                    }
                }
                """);
        writeFile("src/main/java/org/acme/ApplicationGreeting.java", """
                package org.acme;

                import org.acme.library.IncludedGreeting;

                public final class ApplicationGreeting {
                    public String message() {
                        return new IncludedGreeting().message();
                    }
                }
                """);
        writeFile("library-build/settings.gradle", "rootProject.name = 'library'\n");
        writeFile("library-build/build.gradle", """
                plugins {
                    id 'java-library'
                }

                group = 'org.acme'
                version = '1.0'
                """);
        writeFile("library-build/src/main/java/org/acme/library/IncludedGreeting.java", """
                package org.acme.library;

                public final class IncludedGreeting {
                    public String message() {
                        return "hello from included build";
                    }
                }
                """);
        writeFile("library-build/src/main/resources/included-library.properties", "included=true\n");
    }

    private void writeIncludedApplicationVariantConsumer() throws IOException {
        writeFile("settings.gradle", """
                rootProject.name = 'included-application-variant-consumer'
                includeBuild 'application-build'
                """);
        writeFile("build.gradle", """
                import org.gradle.api.artifacts.type.ArtifactTypeDefinition
                import org.gradle.api.attributes.Attribute
                import org.gradle.api.attributes.Category
                import org.gradle.api.attributes.LibraryElements
                import org.gradle.api.file.ConfigurableFileCollection
                import org.gradle.api.file.DirectoryProperty
                import org.gradle.api.tasks.InputDirectory
                import org.gradle.api.tasks.InputFiles
                import org.gradle.api.tasks.TaskAction

                plugins {
                    id 'base'
                }

                def buildName = Attribute.of('io.quarkus.application.build-name', String)
                def buildType = Attribute.of('io.quarkus.application.build-type', String)

                configurations {
                    packageRoot {
                        canBeConsumed = false
                        canBeResolved = true
                        attributes {
                            attribute(Category.CATEGORY_ATTRIBUTE,
                                objects.named(Category, 'quarkus-application-package'))
                            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                                objects.named(LibraryElements, 'quarkus-application-package-directory'))
                            attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, 'directory')
                            attribute(buildName, 'fast')
                            attribute(buildType, 'fast-jar')
                        }
                    }
                    launcher {
                        canBeConsumed = false
                        canBeResolved = true
                        attributes {
                            attribute(Category.CATEGORY_ATTRIBUTE,
                                objects.named(Category, 'quarkus-application-launcher'))
                            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                                objects.named(LibraryElements, 'quarkus-application-launcher-jar'))
                            attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, 'jar')
                            attribute(buildName, 'fast')
                            attribute(buildType, 'fast-jar')
                        }
                    }
                }

                dependencies {
                    packageRoot 'org.acme:included-application:1.0'
                    launcher 'org.acme:included-application:1.0'
                }

                tasks.register('copyIncludedApplicationPackage', Sync) {
                    from configurations.packageRoot
                    into layout.buildDirectory.dir('included-application')
                }

                abstract class VerifyIncludedApplicationPackage extends DefaultTask {
                    @InputFiles
                    abstract ConfigurableFileCollection getPackageRoots()

                    @InputFiles
                    abstract ConfigurableFileCollection getLaunchers()

                    @InputDirectory
                    abstract DirectoryProperty getCopiedPackage()

                    @TaskAction
                    void verify() {
                        assert packageRoots.files.size() == 1
                        assert packageRoots.singleFile.isDirectory()
                        assert launchers.files.size() == 1
                        assert launchers.singleFile.isFile()
                        assert launchers.singleFile.toPath().startsWith(packageRoots.singleFile.toPath())
                        assert copiedPackage.file('quarkus-run.jar').get().asFile.isFile()
                        println "includedPackageDirectory=${packageRoots.singleFile.name}"
                        println "includedLauncherFile=${launchers.singleFile.name}"
                    }
                }

                tasks.register('verifyIncludedApplicationPackage', VerifyIncludedApplicationPackage) {
                    dependsOn tasks.named('copyIncludedApplicationPackage')
                    packageRoots.from configurations.packageRoot
                    launchers.from configurations.launcher
                    copiedPackage.set layout.buildDirectory.dir('included-application')
                }
                """);
        writeFile("application-build/settings.gradle", "rootProject.name = 'included-application'\n");
        writeFile("application-build/gradle.properties", "quarkusVersion = 999-SNAPSHOT\n");
        writeFile("application-build/build.gradle", """
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
                    implementation "io.quarkus:quarkus-arc"
                }

                quarkusApplication {
                    builds {
                        fastJar('fast')
                    }
                }
                """);
        writeFile("application-build/src/main/java/org/acme/IncludedApplication.java", """
                package org.acme;

                public final class IncludedApplication {
                    public String message() {
                        return "included application";
                    }
                }
                """);
    }
}
