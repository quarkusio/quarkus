package io.quarkus.gradle.tasks.worker;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

import org.gradle.workers.WorkAction;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.ApplicationModel;

public abstract class QuarkusWorker<P extends QuarkusParams> implements WorkAction<P> {

    Properties buildSystemProperties() {
        Properties props = new Properties();
        props.putAll(getParameters().getBuildSystemProperties().get());
        return props;
    }

    /**
     * Scrubs stale {@code quarkus.*} / {@code platform.quarkus.*} system properties before bootstrap.
     *
     * <p>
     * Gradle pools process-isolation workers and reuses them across tasks. System properties on
     * a reused worker JVM are fixed at startup via {@code forkOptions.systemProperty(...)} and
     * stay put across submissions, so values set for one task remain visible to later ones.
     * SmallRye Config's {@code SysPropConfigSource} (ordinal 400) outranks the
     * {@code "Build system"} {@code PropertiesConfigSource} that
     * {@code BuildTimeConfigurationReader#initConfiguration} adds (ordinal 100), so
     * {@code quarkus.*} values from the first module's task leak into the second module's
     * bootstrap in a multi-module build. See https://github.com/quarkusio/quarkus/issues/54095.
     *
     * <p>
     * The reset aligns the worker's {@code quarkus.*} / {@code platform.quarkus.*} system
     * properties with the daemon's intent for this submission: the same map the daemon passes
     * to {@code forkOptions.systemProperty(...)}, exposed via
     * {@link QuarkusParams#getForkedSystemProperties()}. Any other matching property left over
     * from a previous task is cleared.
     *
     * <p>
     * The reset is one-way by design. Each submission re-establishes the right state at the top
     * of bootstrap; nothing is restored on exit. A symmetric tear-down would erase legitimate
     * inter-task state, for example properties a build step set via
     * {@link System#setProperty(String, String)} during augmentation.
     */
    void resetQuarkusSystemProperties() {
        Map<String, String> intended = getParameters().getForkedSystemProperties().getOrElse(Map.of());
        for (String key : new ArrayList<>(System.getProperties().stringPropertyNames())) {
            if ((key.startsWith("quarkus.") || key.startsWith("platform.quarkus."))
                    && !intended.containsKey(key)) {
                System.clearProperty(key);
            }
        }
        for (Map.Entry<String, String> entry : intended.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("quarkus.") || key.startsWith("platform.quarkus.")) {
                System.setProperty(key, entry.getValue());
            }
        }
    }

    CuratedApplication createAppCreationContext() throws BootstrapException {
        resetQuarkusSystemProperties();
        QuarkusParams params = getParameters();
        Path buildDir = params.getTargetDirectory().getAsFile().get().toPath();
        String baseName = params.getBaseName().get();
        ApplicationModel appModel = params.getAppModel().get();
        return QuarkusBootstrap.builder()
                .setBaseClassLoader(getClass().getClassLoader())
                .setExistingModel(appModel)
                .setTargetDirectory(buildDir)
                .setBaseName(baseName)
                .setBuildSystemProperties(buildSystemProperties())
                .setAppArtifact(appModel.getAppArtifact())
                .setLocalProjectDiscovery(false)
                .setIsolateDeployment(true)
                .setDependencyInfoProvider(() -> null)
                .build().bootstrap();
    }
}
