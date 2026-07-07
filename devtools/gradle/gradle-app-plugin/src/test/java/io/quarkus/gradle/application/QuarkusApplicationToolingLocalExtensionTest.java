package io.quarkus.gradle.application;

import static io.quarkus.bootstrap.model.gradle.GradleProjectComponent.ClasspathAssociation.COMPILE_ONLY;
import static io.quarkus.bootstrap.model.gradle.GradleProjectComponent.Role.EXTENSION_DEPLOYMENT;
import static io.quarkus.bootstrap.model.gradle.GradleProjectComponent.Role.EXTENSION_RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

import org.gradle.tooling.BuildActionExecuter;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.events.OperationType;
import org.gradle.tooling.events.task.TaskStartEvent;
import org.gradle.wrapper.GradleUserHomeLookup;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.gradle.GradleApplicationModelSidecar;
import io.quarkus.bootstrap.model.gradle.GradleModelCorrelationSupport;
import io.quarkus.bootstrap.model.gradle.GradleProjectComponent;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.gradle.testing.BaseGradleTest;
import io.quarkus.gradle.tooling.ToolingUtils;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;

class QuarkusApplicationToolingLocalExtensionTest extends BaseGradleTest {

    private static final String LOCAL_CONDITIONAL = "local-conditional";
    private static final String LOCAL_DEV = "local-dev";
    private static final String EXTERNAL_CONDITIONAL = "external-conditional";

    @Test
    void resolvesLocalExtensionVariantsAndExternalDescriptorsByMode() throws Exception {
        writeFixture();

        request("NORMAL");
        ModelRequest<ToolingPairedModels> normal = request("NORMAL");
        request("DEVELOPMENT");
        ModelRequest<ToolingPairedModels> development = request("DEVELOPMENT");
        request("TEST");
        ModelRequest<ToolingPairedModels> test = request("TEST");

        assertThat(artifactIds(normal.model().applicationModel()))
                .contains(
                        "runtime-ext",
                        "deployment-ext",
                        LOCAL_CONDITIONAL,
                        "local-conditional-deployment",
                        "external-extension",
                        "external-extension-deployment",
                        EXTERNAL_CONDITIONAL,
                        "external-conditional-deployment")
                .doesNotContain(LOCAL_DEV);
        assertThat(artifactIds(development.model().applicationModel()))
                .contains(LOCAL_CONDITIONAL, LOCAL_DEV, EXTERNAL_CONDITIONAL, "compile-only-lib");
        assertThat(artifactIds(test.model().applicationModel()))
                .contains(LOCAL_CONDITIONAL, EXTERNAL_CONDITIONAL)
                .doesNotContain(LOCAL_DEV);

        ResolvedDependency runtimeExtension = dependency(development.model().applicationModel(), "runtime-ext");
        assertThat(runtimeExtension.isDirect()).isTrue();
        assertThat(runtimeExtension.isRuntimeCp()).isTrue();
        assertThat(runtimeExtension.isDeploymentCp()).isTrue();
        assertThat(runtimeExtension.isWorkspaceModule()).isTrue();
        assertThat(runtimeExtension.isReloadable()).isFalse();

        ResolvedDependency deploymentExtension = dependency(development.model().applicationModel(), "deployment-ext");
        assertThat(deploymentExtension.isRuntimeCp()).isFalse();
        assertThat(deploymentExtension.isDeploymentCp()).isTrue();
        assertThat(deploymentExtension.isWorkspaceModule()).isFalse();

        ResolvedDependency compileOnly = dependency(development.model().applicationModel(), "compile-only-lib");
        assertThat(compileOnly.isFlagSet(io.quarkus.maven.dependency.DependencyFlags.COMPILE_ONLY)).isTrue();
        assertThat(compileOnly.isRuntimeCp()).isTrue();
        assertThat(compileOnly.isDeploymentCp()).isTrue();
        assertThat(compileOnly.isWorkspaceModule()).isTrue();

        GradleProjectComponent runtimeComponent = component(development.model().sidecar(), ":runtime-ext");
        assertThat(runtimeComponent.getRoles()).contains(EXTENSION_RUNTIME);
        GradleProjectComponent deploymentComponent = component(development.model().sidecar(), ":deployment-ext");
        assertThat(deploymentComponent.getRoles()).contains(EXTENSION_DEPLOYMENT);
        GradleProjectComponent compileOnlyComponent = component(development.model().sidecar(), ":compile-only-lib");
        assertThat(compileOnlyComponent.getClasspathAssociations()).contains(COMPILE_ONLY);

        assertThat(normal.taskPaths()).isEmpty();
        assertThat(development.taskPaths()).isEmpty();
        assertThat(test.taskPaths()).isEmpty();
        assertConfigurationCacheReused(normal.output());
        assertConfigurationCacheReused(development.output());
        assertConfigurationCacheReused(test.output());
    }

