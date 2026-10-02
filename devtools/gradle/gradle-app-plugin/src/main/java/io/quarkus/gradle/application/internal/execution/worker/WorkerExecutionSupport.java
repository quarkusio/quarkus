package io.quarkus.gradle.application.internal.execution.worker;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.gradle.api.provider.ProviderFactory;
import org.gradle.process.JavaForkOptions;
import org.gradle.workers.ProcessWorkerSpec;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

import io.smallrye.common.os.OS;

public final class WorkerExecutionSupport {

    private static final List<String> WORKER_FORK_OPTIONS = List.of("quarkus.", "platform.quarkus.",
            "gradle.quarkus.");

    private final WorkerExecutor workerExecutor;
    private final ProviderFactory providers;
    private final ForkOptionsSnapshot forkOptions;
    private final String pathEnvironment;
    private final String gradleWorkerMaxHeap;

    public WorkerExecutionSupport(WorkerExecutor workerExecutor, ProviderFactory providers,
            ForkOptionsSnapshot forkOptions, String pathEnvironment, String gradleWorkerMaxHeap) {
        this.workerExecutor = workerExecutor;
        this.providers = providers;
        this.forkOptions = forkOptions;
        this.pathEnvironment = pathEnvironment;
        this.gradleWorkerMaxHeap = gradleWorkerMaxHeap;
    }

    public boolean isProcessIsolated() {
        return !(providers.systemProperty("org.gradle.debug").map(Boolean::parseBoolean).getOrElse(false) ||
                providers.systemProperty("gradle.quarkus.gradle-worker.no-process").map(Boolean::parseBoolean)
                        .getOrElse(false));
    }

    public WorkQueue workQueue(Map<String, String> configMap) {
        if (!isProcessIsolated()) {
            return workerExecutor.classLoaderIsolation();
        }
        return workerExecutor.processIsolation(processWorkerSpec -> configureProcessWorkerSpec(processWorkerSpec,
                configMap));
    }

    void configureProcessWorkerSpec(ProcessWorkerSpec processWorkerSpec, Map<String, String> configMap) {
        JavaForkOptions javaForkOptions = processWorkerSpec.getForkOptions();
        forkOptions.applyTo(javaForkOptions);

        String userDir = configMap.get("user.dir");
        if (userDir != null) {
            javaForkOptions.systemProperty("user.dir", userDir);
        }

        if (gradleWorkerMaxHeap != null
                && javaForkOptions.getAllJvmArgs().stream().noneMatch(arg -> arg.startsWith("-Xmx"))) {
            javaForkOptions.jvmArgs("-Xmx" + gradleWorkerMaxHeap);
        }

        if (OS.current() == OS.WINDOWS) {
            String java = javaForkOptions.getExecutable();
            Path javaBinPath = Paths.get(java).getParent().toAbsolutePath();
            String javaBin = javaBinPath.toString();
            String javaHome = javaBinPath.getParent().toString();
            javaForkOptions.environment("JAVA_HOME", javaHome);
            javaForkOptions.environment("PATH",
                    javaBin + File.pathSeparator + (pathEnvironment == null ? "" : pathEnvironment));
        } else if (pathEnvironment != null) {
            javaForkOptions.environment("PATH", pathEnvironment);
        }

        configMap.entrySet().stream()
                .filter(entry -> WORKER_FORK_OPTIONS.stream().anyMatch(entry.getKey().toLowerCase()::startsWith))
                .forEach(entry -> javaForkOptions.systemProperty(entry.getKey(), entry.getValue()));
    }
}
