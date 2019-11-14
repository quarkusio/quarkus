package io.quarkus.gradle.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.creator.AppCreatorException;
import io.quarkus.creator.CuratedApplicationCreator;
import io.quarkus.creator.phase.augment.AugmentTask;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public class QuarkusBuild extends QuarkusTask {

    private boolean uberJar;

    private List<String> ignoredEntries = new ArrayList<>();

    public QuarkusBuild() {
        super("Quarkus builds a runner jar based on the build jar");
    }

    @Optional
    @Input
    public boolean isUberJar() {
        return uberJar;
    }

    @Option(description = "Set to true if the build task should build an uberjar", option = "uber-jar")
    public void setUberJar(boolean uberJar) {
        this.uberJar = uberJar;
    }

    @Optional
    @Input
    public List<String> getIgnoredEntries() {
        return ignoredEntries;
    }

    @Option(description = "When using the uber-jar option, this option can be used to "
            + "specify one or more entries that should be excluded from the final jar", option = "ignored-entry")
    public void setIgnoredEntries(List<String> ignoredEntries) {
        this.ignoredEntries.addAll(ignoredEntries);
    }

    @TaskAction
    public void buildQuarkus() {
        getLogger().lifecycle("building quarkus runner");

        final AppArtifact appArtifact = extension().getAppArtifact();
        final AppModelResolver modelResolver = extension().resolveAppModel();
        try {
            // this needs to be done otherwise the app artifact doesn't get a proper path
            modelResolver.resolveModel(appArtifact);
        } catch (AppModelResolverException e) {
            throw new GradleException("Failed to resolve application model " + appArtifact + " dependencies", e);
        }
        final Map<String, ?> properties = getProject().getProperties();
        final Properties realProperties = new Properties();
        for (Map.Entry<String, ?> entry : properties.entrySet()) {
            final String key = entry.getKey();
            final Object value = entry.getValue();
            if (key != null && value instanceof String && key.startsWith("quarkus.")) {
                realProperties.setProperty(key, (String) value);
            }
        }
        realProperties.putIfAbsent("quarkus.application.name", appArtifact.getArtifactId());
        realProperties.putIfAbsent("quarkus.application.version", appArtifact.getVersion());

        boolean clear = false;
        if (uberJar && System.getProperty("quarkus.package.uber-jar") == null) {
            System.setProperty("quarkus.package.uber-jar", "true");
            clear = true;
        }
        try (CuratedApplicationCreator appCreationContext = CuratedApplicationCreator.builder()
                .setModuleDir(getProject().getProjectDir().toPath())
                .setWorkDir(getProject().getBuildDir().toPath())
                .setModelResolver(modelResolver)
                .setBaseName(extension().finalName())
                .setAppArtifact(appArtifact).build()) {

            AugmentTask task = AugmentTask.builder().setBuildSystemProperties(realProperties)
                    .setAppClassesDir(extension().outputDirectory().toPath())
                    .setConfigDir(extension().outputConfigDirectory().toPath()).build();
            appCreationContext.runTask(task);

        } catch (AppCreatorException e) {
            throw new GradleException("Failed to build a runnable JAR", e);
        } finally {
            if (clear) {
                System.clearProperty("quarkus.package.uber-jar");
            }
        }
    }
}
