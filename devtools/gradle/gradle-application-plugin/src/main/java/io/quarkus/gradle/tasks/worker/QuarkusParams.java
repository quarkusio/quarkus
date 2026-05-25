package io.quarkus.gradle.tasks.worker;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.workers.WorkParameters;

import io.quarkus.bootstrap.model.ApplicationModel;

public interface QuarkusParams extends WorkParameters {
    DirectoryProperty getTargetDirectory();

    MapProperty<String, String> getBuildSystemProperties();

    /**
     * The {@code quarkus.*} / {@code platform.quarkus.*} system properties the daemon set as
     * fork options for this submission. Callers populate this with the same map they pass to
     * {@code workQueue(map, ...)}, so {@link QuarkusWorker#resetQuarkusSystemProperties()} can
     * re-establish that exact set on the worker JVM and clear any {@code quarkus.*} property
     * left over from a previous task on the same pooled worker.
     *
     * <p>
     * This map does not seed
     * {@link io.quarkus.bootstrap.app.QuarkusBootstrap#setBuildSystemProperties};
     * {@link #getBuildSystemProperties()} does, and so feeds the on-disk
     * {@code build-system.properties}. Keeping the two maps separate leaves that artifact
     * unchanged.
     */
    MapProperty<String, String> getForkedSystemProperties();

    Property<String> getBaseName();

    Property<ApplicationModel> getAppModel();

    Property<String> getGradleVersion();
}
