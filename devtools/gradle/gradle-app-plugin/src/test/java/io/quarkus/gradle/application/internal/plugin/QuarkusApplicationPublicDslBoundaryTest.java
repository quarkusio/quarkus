package io.quarkus.gradle.application.internal.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.gradle.application.QuarkusApplicationPlugin;
import io.quarkus.gradle.application.dsl.QuarkusAotJarOutput;
import io.quarkus.gradle.application.dsl.QuarkusApplicationBuilds;
import io.quarkus.gradle.application.dsl.QuarkusApplicationExtension;
import io.quarkus.gradle.application.dsl.QuarkusApplicationJvmStartupArchive;
import io.quarkus.gradle.application.dsl.QuarkusApplicationJvmTestSuite;
import io.quarkus.gradle.application.dsl.QuarkusApplicationTests;

class QuarkusApplicationPublicDslBoundaryTest {

    private static final List<Class<?>> COORDINATOR_AWARE_DSL_TYPES = List.of(
            QuarkusApplicationExtension.class,
            QuarkusApplicationBuilds.class,
            QuarkusAotJarOutput.class,
            QuarkusApplicationJvmStartupArchive.class,
            QuarkusApplicationTests.class,
            QuarkusApplicationJvmTestSuite.class);

    @Test
    void removedHelperIsNotLoadableAndPublicDslSignaturesDoNotExposeInternalTypes() {
        assertThatThrownBy(() -> Class.forName("io.quarkus.gradle.application.dsl.PluginInternalHelper"))
                .isInstanceOf(ClassNotFoundException.class);

        for (Class<?> type : COORDINATOR_AWARE_DSL_TYPES) {
            for (Constructor<?> constructor : type.getConstructors()) {
                assertThat(constructor.getParameterTypes())
                        .as(constructor.toString())
                        .noneMatch(QuarkusApplicationPublicDslBoundaryTest::isInternalType);
            }
            for (Method method : type.getMethods()) {
                if (method.getDeclaringClass() == Object.class) {
                    continue;
                }
                assertThat(method.getReturnType())
                        .as(method.toString())
                        .matches(returnType -> !isInternalType(returnType));
                assertThat(method.getParameterTypes())
                        .as(method.toString())
                        .noneMatch(QuarkusApplicationPublicDslBoundaryTest::isInternalType);
            }
        }
        assertThat(DslLifecycleCoordinator.class.getConstructors()).isEmpty();
        assertThat(Modifier.isPublic(DslLifecycleCoordinator.class.getDeclaredConstructors()[0].getModifiers()))
                .isFalse();
    }

    @Test
    void projectRuntimeSurfaceDoesNotExposeTheCoordinator() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);

        assertThat(project.getExtensions().getExtraProperties().getProperties().values())
                .noneMatch(DslLifecycleCoordinator.class::isInstance);
        assertThat(project.getExtensions().getByName("quarkusApplication"))
                .isNotInstanceOf(DslLifecycleCoordinator.class);
        assertThat(project.getTasks()).noneMatch(DslLifecycleCoordinator.class::isInstance);
    }

    @Test
    void removedHelperCannotBeCompiledAgainstThePluginClasspath(@TempDir Path temporaryDirectory) throws IOException {
        Path source = temporaryDirectory.resolve("RemovedHelperProbe.java");
        Files.writeString(source, """
                import io.quarkus.gradle.application.dsl.PluginInternalHelper;

                final class RemovedHelperProbe {
                    private final Class<?> helper = PluginInternalHelper.class;
                }
                """);
        Path classes = Files.createDirectory(temporaryDirectory.resolve("classes"));
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager files = ToolProvider.getSystemJavaCompiler()
                .getStandardFileManager(diagnostics, null, null)) {
            var units = files.getJavaFileObjects(source);
            boolean compiled = ToolProvider.getSystemJavaCompiler()
                    .getTask(null, files, diagnostics,
                            List.of("-classpath", System.getProperty("java.class.path"), "-d", classes.toString()),
                            null, units)
                    .call();

            assertThat(compiled).isFalse();
            assertThat(diagnostics.getDiagnostics())
                    .extracting(diagnostic -> diagnostic.getMessage(null))
                    .anyMatch(message -> message.contains("PluginInternalHelper"));
        }
    }

    private static boolean isInternalType(Class<?> type) {
        Package typePackage = type.getPackage();
        return typePackage != null
                && typePackage.getName().startsWith("io.quarkus.gradle.application.internal.");
    }
}