    @Test
    void resolvesIncludedBuildExtensionRuntimeAndDeploymentVariants() throws Exception {
        writeIncludedBuildFixture();

        request("DEVELOPMENT");
        ModelRequest<ToolingPairedModels> result = request("DEVELOPMENT");
        ApplicationModel model = result.model().applicationModel();

        ResolvedDependency runtimeExtension = dependency(model, "included-runtime-ext");
        assertThat(runtimeExtension.isDirect()).isTrue();
        assertThat(runtimeExtension.isRuntimeCp()).isTrue();
        assertThat(runtimeExtension.isDeploymentCp()).isTrue();
        assertThat(runtimeExtension.isWorkspaceModule()).isTrue();
        assertThat(runtimeExtension.isReloadable()).isFalse();

        ResolvedDependency deploymentExtension = dependency(model, "included-deployment-ext");
        assertThat(deploymentExtension.isRuntimeCp()).isFalse();
        assertThat(deploymentExtension.isDeploymentCp()).isTrue();
        assertThat(deploymentExtension.isWorkspaceModule()).isFalse();

        assertThat(component(result.model().sidecar(), ":included-extension:included-runtime-ext").getRoles())
                .contains(EXTENSION_RUNTIME);
        assertThat(component(result.model().sidecar(), ":included-extension:included-deployment-ext").getRoles())
                .contains(EXTENSION_DEPLOYMENT);
        assertThat(result.taskPaths()).isEmpty();
        assertConfigurationCacheReused(result.output());
    }

    @Test
    void providerModelMatchesSerializedTaskModelAfterExplicitPrerequisites() throws Exception {
        writeFixture();

        var taskResult = buildResultWithIsolatedProjects(
                ":classes",
                ":quarkusApplicationDevModel");
        assertTaskOutcomes(taskResult, SUCCESS, ":classes", ":quarkusApplicationDevModel");
        ApplicationModel taskModel = ToolingUtils.deserializeAppModel(
                testProjectDir.resolve("build/quarkus/application-model/quarkus-application-dev-model.dat"));

        request("DEVELOPMENT");
        ModelRequest<ToolingPairedModels> provider = request("DEVELOPMENT");

        assertThat(sharedGraphFacts(provider.model())).isEqualTo(sharedGraphFacts(taskModel));
        assertThat(workspaceFacts(provider.model().applicationModel().getApplicationModule()))
                .isEqualTo(workspaceFacts(taskModel.getApplicationModule()));
        assertThat(dependency(provider.model().applicationModel(), "compile-only-lib").isReloadable()).isFalse();
        assertThat(dependency(taskModel, "compile-only-lib").isReloadable()).isTrue();
        assertThat(provider.model().sidecar().getCorrelation().getCanonicalGraphFacts())
                .isNotEmpty();
        assertThat(provider.taskPaths()).isEmpty();
        assertConfigurationCacheReused(provider.output());
    }

