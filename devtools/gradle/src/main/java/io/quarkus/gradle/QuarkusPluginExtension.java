package io.quarkus.gradle;

import java.io.File;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.runtime.LaunchMode;

public class QuarkusPluginExtension {

    private final Project project;

    private File outputDirectory;

    private String finalName;

    private File sourceDir;

    private File workingDir;

    private File outputConfigDirectory;

    public QuarkusPluginExtension(Project project) {
        this.project = project;
    }

    public File outputDirectory() {
        if (outputDirectory == null) {
            outputDirectory = getLastFile(project.getConvention().getPlugin(JavaPluginConvention.class)
                    .getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getOutput().getClassesDirs());
        }
        return outputDirectory;
    }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = new File(outputDirectory);
    }

    public File outputConfigDirectory() {
        if (outputConfigDirectory == null) {
            outputConfigDirectory = project.getConvention().getPlugin(JavaPluginConvention.class)
                    .getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getOutput().getResourcesDir();
        }
        return outputConfigDirectory;
    }

    public void setOutputConfigDirectory(String outputConfigDirectory) {
        this.outputConfigDirectory = new File(outputConfigDirectory);
    }

    public File sourceDir() {
        if (sourceDir == null) {
            sourceDir = getLastFile(project.getConvention().getPlugin(JavaPluginConvention.class)
                    .getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getAllJava().getSourceDirectories());
        }
        return sourceDir;
    }

    public void setSourceDir(String sourceDir) {
        this.sourceDir = new File(sourceDir);
    }

    public File workingDir() {
        if (workingDir == null) {
            workingDir = outputDirectory();
        }
        return workingDir;
    }

    public void setWorkingDir(String workingDir) {
        this.workingDir = new File(workingDir);
    }

    public String finalName() {
        if (finalName == null || finalName.length() == 0) {
            this.finalName = String.format("%s-%s", project.getName(), project.getVersion());
        }
        return finalName;
    }

    public void setFinalName(String finalName) {
        this.finalName = finalName;
    }

    public Set<File> resourcesDir() {
        return project.getConvention().getPlugin(JavaPluginConvention.class)
                .getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getResources().getSrcDirs();
    }

    public AppArtifact getAppArtifact() {
        return new AppArtifact(project.getGroup().toString(), project.getName(),
                project.getVersion().toString());
    }

    public AppModelResolver getAppModelResolver() {
        return getAppModelResolver(LaunchMode.NORMAL);
    }

    public AppModelResolver getAppModelResolver(LaunchMode mode) {
        return new AppModelGradleResolver(project, mode);
    }

    /**
     * Returns the last file from the specified {@link FileCollection}.
     * Needed for the Scala plugin.
     */
    private File getLastFile(FileCollection fileCollection) {
        File result = null;
        for (File f : fileCollection) {
            if (result == null || f.exists()) {
                result = f;
            }
        }
        return result;
    }
}
