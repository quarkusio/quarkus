package io.quarkus.gradle.application.internal.execution.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.gradle.api.Project;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.JavaExec;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.workers.ProcessWorkerSpec;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;
import org.junit.jupiter.api.Test;

import io.smallrye.common.os.OS;

class WorkerExecutionSupportTest {

    private final Project project = ProjectBuilder.builder().build();

    @Test
    void selectsIsolationModeFromGradleProperties() {
        assertThat(workerExecution(mock(WorkerExecutor.class), Map.of()).isProcessIsolated()).isTrue();
        assertThat(workerExecution(mock(WorkerExecutor.class), Map.of("org.gradle.debug", "true")).isProcessIsolated())
                .isFalse();
        assertThat(workerExecution(mock(WorkerExecutor.class),
                Map.of("gradle.quarkus.gradle-worker.no-process", "true")).isProcessIsolated()).isFalse();
    }

    @Test
    void selectsClassLoaderIsolationWhenProcessIsolationIsDisabled() {
        WorkerExecutor workerExecutor = mock(WorkerExecutor.class);
        WorkQueue classLoaderQueue = mock(WorkQueue.class);
        when(workerExecutor.classLoaderIsolation()).thenReturn(classLoaderQueue);

        WorkQueue selected = workerExecution(workerExecutor, Map.of("org.gradle.debug", "true")).workQueue(Map.of());

        assertThat(selected).isSameAs(classLoaderQueue);
        verify(workerExecutor, never()).processIsolation(any());
    }

    @Test
    void selectsProcessIsolationByDefault() {
        WorkerExecutor workerExecutor = mock(WorkerExecutor.class);
        WorkQueue processQueue = mock(WorkQueue.class);
        when(workerExecutor.processIsolation(any())).thenReturn(processQueue);

        WorkQueue selected = workerExecution(workerExecutor, Map.of()).workQueue(Map.of());

        assertThat(selected).isSameAs(processQueue);
        verify(workerExecutor, never()).classLoaderIsolation();
    }

    @Test
    void configuresProcessWorkerForkOptions() {
        JavaExec forkOptions = project.getTasks().register("workerForkOptions", JavaExec.class).get();
        String javaExecutable = javaExecutable();
        forkOptions.setExecutable(javaExecutable);
        ProcessWorkerSpec processWorkerSpec = mock(ProcessWorkerSpec.class);
        when(processWorkerSpec.getForkOptions()).thenReturn(forkOptions);
        var snapshot = new ForkOptionsSnapshot(
                List.of("-Dconfigured.argument=true"),
                Map.of("configured.property", "configured-value"),
                Map.of("CONFIGURED_ENV", "configured-value"),
                "64m",
                null,
                true,
                false,
                "UTF-8");
        var workerExecution = new WorkerExecutionSupport(mock(WorkerExecutor.class), project.getProviders(), snapshot,
                "configured-path", "512m");

        workerExecution.configureProcessWorkerSpec(processWorkerSpec, Map.of(
                "user.dir", "configured-user-dir",
                "quarkus.worker", "quarkus-value",
                "QuArKuS.mixed-worker", "mixed-value",
                "platform.quarkus.worker", "platform-value",
                "gradle.quarkus.worker", "gradle-value",
                "unrelated.worker", "unrelated-value"));

        assertThat(forkOptions.getAllJvmArgs())
                .contains("-Dconfigured.argument=true", "-Xmx512m");
        assertThat(forkOptions.getSystemProperties())
                .containsEntry("configured.property", "configured-value")
                .containsEntry("user.dir", "configured-user-dir")
                .containsEntry("quarkus.worker", "quarkus-value")
                .containsEntry("QuArKuS.mixed-worker", "mixed-value")
                .containsEntry("platform.quarkus.worker", "platform-value")
                .containsEntry("gradle.quarkus.worker", "gradle-value")
                .doesNotContainKey("unrelated.worker");
        assertThat(forkOptions.getEnvironment()).containsEntry("CONFIGURED_ENV", "configured-value");
        assertThat(forkOptions.getMinHeapSize()).isEqualTo("64m");
        assertThat(forkOptions.getEnableAssertions()).isTrue();
        assertThat(forkOptions.getDefaultCharacterEncoding()).isEqualTo("UTF-8");
        if (OS.current() == OS.WINDOWS) {
            Path javaBinPath = Paths.get(javaExecutable).getParent().toAbsolutePath();
            assertThat(forkOptions.getEnvironment())
                    .containsEntry("JAVA_HOME", javaBinPath.getParent().toString())
                    .containsEntry("PATH", javaBinPath + File.pathSeparator + "configured-path");
        } else {
            assertThat(forkOptions.getEnvironment()).containsEntry("PATH", "configured-path");
        }
    }

    @Test
    void preservesExplicitMaximumHeapSize() {
        JavaExec forkOptions = project.getTasks().register("workerExplicitHeap", JavaExec.class).get();
        forkOptions.setExecutable(javaExecutable());
        ProcessWorkerSpec processWorkerSpec = mock(ProcessWorkerSpec.class);
        when(processWorkerSpec.getForkOptions()).thenReturn(forkOptions);
        var snapshot = new ForkOptionsSnapshot(
                List.of("-Xmx768m"), Map.of(), Map.of(), null, null, false, false, null);
        var workerExecution = new WorkerExecutionSupport(mock(WorkerExecutor.class), project.getProviders(), snapshot,
                null, "512m");

        workerExecution.configureProcessWorkerSpec(processWorkerSpec, Map.of());

        assertThat(forkOptions.getAllJvmArgs())
                .contains("-Xmx768m")
                .doesNotContain("-Xmx512m");
    }

    private WorkerExecutionSupport workerExecution(WorkerExecutor workerExecutor, Map<String, String> systemProperties) {
        ProviderFactory providers = mock(ProviderFactory.class);
        when(providers.systemProperty(anyString())).thenAnswer(invocation -> project.getProviders()
                .provider(() -> systemProperties.get(invocation.getArgument(0, String.class))));
        return new WorkerExecutionSupport(workerExecutor, providers,
                new ForkOptionsSnapshot(List.of(), Map.of(), Map.of(), null, null, false, false, null), null, null);
    }

    private static String javaExecutable() {
        return Path.of(System.getProperty("java.home"), "bin", OS.current() == OS.WINDOWS ? "java.exe" : "java")
                .toString();
    }
}