    private void writeFixture() throws IOException {
        Path repository = testProjectDir.resolve("repo");
        writeExternalExtensionRepository(repository);
        writeFile("settings.gradle", """
                rootProject.name = 'tooling-local-extension'
                include 'runtime-ext', 'deployment-ext', 'compile-only-lib'
                """);
        writeFile("build.gradle", """
                %s
                apply plugin: 'io.quarkus.application'

                group = 'org.acme'
                version = '1.0'

                repositories {
                    maven { url = uri('repo') }
                }

                dependencies {
                    implementation project(':runtime-ext')
                    implementation 'org.acme:external-extension:1.0'
                    implementation 'org.condition:present:1.0'
                    compileOnly project(':compile-only-lib')
                }
                """.formatted(buildscriptClasspath()));
        writeFile("runtime-ext/build.gradle", """
                %s
                apply plugin: 'java-library'
                apply plugin: 'io.quarkus.extension'

                group = 'org.acme'
                version = '1.0'

                repositories {
                    mavenLocal()
                    mavenCentral()
                    maven { url = uri('../repo') }
                }

                quarkusExtension {
                    disableValidation = true
                    deploymentModule = 'deployment-ext'
                    conditionalDependencies = ['org.acme:local-conditional::jar:1.0']
                    conditionalDevDependencies = ['org.acme:local-dev::jar:1.0']
                }
                """.formatted(buildscriptClasspath()));
        writeFile("deployment-ext/build.gradle", """
                %s
                apply plugin: 'io.quarkus.extension.deployment'

                group = 'org.acme'
                version = '1.0'

                repositories {
                    mavenLocal()
                    mavenCentral()
                }
                """.formatted(buildscriptClasspath()));
        writeFile("compile-only-lib/build.gradle", """
                plugins {
                    id 'java-library'
                }

                group = 'org.acme'
                version = '1.0'
                """);
        writeJavaSource("", "ApplicationValue");
        writeJavaSource("runtime-ext", "RuntimeExtension");
        writeJavaSource("deployment-ext", "DeploymentExtension");
        writeJavaSource("compile-only-lib", "CompileOnlyValue");
        writeFile("compile-only-lib/src/main/resources/compile-only.properties", "compile-only=true\n");
    }

    private void writeIncludedBuildFixture() throws IOException {
        writeFile("settings.gradle", """
                rootProject.name = 'tooling-included-extension'
                includeBuild 'included-extension'
                """);
        writeFile("build.gradle", """
                %s
                apply plugin: 'io.quarkus.application'

                group = 'org.acme'
                version = '1.0'

                dependencies {
                    implementation 'org.acme.included:included-runtime-ext:1.0'
                }
                """.formatted(buildscriptClasspath()));
        writeFile("included-extension/settings.gradle", """
                rootProject.name = 'included-extension'
                include 'included-runtime-ext', 'included-deployment-ext'
                """);
        writeFile("included-extension/included-runtime-ext/build.gradle", """
                %s
                apply plugin: 'java-library'
                apply plugin: 'io.quarkus.extension'

                group = 'org.acme.included'
                version = '1.0'

                quarkusExtension {
                    disableValidation = true
                    deploymentModule = 'included-deployment-ext'
                }
                """.formatted(buildscriptClasspath()));
        writeFile("included-extension/included-deployment-ext/build.gradle", """
                %s
                apply plugin: 'io.quarkus.extension.deployment'

                group = 'org.acme.included'
                version = '1.0'
                """.formatted(buildscriptClasspath()));
        writeJavaSource("", "ApplicationValue");
        writeJavaSource("included-extension/included-runtime-ext", "IncludedRuntimeExtension");
        writeJavaSource("included-extension/included-deployment-ext", "IncludedDeploymentExtension");
    }

    private void writeJavaSource(String projectName, String className) throws IOException {
        String projectPrefix = projectName.isEmpty() ? "" : projectName + "/";
        writeFile(projectPrefix + "src/main/java/org/acme/" + className + ".java", """
                package org.acme;

                public final class %s {
                }
                """.formatted(className));
    }

