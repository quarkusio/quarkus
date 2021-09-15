package io.quarkus.gradle.tasks;

import java.io.File;
import java.util.Collections;

import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.CompileClasspath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.ApplicationModel;
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

    /**
     * Create a dependency on classpath resolution. This makes sure included build are build this task runs.
     *
     * @return resolved compile classpath
     */
    @CompileClasspath
    public FileCollection getClasspath() {
        return QuarkusGradleUtils.getSourceSet(getProject(), SourceSet.MAIN_SOURCE_SET_NAME).getCompileClasspath();
    }

    @Option(description = "The name of the file to generate", option = "file")
    public void setFile(String file) {
        this.file = file;
    }

    @TaskAction
    public void buildQuarkus() {
        getLogger().lifecycle("generating example config");

        final ApplicationModel appModel = extension().getApplicationModel();
        if (extension().resourcesDir().isEmpty()) {
            throw new GradleException("No resources directory, cannot create application.properties");
        }
        File target = extension().resourcesDir().iterator().next();

        String name = file;
        if (name == null || name.isEmpty()) {
            name = "application.properties.example";
        }
        try (CuratedApplication bootstrap = QuarkusBootstrap.builder()
                .setBaseClassLoader(getClass().getClassLoader())
                .setExistingModel(appModel)
                .setTargetDirectory(getProject().getBuildDir().toPath())
                .setBaseName(extension().finalName())
                .setAppArtifact(appModel.getAppArtifact())
                .setLocalProjectDiscovery(false)
                .setIsolateDeployment(true)
                .build().bootstrap()) {
            bootstrap.runInAugmentClassLoader(GenerateConfigTask.class.getName(),
                    Collections.singletonMap(GenerateConfigTask.CONFIG_FILE, new File(target, name).toPath()));
            getLogger().lifecycle("Generated config file " + name);
        } catch (BootstrapException e) {
            throw new RuntimeException(e);
        }
    }
}
