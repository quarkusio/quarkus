package io.quarkus.gradle;

import java.io.File;
import java.util.Set;
import java.util.regex.Pattern;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

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
                    .getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getOutput().getClassesDirs().getAsPath();

        return new File(outputDirectory);
    }

    public File outputConfigDirectory() {
        if (outputConfigDirectory == null) {
            outputConfigDirectory = project.getConvention().getPlugin(JavaPluginConvention.class)
                    .getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getOutput().getResourcesDir().getAbsolutePath();
        }
        return new File(outputConfigDirectory);
    }

    public Set<File> sourceDir() {
        if (sourceDir == null) {
            sourceDir = project.getConvention().getPlugin(JavaPluginConvention.class)
                    .getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getAllJava().getSourceDirectories().getAsPath();
        }
        return project.getLayout().files(sourceDir.split(Pattern.quote(File.pathSeparator))).getFiles();
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
                .getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getResources().getSrcDirs();
    }

    public AppArtifact getAppArtifact() {
        return new AppArtifact(project.getGroup().toString(), project.getName(),
                project.getVersion().toString());
    }

    public AppModelResolver resolveAppModel() {
        return new AppModelGradleResolver(project);
    }
}
