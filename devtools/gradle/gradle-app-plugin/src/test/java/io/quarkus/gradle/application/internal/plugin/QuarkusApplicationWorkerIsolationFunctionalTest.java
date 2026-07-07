package io.quarkus.gradle.application.internal.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarFile;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import io.quarkus.gradle.testing.BaseGradleTest;

class QuarkusApplicationWorkerIsolationFunctionalTest extends BaseGradleTest {

    private static final String PROBE_RESOURCE = "META-INF/quarkus-worker-isolation-probe.properties";
    private static final String PROCESS_WORKER_TASK = ":quarkusAppBuild";

    @Test
    void processWorkerPreservesConfigurationAcrossRepeatedSubmissions() throws IOException {
        writeWorkerReuseApplication();

        assertRepeatedProcessWorkerSubmission();
    }

    @Test
    void classloaderIsolatedWorkerPreservesProfileSelectedImmediatelyBeforeAugmentation() throws IOException {
        writeNoProcessApplication();

        List<String> arguments = List.of(
                ":app:quarkusAppBuild",
                "--max-workers=1",
                "--no-parallel",
                BUILD_CACHE,
                "-Dgradle.quarkus.gradle-worker.no-process=true",
                "-Dquarkus.profile=worker-profile");
        BuildResult result = buildResultWithIsolatedProjects(arguments.toArray(String[]::new));

        assertTaskOutcomes(result, SUCCESS, ":app:quarkusAppBuild");
        assertThat(workerProbe("app"))
                .containsEntry("effective.profile-value", "from-worker-profile");

        BuildResult secondResult = buildResultWithIsolatedProjects(arguments.toArray(String[]::new));

        assertTaskOutcomes(secondResult, UP_TO_DATE, ":app:quarkusAppBuild");
        assertThat(secondResult.getOutput()).contains("Configuration cache entry reused.");
        assertThat(workerProbe("app"))
                .containsEntry("effective.profile-value", "from-worker-profile");
    }

    private void assertRepeatedProcessWorkerSubmission() throws IOException {
        BuildResult firstResult = buildResultWithIsolatedProjects(
                PROCESS_WORKER_TASK, "--max-workers=1", "--no-parallel", BUILD_CACHE);
        assertTaskOutcomes(firstResult, SUCCESS, PROCESS_WORKER_TASK);
        Properties firstProbe = workerProbeForBuild("app");

        BuildResult secondResult = buildResultWithIsolatedProjects(
                PROCESS_WORKER_TASK, "--max-workers=1", "--no-parallel", BUILD_CACHE, "--rerun-tasks");
        assertTaskOutcomes(secondResult, SUCCESS, PROCESS_WORKER_TASK);
        assertThat(secondResult.getOutput()).contains("Configuration cache entry reused.");
        Properties secondProbe = workerProbeForBuild("app");

        assertThat(firstProbe)
                .containsEntry("system.sentinel-before", "<missing>")
                .containsEntry("system.sentinel-after", "worker-mutation")
                .containsEntry("system.output-name", "shared-output")
                .containsEntry("effective.sentinel", "worker-mutation")
                .containsEntry("effective.output-name", "shared-output")
                .containsEntry("effective.application-value", "shared-config");
        assertThat(secondProbe)
                .containsEntry("system.sentinel-before", "<missing>")
                .containsEntry("system.sentinel-after", "worker-mutation")
                .containsEntry("system.output-name", "shared-output")
                .containsEntry("effective.sentinel", "worker-mutation")
                .containsEntry("effective.output-name", "shared-output")
                .containsEntry("effective.application-value", "shared-config");
    }

    private void writeWorkerReuseApplication() throws IOException {
        writeFile(testProjectDir.resolve("settings.gradle"), """
                rootProject.name = 'worker-reuse-application'
                include 'probe-runtime', 'probe-deployment'
                """);
        writeProbeExtension();
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
                    implementation enforcedPlatform('io.quarkus:quarkus-bom:999-SNAPSHOT')
                    implementation project(':probe-runtime')
                }

                quarkusApplication {
                    builds {
                        fastJar('app') {
                            outputName.set('shared-output')
                        }
                    }
                }
                """);
        writeFile(testProjectDir.resolve("src/main/java/org/acme/Application.java"), """
                package org.acme;

                public final class Application {
                }
                """);
        writeFile(testProjectDir.resolve("src/main/resources/application.properties"),
                "worker.application-value=shared-config\n");
    }

    private void writeNoProcessApplication() throws IOException {
        writeFile(testProjectDir.resolve("settings.gradle"), """
                rootProject.name = 'no-process-worker-application'
                include 'app', 'probe-runtime', 'probe-deployment'
                """);
        writeProbeExtension();
        writeApplication("app", "quarkusBuildProperties.put('quarkus.test.require-profile-value', 'true')", "");
        writeFile(testProjectDir.resolve("app/src/main/resources/application-worker-profile.properties"),
                "worker.profile-only=from-worker-profile\n");
        // Keep the invocation's profile daemon-local: it must not enter the worker's intended forked-property map.
        writeFile(testProjectDir.resolve("app/build.gradle"), Files.readString(testProjectDir.resolve("app/build.gradle"))
                + """