    private ModelRequest<ToolingPairedModels> request(String mode) {
        ByteArrayOutputStream standardOutput = new ByteArrayOutputStream();
        ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();
        List<String> taskPaths = new CopyOnWriteArrayList<>();
        try (ProjectConnection connection = connection()) {
            BuildActionExecuter<ToolingPairedModels> request = connection.action(new ToolingPairedModelsAction(mode))
                    .withArguments(arguments())
                    .setStandardOutput(standardOutput)
                    .setStandardError(errorOutput);
            request.addProgressListener(event -> {
                if (event instanceof TaskStartEvent taskStart) {
                    taskPaths.add(taskStart.getDescriptor().getTaskPath());
                }
            }, OperationType.TASK);
            ToolingPairedModels models = request.run();
            return new ModelRequest<>(models, List.copyOf(taskPaths),
                    standardOutput.toString(StandardCharsets.UTF_8)
                            + System.lineSeparator()
                            + errorOutput.toString(StandardCharsets.UTF_8));
        }
    }

    private ProjectConnection connection() {
        GradleConnector connector = GradleConnector.newConnector()
                .forProjectDirectory(testProjectDir.toFile())
                .useGradleUserHomeDir(GradleUserHomeLookup.gradleUserHome());
        String requestedVersion = System.getProperty("quarkus-test-gradle-wrapper-version");
        if (requestedVersion != null) {
            connector.useGradleVersion(requestedVersion);
        }
        return connector.connect();
    }

    private static String[] arguments() {
        return new String[] {
                CONFIGURATION_CACHE,
                ISOLATED_PROJECTS,
                STACKTRACE,
                "--info",
                "-Dorg.gradle.console=plain",
                "-Dquarkus.analytics.disabled=true"
        };
    }

    private static GradleProjectComponent component(GradleApplicationModelSidecar sidecar, String buildTreePath) {
        return sidecar.getProjectComponents().stream()
                .filter(component -> component.getProjectIdentity().getBuildTreePath().equals(buildTreePath))
                .findFirst()
                .orElseThrow();
    }

    private static ResolvedDependency dependency(ApplicationModel model, String artifactId) {
        return model.getDependencies().stream()
                .filter(dependency -> dependency.getArtifactId().equals(artifactId))
                .findFirst()
                .orElseThrow();
    }

    private static List<String> artifactIds(ApplicationModel model) {
        return model.getDependencies().stream()
                .map(ResolvedDependency::getArtifactId)
                .toList();
    }

    private static Map<String, DependencyFact> sharedDependencyFacts(ApplicationModel model) {
        return model.getDependencies().stream()
                .collect(Collectors.toMap(
                        ArtifactCoords::toCompactCoords,
                        dependency -> new DependencyFact(
                                dependency.getFlags() & ~DependencyFlags.RELOADABLE,
                                dependency.getWorkspaceModule() == null
                                        ? null
                                        : dependency.getWorkspaceModule().getId().toString()),
                        (left, right) -> left,
                        TreeMap::new));
    }

    private static SharedGraphFacts sharedGraphFacts(ToolingPairedModels models) {
        return new SharedGraphFacts(
                sharedDependencyFacts(models.applicationModel()),
                withoutReloadableFlag(models.sidecar().getCorrelation().getCanonicalGraphFacts()));
    }

    private static SharedGraphFacts sharedGraphFacts(ApplicationModel model) {
        return new SharedGraphFacts(
                sharedDependencyFacts(model),
                withoutReloadableFlag(GradleModelCorrelationSupport.canonicalGraphFacts(model)));
    }

