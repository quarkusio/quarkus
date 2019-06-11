package io.quarkus.gradle.tasks;

import java.io.File;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.creator.AppCreator;
import io.quarkus.creator.AppCreatorException;
import io.quarkus.creator.phase.curate.CurateOutcome;
import io.quarkus.creator.phase.generateconfig.ConfigPhaseOutcome;
import io.quarkus.creator.phase.generateconfig.GenerateConfigPhase;

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
        getLogger().lifecycle("building quarkus runner");

        final AppArtifact appArtifact = extension().getAppArtifact();
        final AppModel appModel;
        final AppModelResolver modelResolver = extension().resolveAppModel();
        try {
            appModel = modelResolver.resolveModel(appArtifact);
        } catch (AppModelResolverException e) {
            throw new GradleException("Failed to resolve application model " + appArtifact + " dependencies", e);
        }
        if (extension().resourcesDir().isEmpty()) {
            throw new GradleException("No resources directory, cannot create application.properties");
        }
        File target = extension().resourcesDir().iterator().next();

        String name = file;
        if (name == null || name.isEmpty()) {
            name = "application.properties.example";
        }

        try (AppCreator appCreator = AppCreator.builder()
                // configure the build phases we want the app to go through
                .addPhase(new GenerateConfigPhase()
                        .setConfigFile(new File(target, name).toPath()))
                .setWorkDir(getProject().getBuildDir().toPath())
                .build()) {

            // push resolved application state
            appCreator.pushOutcome(CurateOutcome.builder()
                    .setAppModelResolver(modelResolver)
                    .setAppModel(appModel)
                    .build());
            appCreator.resolveOutcome(ConfigPhaseOutcome.class);
            getLogger().lifecycle("Generated config file " + name);
        } catch (AppCreatorException e) {
            throw new GradleException("Failed to generate config file", e);
        }
    }
}
