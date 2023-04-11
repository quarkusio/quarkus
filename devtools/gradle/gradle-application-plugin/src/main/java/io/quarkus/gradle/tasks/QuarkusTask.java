package io.quarkus.gradle.tasks;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Supplier;

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

    private QuarkusPluginExtension extension;

    QuarkusTask(String description) {
        setDescription(description);
        setGroup("quarkus");
    }

    @Inject
    protected abstract WorkerExecutor getWorkerExecutor();

    QuarkusPluginExtension extension() {
        if (extension == null) {
            extension = getProject().getExtensions().findByType(QuarkusPluginExtension.class);
        }
        return extension;
    }

    WorkQueue workQueue(EffectiveConfig effectiveConfig, Supplier<List<Action<? super JavaForkOptions>>> forkOptionsActions) {
        WorkerExecutor workerExecutor = getWorkerExecutor();

        // Use process isolation by default, unless Gradle's started with its debugging system property or the
        // system property `quarkus.gradle-worker.no-process is set to `true`.
        if (Boolean.getBoolean("org.gradle.debug") || Boolean.getBoolean("quarkus.gradle-worker.no-process")) {
            return workerExecutor.classLoaderIsolation();
        }

        return workerExecutor.processIsolation(processWorkerSpec -> configureProcessWorkerSpec(processWorkerSpec,
                effectiveConfig, forkOptionsActions.get()));
    }

    private void configureProcessWorkerSpec(ProcessWorkerSpec processWorkerSpec, EffectiveConfig effectiveConfig,
            List<Action<? super JavaForkOptions>> customizations) {
        JavaForkOptions forkOptions = processWorkerSpec.getForkOptions();

        customizations.forEach(a -> a.execute(forkOptions));

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

        // It's kind of a "very big hammer" here, but this way we ensure that all 'quarkus.*' properties from
        // all configuration sources are (forcefully) used in the Quarkus build - even properties defined on the
        // QuarkusPluginExtension.
        // This prevents that settings from e.g. a application.properties takes precedence over an explicit
        // setting in Gradle project properties, the Quarkus extension or even via the environment or system
        // properties.
        // Note that we MUST NOT mess with the system properties of the JVM running the build! And that is the
        // main reason why build and code generation happen in a separate process.
        effectiveConfig.configMap().entrySet().stream().filter(e -> e.getKey().startsWith("quarkus."))
                .forEach(e -> forkOptions.systemProperty(e.getKey(), e.getValue()));

        // populate worker classpath with additional content?
        // or maybe remove some dependencies from the plugin and make those exclusively available to the worker?
        // processWorkerSpec.getClasspath().from();
    }
}
