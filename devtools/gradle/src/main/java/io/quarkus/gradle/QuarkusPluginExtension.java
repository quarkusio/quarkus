package io.quarkus.gradle;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.plugins.Convention;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.tasks.Jar;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.gradle.tasks.QuarkusGradleUtils;
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

    void beforeTest(Test task) {
        try {
            final Map<String, Object> props = task.getSystemProperties();

            final AppModel deploymentDeps = getAppModelResolver(LaunchMode.TEST)
                    .resolveModel(getAppArtifact());
            final Path serializedModel = QuarkusGradleUtils.serializeAppModel(deploymentDeps, task);
            props.put(BootstrapConstants.SERIALIZED_APP_MODEL, serializedModel.toString());

            final String nativeRunner = task.getProject().getBuildDir().toPath().resolve(finalName() + "-runner")
                    .toAbsolutePath()
                    .toString();
            props.put("native.image.path", nativeRunner);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resolve deployment classpath", e);
        }
    }

    public Path appJarOrClasses() {
        final Jar jarTask = (Jar) project.getTasks().findByName(JavaPlugin.JAR_TASK_NAME);
        if (jarTask == null) {
            throw new RuntimeException("Failed to locate task 'jar' in the project.");
        }
        final Provider<RegularFile> jarProvider = jarTask.getArchiveFile();
        Path classesDir = null;
        if (jarProvider.isPresent()) {
            final File f = jarProvider.get().getAsFile();
            if (f.exists()) {
                classesDir = f.toPath();
            }
        }
        if (classesDir == null) {
            final Convention convention = project.getConvention();
            JavaPluginConvention javaConvention = convention.findPlugin(JavaPluginConvention.class);
            if (javaConvention != null) {
                final SourceSet mainSourceSet = javaConvention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
                final String classesPath = QuarkusGradleUtils.getClassesDir(mainSourceSet, jarTask.getTemporaryDir());
                if (classesPath != null) {
                    classesDir = Paths.get(classesPath);
                }
            }
        }
        if (classesDir == null) {
            throw new RuntimeException("Failed to locate project's classes directory");
        }
        return classesDir;
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
