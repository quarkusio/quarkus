package io.quarkus.gradle.application.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.quarkus.gradle.application.internal.codegen.CodegenOperations;
import io.quarkus.gradle.application.internal.codegen.CodegenRequest;
import io.quarkus.runtime.LaunchMode;

class QuarkusApplicationGenerateCodeTaskTest {

    @TempDir
    Path directory;

    @Test
    void createsCodegenRequestFromTaskProperties() throws Exception {
        QuarkusApplicationGenerateCodeTask task = task("quarkusApplicationGenerateCode");
        Path sourceParent = directory.resolve("src/main");
        Path classpathJar = directory.resolve("lib/runtime.jar");
        Files.createDirectories(sourceParent);
        Files.createDirectories(classpathJar.getParent());
        Files.createFile(classpathJar);
        Files.createDirectories(sourceParent.resolve("resources"));
        Files.writeString(sourceParent.resolve("resources/application.properties"),
                "quarkus.generate-code.grpc.scan-for-proto=org.acme:library\n");

        configureRequiredProperties(task);
        task.getLaunchMode().set(LaunchMode.NORMAL);
        task.getCodegenProviders().set(List.of("grpc"));
        task.getCodegenInputNames().set(List.of("proto"));
        task.getSourceParentDirectories().from(sourceParent);
        task.getConfigurationSourceDirectories().from(sourceParent.resolve("resources"));
        task.getClasspath().from(classpathJar);
        task.getQuarkusBuildProperties().put("quarkus.codegen.test", "true");

        CodegenRequest request = task.codegenRequest();

        assertThat(request.appModel()).isEqualTo(directory.resolve("app-model.dat"));
        assertThat(request.launchMode()).isEqualTo(LaunchMode.NORMAL);
        assertThat(request.sourceParentDirectories()).containsExactly(sourceParent.toFile());
        assertThat(request.generatedSourcesDirectory()).isEqualTo(directory.resolve("generated"));
        assertThat(request.buildDirectory()).isEqualTo(directory.resolve("build"));
        assertThat(request.projectDisplayName()).isEqualTo("codegen-app");
        assertThat(request.codegenProviders()).containsExactly("grpc");
        assertThat(request.codegenInputNames()).containsExactly("proto");
        assertThat(request.classpath()).containsExactly(classpathJar);
        assertThat(request.effectiveConfig().fullValues())
                .containsEntry("quarkus.application.name", "codegen-app")
                .containsEntry("quarkus.application.version", "1.0")
                .containsEntry("quarkus.codegen.test", "true");
        assertThat(request.buildSystemProperties())
                .containsEntry("quarkus.application.name", "codegen-app")
                .containsEntry("quarkus.codegen.test", "true")
                .containsEntry("quarkus.generate-code.grpc.scan-for-proto", "org.acme:library");
    }

    @ParameterizedTest
    @EnumSource(value = LaunchMode.class, names = { "NORMAL", "DEVELOPMENT", "TEST" })
    void usesLaunchModeProfileForCodegen(LaunchMode launchMode) throws Exception {
        QuarkusApplicationGenerateCodeTask task = task("quarkusApplicationGenerateCode");
        Path resources = directory.resolve("src/main/resources");
        Files.createDirectories(resources);
        Files.writeString(resources.resolve("application.properties"), """
                quarkus.codegen.profile-value=base
                %prod.quarkus.codegen.profile-value=prod
                %dev.quarkus.codegen.profile-value=dev
                %test.quarkus.codegen.profile-value=test
                """);
        configureRequiredProperties(task);
        task.getLaunchMode().set(launchMode);
        task.getConfigurationSourceDirectories().from(resources);

        CodegenRequest request = task.codegenRequest();

        assertThat(request.launchMode()).isEqualTo(launchMode);
        assertThat(request.effectiveConfig().fullValues())
                .containsEntry("quarkus.codegen.profile-value", launchMode.getDefaultProfile());
        assertThat(request.buildSystemProperties())
                .containsEntry("quarkus.codegen.profile-value", launchMode.getDefaultProfile());
    }

    @Test
    void delegatesExecutionToConfiguredOperations() {
        QuarkusApplicationGenerateCodeTask task = task("quarkusApplicationGenerateCode");
        configureRequiredProperties(task);
        task.getLaunchMode().set(LaunchMode.NORMAL);
        RecordingCodegenOperations operations = new RecordingCodegenOperations();
        task.getOperations().set(operations);

        task.generateCode();

        assertThat(operations.request).isNotNull();
        assertThat(operations.request.generatedSourcesDirectory()).isEqualTo(directory.resolve("generated"));
    }

    private QuarkusApplicationGenerateCodeTask task(String name) {
        Project project = ProjectBuilder.builder().withProjectDir(directory.toFile()).build();
        return project.getTasks().register(name, QuarkusApplicationGenerateCodeTask.class).get();
    }

    private void configureRequiredProperties(QuarkusApplicationGenerateCodeTask task) {
        task.getApplicationModel().set(directory.resolve("app-model.dat").toFile());
        task.getGeneratedOutputDirectory().set(directory.resolve("generated").toFile());
        task.getBuildDirectory().set(directory.resolve("build").toFile());
        task.getApplicationName().set("codegen-app");
        task.getApplicationVersion().set("1.0");
        task.getQuarkusBuildProperties().convention(Map.of());
        task.getCodegenProviders().convention(List.of());
        task.getCodegenInputNames().convention(List.of());
        task.getGradlePropertyPrefixes().convention(List.of());
        task.getGradlePropertyNames().convention(List.of());
        task.getSystemPropertyPrefixes().convention(List.of());
        task.getSystemPropertyNames().convention(List.of());
        task.getEnvironmentVariablePrefixes().convention(List.of());
        task.getEnvironmentVariableNames().convention(List.of());
    }

    private static final class RecordingCodegenOperations implements CodegenOperations {
        private CodegenRequest request;

        @Override
        public void generate(CodegenRequest request) {
            this.request = request;
        }
    }
}
