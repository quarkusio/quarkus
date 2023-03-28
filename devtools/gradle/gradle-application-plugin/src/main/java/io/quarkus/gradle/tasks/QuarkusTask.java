package io.quarkus.gradle.tasks;

import java.util.List;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.process.JavaForkOptions;
import org.gradle.workers.ProcessWorkerSpec;
import org.gradle.workers.WorkerExecutor;

import io.quarkus.gradle.extension.QuarkusPluginExtension;

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

    void configureProcessWorkerSpec(ProcessWorkerSpec processWorkerSpec, EffectiveConfig effectiveConfig,
            List<Action<? super JavaForkOptions>> customizations) {
        JavaForkOptions forkOptions = processWorkerSpec.getForkOptions();

        customizations.forEach(a -> a.execute(forkOptions));

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
