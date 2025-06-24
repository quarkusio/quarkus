package io.quarkus.gradle.tasks;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.process.JavaForkOptions;
import org.gradle.workers.ProcessWorkerSpec;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

import io.quarkus.gradle.extension.QuarkusPluginExtension;
import io.quarkus.utilities.OS;

public abstract class QuarkusTask extends DefaultTask {
    private static final List<String> WORKER_BUILD_FORK_OPTIONS = List.of("quarkus.", "platform.quarkus.");

    private final transient QuarkusPluginExtension extension;
    protected final File projectDir;
    protected final File buildDir;

    QuarkusTask(String description) {
        this(description, false);
    }

    QuarkusTask(String description, boolean configurationCacheCompatible) {
        setDescription(description);
        setGroup("quarkus");
        this.extension = getProject().getExtensions().findByType(QuarkusPluginExtension.class);
        this.projectDir = getProject().getProjectDir();
        this.buildDir = getProject().getLayout().getBuildDirectory().getAsFile().get();

        // Calling this method tells Gradle that it should not fail the build. Side effect is that the configuration
        // cache will be at least degraded, but the build will not fail.
        if (!configurationCacheCompatible) {
            notCompatibleWithConfigurationCache("The Quarkus Plugin isn't compatible with the configuration cache");
        }
    }

    @Inject
    protected abstract WorkerExecutor getWorkerExecutor();

    QuarkusPluginExtension extension() {
        return extension;
    }

    WorkQueue workQueue(Map<String, String> configMap, List<Action<? super JavaForkOptions>> forkOptionsSupplier) {
        WorkerExecutor workerExecutor = getWorkerExecutor();

        // Use process isolation by default, unless Gradle's started with its debugging system property or the
        // system property `quarkus.gradle-worker.no-process` is set to `true`.
        if (Boolean.getBoolean("org.gradle.debug") || Boolean.getBoolean("quarkus.gradle-worker.no-process")) {
            return workerExecutor.classLoaderIsolation();
        }

        return workerExecutor.processIsolation(processWorkerSpec -> configureProcessWorkerSpec(processWorkerSpec,
                configMap, forkOptionsSupplier));
    }

    private void configureProcessWorkerSpec(ProcessWorkerSpec processWorkerSpec, Map<String, String> configMap,
            List<Action<? super JavaForkOptions>> customizations) {
        JavaForkOptions forkOptions = processWorkerSpec.getForkOptions();
        customizations.forEach(a -> a.execute(forkOptions));

        // Propagate user.dir to load config sources that use it (instead of the worker user.dir)
        String userDir = configMap.get("user.dir");
        if (userDir != null) {
            forkOptions.systemProperty("user.dir", userDir);
        }

        String quarkusWorkerMaxHeap = System.getProperty("quarkus.gradle-worker.max-heap");
        if (quarkusWorkerMaxHeap != null && forkOptions.getAllJvmArgs().stream().noneMatch(arg -> arg.startsWith("-Xmx"))) {
            forkOptions.jvmArgs("-Xmx" + quarkusWorkerMaxHeap);
        }

        // Pass all environment variables
        forkOptions.environment(System.getenv());

        if (OS.determineOS() == OS.WINDOWS) {
            // On Windows, gRPC code generation is sometimes(?) unable to find "java.exe". Feels (not proven) that
            // the grpc code generation tool looks up "java.exe" instead of consulting the 'JAVA_HOME' environment.
            // Might be, that Gradle's process isolation "loses" some information down to the worker process, so add
            // both JAVA_HOME and updated PATH environment from the 'java' executable chosen by Gradle (could be from
            // a different toolchain than the one running the build, in theory at least).
            // Linux is fine though, so no need to add a hack for Linux.
            String java = forkOptions.getExecutable();
            Path javaBinPath = Paths.get(java).getParent().toAbsolutePath();
            String javaBin = javaBinPath.toString();
            String javaHome = javaBinPath.getParent().toString();
            forkOptions.environment("JAVA_HOME", javaHome);
            forkOptions.environment("PATH", javaBin + File.pathSeparator + System.getenv("PATH"));
        }

        // It's kind of a "very big hammer" here, but this way we ensure that all necessary properties
        // "quarkus.*" from all configuration sources
        // are (forcefully) used in the Quarkus build - even properties defined on the QuarkusPluginExtension.
        // This prevents that settings from e.g. a application.properties takes precedence over an explicit
        // setting in Gradle project properties, the Quarkus extension or even via the environment or system
        // properties.
        // see https://github.com/quarkusio/quarkus/issues/33321 why not all properties are passed as system properties
        // Note that we MUST NOT mess with the system properties of the JVM running the build! And that is the
        // main reason why build and code generation happen in a separate process.
        configMap.entrySet().stream()
                .filter(e -> WORKER_BUILD_FORK_OPTIONS.stream().anyMatch(e.getKey().toLowerCase()::startsWith))
                .forEach(e -> forkOptions.systemProperty(e.getKey(), e.getValue()));

        // populate worker classpath with additional content?
        // or maybe remove some dependencies from the plugin and make those exclusively available to the worker?
        // processWorkerSpec.getClasspath().from();
    }
}
