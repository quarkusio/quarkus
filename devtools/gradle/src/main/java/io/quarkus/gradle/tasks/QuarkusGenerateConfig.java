package io.quarkus.gradle.tasks;

import java.io.File;

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
import io.quarkus.runner.bootstrap.GenerateConfigTask;

public class QuarkusGenerateConfig extends QuarkusTask {

    private String file = "application.properties.example";

    public QuarkusGenerateConfig() {
        super("Generates an example config file");
    }

    @Optional
    @Input
    public String getFile() {
        return file;
    }

    @Option(description = "The name of the file to generate", option = "file")
    public void setFile(String file) {
        this.file = file;
    }

    @TaskAction
    public void buildQuarkus() {
        getLogger().lifecycle("generating example config");

        final AppArtifact appArtifact = extension().getAppArtifact();
        final AppModelResolver modelResolver = extension().resolveAppModel();
        if (extension().resourcesDir().isEmpty()) {
            throw new GradleException("No resources directory, cannot create application.properties");
        }
        File target = extension().resourcesDir().iterator().next();

        String name = file;
        if (name == null || name.isEmpty()) {
            name = "application.properties.example";
        }
        try (CuratedApplication bootstrap = QuarkusBootstrap.builder(getProject().getBuildDir().toPath())
                .setMode(QuarkusBootstrap.Mode.PROD)
                .setAppModelResolver(modelResolver)
                .setBuildSystemProperties(getBuildSystemProperties(appArtifact))
                .build()
                .bootstrap()) {
            GenerateConfigTask ct = new GenerateConfigTask(new File(target, name).toPath());
            ct.run(bootstrap);
            getLogger().lifecycle("Generated config file " + name);
        } catch (BootstrapException e) {
            throw new RuntimeException(e);
        }
    }
}
