package io.quarkus.gradle.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.bootstrap.resolver.AppModelResolverException;

public class QuarkusBuild extends QuarkusTask {

    private boolean uberJar;

    private List<String> ignoredEntries = new ArrayList<>();

    public QuarkusBuild() {
        super("Quarkus builds a runner jar based on the build jar");
    }

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
        final Properties realProperties = getBuildSystemProperties(appArtifact);

        boolean clear = false;
        if (uberJar && System.getProperty("quarkus.package.uber-jar") == null) {
            System.setProperty("quarkus.package.uber-jar", "true");
            clear = true;
        }
        try (CuratedApplication appCreationContext = QuarkusBootstrap.builder(appArtifact.getPath())
                .setBaseClassLoader(getClass().getClassLoader())
                .setAppModelResolver(modelResolver)
                .setTargetDirectory(getProject().getBuildDir().toPath())
                .setBaseName(extension().finalName())
                .setBuildSystemProperties(realProperties)
                .setAppArtifact(appArtifact)
                .setLocalProjectDiscovery(false)
                .setIsolateDeployment(true)
                //.setConfigDir(extension().outputConfigDirectory().toPath())
                //.setTargetDirectory(extension().outputDirectory().toPath())
                .build().bootstrap()) {

            appCreationContext.createAugmentor().createProductionApplication();

        } catch (BootstrapException e) {
            throw new GradleException("Failed to build a runnable JAR", e);
        } finally {
            if (clear) {
                System.clearProperty("quarkus.package.uber-jar");
            }
        }
    }
}