                        quarkusApplication {
                            configInputs {
                                systemProperties {
                                    prefixes.set([])
                                    names.set([])
                                }
                            }
                        }
                        """);
    }

    private void writeApplication(String projectName, String buildConfiguration, String applicationProperties)
            throws IOException {
        writeFile(testProjectDir.resolve(projectName + "/build.gradle"), """
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
                    implementation enforcedPlatform('io.quarkus:quarkus-bom:999-SNAPSHOT')
                    implementation project(':probe-runtime')
                }

                quarkusApplication {
                    builds {
                        fastJar('app') {
                            %s
                        }
                    }
                }
                """.formatted(buildConfiguration));
        writeFile(testProjectDir.resolve(projectName + "/src/main/java/org/acme/Application.java"), """
                package org.acme;

                public final class Application {
                }
                """);
        writeFile(testProjectDir.resolve(projectName + "/src/main/resources/application.properties"),
                applicationProperties);
    }

    private void writeProbeExtension() throws IOException {
        writeFile(testProjectDir.resolve("probe-runtime/build.gradle"), """
                plugins {
                    id 'java-library'
                    id 'io.quarkus.extension'
                }

                group = 'org.acme'
                version = '1.0'

                repositories {
                    mavenLocal()
                    mavenCentral()
                }

                quarkusExtension {
                    disableValidation = true
                    deploymentModule = 'probe-deployment'
                }

                dependencies {
                    implementation enforcedPlatform('io.quarkus:quarkus-bom:999-SNAPSHOT')
                    implementation 'io.quarkus:quarkus-core'
                }
                """);
        writeFile(testProjectDir.resolve("probe-runtime/src/main/java/org/acme/probe/ProbeExtension.java"), """
                package org.acme.probe;

                public final class ProbeExtension {
                }
                """);
        writeFile(testProjectDir.resolve("probe-deployment/build.gradle"), """
                plugins {
                    id 'java-library'
                    id 'io.quarkus.extension.deployment'
                }

                group = 'org.acme'
                version = '1.0'

                repositories {
                    mavenLocal()
                    mavenCentral()
                }

                dependencies {
                    implementation enforcedPlatform('io.quarkus:quarkus-bom:999-SNAPSHOT')
                    annotationProcessor 'io.quarkus:quarkus-extension-processor:999-SNAPSHOT'
                    implementation 'io.quarkus:quarkus-core-deployment'
                }
                """);
        writeFile(testProjectDir.resolve(
                "probe-deployment/src/main/java/org/acme/probe/deployment/WorkerIsolationProbeProcessor.java"), """
                        package org.acme.probe.deployment;

                        import java.nio.charset.StandardCharsets;

                        import org.eclipse.microprofile.config.ConfigProvider;

                        import io.quarkus.deployment.annotations.BuildStep;
                        import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;

                        class WorkerIsolationProbeProcessor {

                            private static final String MISSING = "<missing>";

                            @BuildStep
                            GeneratedResourceBuildItem workerIsolationProbe() {
                                var config = ConfigProvider.getConfig();
                                boolean requireProfileValue = config
                                        .getOptionalValue("quarkus.test.require-profile-value", Boolean.class)
                                        .orElse(false);
                                String profileValue = requireProfileValue
                                        ? config.getValue("worker.profile-only", String.class)
                                        : config.getOptionalValue("worker.profile-only", String.class).orElse(MISSING);
                                String applicationValue = config
                                        .getOptionalValue("worker.application-value", String.class).orElse(MISSING);
                                String staleSentinel = System.getProperty("quarkus.test.worker-sentinel", MISSING);
                                if ("shared-config".equals(applicationValue)) {
                                    System.setProperty("quarkus.test.worker-sentinel", "worker-mutation");
                                }
                                String probe = ("worker.pid=%s\\n"
                                        + "system.sentinel-before=%s\\n"
                                        + "system.sentinel-after=%s\\n"
                                        + "system.output-name=%s\\n"
                                        + "effective.sentinel=%s\\n"
                                        + "effective.output-name=%s\\n"
                                        + "effective.application-value=%s\\n"
                                        + "effective.profile-value=%s\\n").formatted(
                                        ProcessHandle.current().pid(),
                                        staleSentinel,
                                        System.getProperty("quarkus.test.worker-sentinel", MISSING),
                                        System.getProperty("quarkus.package.output-name", MISSING),
                                        config.getOptionalValue("quarkus.test.worker-sentinel", String.class).orElse(MISSING),
                                        config.getOptionalValue("quarkus.package.output-name", String.class).orElse(MISSING),
                                        applicationValue,
                                        profileValue);
                                return new GeneratedResourceBuildItem(
                                        "META-INF/quarkus-worker-isolation-probe.properties",
                                        probe.getBytes(StandardCharsets.UTF_8));
                            }
                        }
                        """);
    }

    private Properties workerProbe(String projectName) throws IOException {
        Path packageRoot = testProjectDir.resolve(projectName + "/build/quarkus-builds/app/package");
        return workerProbe(packageRoot);
    }

    private Properties workerProbeForBuild(String buildName) throws IOException {
        return workerProbe(testProjectDir.resolve("build/quarkus-builds/" + buildName + "/package"));
    }

    private static Properties workerProbe(Path packageRoot) throws IOException {
        try (var paths = Files.walk(packageRoot)) {
            for (Path candidate : paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .toList()) {
                try (var jar = new JarFile(candidate.toFile())) {
                    var entry = jar.getJarEntry(PROBE_RESOURCE);
                    if (entry != null) {
                        Properties properties = new Properties();
                        try (InputStream stream = jar.getInputStream(entry)) {
                            properties.load(stream);
                        }
                        return properties;
                    }
                }
            }
        }
        throw new AssertionError("No " + PROBE_RESOURCE + " found under " + packageRoot);
    }
}