    private static List<String> withoutReloadableFlag(List<String> canonicalGraphFacts) {
        List<String> result = new ArrayList<>(canonicalGraphFacts.size());
        for (String fact : canonicalGraphFacts) {
            if (fact.startsWith("workspace-edge|")) {
                result.add(fact);
                continue;
            }
            String[] elements = fact.split("\\|", 4);
            int flags = Integer.parseInt(elements[2]) & ~DependencyFlags.RELOADABLE;
            result.add(elements[0] + '|' + elements[1] + '|' + flags + '|' + elements[3]);
        }
        result.sort(Comparator.naturalOrder());
        return List.copyOf(result);
    }

    private static WorkspaceFact workspaceFacts(WorkspaceModule workspaceModule) {
        return new WorkspaceFact(
                workspaceModule.getId().toString(),
                absolutePath(workspaceModule.getModuleDir()),
                absolutePath(workspaceModule.getBuildDir()),
                workspaceModule.getSourceClassifiers().stream().sorted().toList());
    }

    private static String absolutePath(File file) {
        return file == null ? null : file.toPath().toAbsolutePath().normalize().toString();
    }

    private static String buildscriptClasspath() {
        return """
                buildscript {
                    dependencies {
                        classpath files(%s)
                    }
                }
                """.formatted(TestKitPluginClasspath.implementationClasspath().stream()
                .map(File::getAbsolutePath)
                .sorted(Comparator.naturalOrder())
                .map(QuarkusApplicationToolingLocalExtensionTest::singleQuotedGroovyString)
                .collect(Collectors.joining(", ")));
    }

    private static String singleQuotedGroovyString(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    private static void writeExternalExtensionRepository(Path repository) throws IOException {
        writeMavenArtifact(repository, "org.condition", "present", null);
        writeMavenArtifact(repository, "org.acme", LOCAL_CONDITIONAL, """
                dependency-condition=org.condition\\:present
                deployment-artifact=org.acme\\:local-conditional-deployment\\:1.0
                """);
        writeMavenArtifact(repository, "org.acme", "local-conditional-deployment", null);
        writeMavenArtifact(repository, "org.acme", LOCAL_DEV, null);
        writeMavenArtifact(repository, "org.acme", "external-extension", """
                conditional-dependencies=org.acme\\:external-conditional\\:\\:jar\\:1.0
                deployment-artifact=org.acme\\:external-extension-deployment\\:1.0
                """);
        writeMavenArtifact(repository, "org.acme", "external-extension-deployment", null);
        writeMavenArtifact(repository, "org.acme", EXTERNAL_CONDITIONAL, """
                dependency-condition=org.condition\\:present
                deployment-artifact=org.acme\\:external-conditional-deployment\\:1.0
                """);
        writeMavenArtifact(repository, "org.acme", "external-conditional-deployment", null);
    }

    private static void writeMavenArtifact(Path repository, String groupId, String artifactId,
            String extensionDescriptor) throws IOException {
        Path artifactDirectory = repository.resolve(groupId.replace('.', '/')).resolve(artifactId).resolve("1.0");
        Files.createDirectories(artifactDirectory);
        String baseName = artifactId + "-1.0";
        writeFile(artifactDirectory.resolve(baseName + ".pom"), """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>%s</groupId>
                  <artifactId>%s</artifactId>
                  <version>1.0</version>
                </project>
                """.formatted(groupId, artifactId));
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(
                artifactDirectory.resolve(baseName + ".jar")))) {
            if (extensionDescriptor != null) {
                jar.putNextEntry(new JarEntry("META-INF/quarkus-extension.properties"));
                jar.write(extensionDescriptor.getBytes(StandardCharsets.UTF_8));
                jar.closeEntry();
            }
        }
    }

    private record ModelRequest<T>(T model, List<String> taskPaths, String output) {
    }

    private record DependencyFact(int flags, String workspaceModuleId) {
    }

    private record SharedGraphFacts(
            Map<String, DependencyFact> dependencies,
            List<String> canonicalGraphFacts) {
    }

    private record WorkspaceFact(
            String id,
            String moduleDirectory,
            String buildDirectory,
            List<String> sourceClassifiers) {
    }
}
