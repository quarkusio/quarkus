package io.quarkus.gradle;

import java.io.File;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.resolver.AppModelResolver;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public class QuarkusPluginExtension {

    private final Project project;

    private String outputDirectory;

    private String finalName;

    private String sourceDir;

    private String workingDir;

    private String outputConfigDirectory;

    public QuarkusPluginExtension(Project project) {
        this.project = project;
    }

    public File outputDirectory() {
        if (outputDirectory == null)
            outputDirectory = project.getConvention().getPlugin(JavaPluginConvention.class)
                    .getSourceSets().getByName("main").getOutput().getClassesDirs().getAsPath();

        return new File(outputDirectory);
    }

    public File outputConfigDirectory() {
        if (outputConfigDirectory == null) {
            outputConfigDirectory = project.getConvention().getPlugin(JavaPluginConvention.class)
                    .getSourceSets().getByName("main").getOutput().getResourcesDir().getAbsolutePath();
        }
        return new File(outputConfigDirectory);
    }

    public File sourceDir() {
        if (sourceDir == null) {
            sourceDir = project.getConvention().getPlugin(JavaPluginConvention.class)
                    .getSourceSets().getByName("main").getAllJava().getSourceDirectories().getAsPath();
        }
        return new File(sourceDir);
    }

    public File workingDir() {
        if (workingDir == null) {
            workingDir = outputDirectory().getPath();
        }

        return new File(workingDir);
    }

    public String finalName() {
        if (finalName == null || finalName.length() == 0) {
            this.finalName = project.getName() + "-" + project.getVersion();
        }
        return finalName;
    }

    public Set<File> resourcesDir() {
        return project.getConvention().getPlugin(JavaPluginConvention.class)
                .getSourceSets().getByName("main").getResources().getSrcDirs();
    }

    public AppArtifact getAppArtifact() {
        return new AppArtifact(project.getGroup().toString(), project.getName(),
                project.getVersion().toString());
    }

    public AppModelResolver resolveAppModel() {
        return new AppModelGradleResolver(project);
    }
}
