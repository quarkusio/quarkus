package io.quarkus.gradle.application;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;

import io.quarkus.gradle.testing.BaseGradleTest;

final class ToolingSemanticFixture {

    private ToolingSemanticFixture() {
    }

    static void write(Path root) throws IOException {
        write(root, "settings.gradle", """
                rootProject.name = 'semantic-matrix'
                include 'same-direct', 'same-transitive', 'same-compile-only', 'shared',
                        'custom-output', 'output-only', 'fixture-provider'
                includeBuild 'included'
                """);
        write(root, "build.gradle", """
                buildscript {
                    dependencies {
                        classpath files(%s)
                    }
                }

                apply plugin: 'io.quarkus.application'

                group = 'org.acme.application'
                version = '1.0'

                layout.buildDirectory = layout.projectDirectory.dir('application-build')

                sourceSets {
                    main {
                        java.srcDirs = ['application-sources/main/java']
                        resources.srcDirs = ['application-sources/main/resources']
                        output.classesDirs.from(layout.buildDirectory.dir('classes/java/secondary'))
                    }
                    test {
                        java.srcDirs = ['application-sources/test/java']
                        resources.srcDirs = ['application-sources/test/resources']
                        output.classesDirs.from(layout.buildDirectory.dir('classes/java/main'))
                    }
                }

                dependencies {
                    implementation project(':same-direct')
                    implementation project(':shared')
                    implementation project(':custom-output')
                    implementation project(':output-only')
                    implementation 'org.acme.included:included-bridge:1.0'
                    implementation 'org.acme.included:shared:1.0'
                    compileOnly project(':same-compile-only')
                    testImplementation testFixtures(project(':fixture-provider'))
                }
                """.formatted(pluginClasspathFiles()));
        writeJavaLibrary(root, "same-direct", "org.acme.same", "same-direct",
                "dependencies { api project(':same-transitive') }");
        writeJavaLibrary(root, "same-transitive", "org.acme.same", "same-transitive", "");
        writeJavaLibrary(root, "same-compile-only", "org.acme.same", "same-compile-only", "");
        writeJavaLibrary(root, "shared", "org.acme.same", "shared", "");
        writeCustomOutputLibrary(root);
        writeOutputOnlyLibrary(root);
        writeFixtureProvider(root);
        writeIncludedBuild(root);
        writeJavaSource(root, "application-sources/main/java", "ApplicationValue");
        write(root, "application-sources/main/resources/application-marker.txt", "marker");
        writeJavaSource(root, "application-sources/test/java", "ApplicationTestValue");
        write(root, "application-sources/test/resources/application-test-marker.txt", "marker");
    }

    private static void writeCustomOutputLibrary(Path root) throws IOException {
        write(root, "custom-output/build.gradle", """
                plugins {
                    id 'java-library'
                }

                group = 'org.acme.same'
                version = '1.0'

                layout.buildDirectory = layout.projectDirectory.dir('custom-build')
                sourceSets.main {
                    java.srcDirs = ['custom-java']
                    java.srcDir(layout.buildDirectory.dir('generated/sources/registered'))
                    resources.srcDirs = ['custom-resources']
                    output.classesDirs.from(layout.buildDirectory.dir('classes/java/secondary'))
                }
                """);
        writeJavaSource(root, "custom-output/custom-java", "CustomOutputValue");
        writeJavaSource(root, "custom-output/custom-build/generated/sources/registered", "RegisteredGeneratedValue");
        writeJavaSource(root, "custom-output/custom-build/generated/sources/unregistered", "UnregisteredGeneratedValue");
        write(root, "custom-output/custom-resources/custom-marker.txt", "marker");
    }

    private static void writeOutputOnlyLibrary(Path root) throws IOException {
        write(root, "output-only/build.gradle", """
                plugins {
                    id 'java-library'
                }

                group = 'org.acme.same'
                version = '1.0'

                configurations.named('mainSourceElements') {
                    outgoing.artifacts.clear()
                }
                """);
        writeJavaSource(root, "output-only/src/main/java", "OutputOnlyValue");
    }

    private static void writeFixtureProvider(Path root) throws IOException {
        write(root, "fixture-provider/build.gradle", """
                plugins {
                    id 'java-library'
                    id 'java-test-fixtures'
                }

                group = 'org.acme.same'
                version = '1.0'
                """);
        writeJavaSource(root, "fixture-provider/src/main/java", "FixtureProviderValue");
        writeJavaSource(root, "fixture-provider/src/testFixtures/java", "FixtureValue");
    }

    private static void writeIncludedBuild(Path root) throws IOException {
        write(root, "included/settings.gradle", """
                rootProject.name = 'included'
                include 'included-bridge', 'included-transitive', 'shared'
                """);
        writeJavaLibrary(root, "included/included-bridge", "org.acme.included", "included-bridge",
                "dependencies { api project(':included-transitive') }");
        writeJavaLibrary(root, "included/included-transitive", "org.acme.included", "included-transitive", "");
        writeJavaLibrary(root, "included/shared", "org.acme.included", "shared", "");
    }

    private static void writeJavaLibrary(Path root, String projectDirectory, String group, String archiveName,
            String extra) throws IOException {
        write(root, projectDirectory + "/build.gradle", """
                plugins {
                    id 'java-library'
                }

                group = '%s'
                version = '1.0'
                base.archivesName = '%s'

                %s
                """.formatted(group, archiveName, extra));
        writeJavaSource(root, projectDirectory + "/src/main/java", className(archiveName));
    }

    private static void writeJavaSource(Path root, String sourceRoot, String className) throws IOException {
        write(root, sourceRoot + "/org/acme/" + className + ".java", """
                package org.acme;

                public final class %s {
                }
                """.formatted(className));
    }

    private static void write(Path root, String relativePath, String content) throws IOException {
        BaseGradleTest.writeFile(root.resolve(relativePath), content);
    }

    private static String pluginClasspathFiles() {
        return TestKitPluginClasspath.implementationClasspath().stream()
                .map(File::getAbsolutePath)
                .map(ToolingSemanticFixture::singleQuotedGroovyString)
                .collect(Collectors.joining(", "));
    }

    private static String singleQuotedGroovyString(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    private static String className(String projectName) {
        StringBuilder result = new StringBuilder();
        boolean capitalize = true;
        for (char character : projectName.toCharArray()) {
            if (character == '-') {
                capitalize = true;
            } else if (capitalize) {
                result.append(Character.toUpperCase(character));
                capitalize = false;
            } else {
                result.append(character);
            }
        }
        return result.append("Value").toString();
    }
}
