package io.quarkus.gradle.extension;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.tasks.Jar;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.gradle.ModelParameter;
import io.quarkus.bootstrap.model.gradle.impl.ModelParameterImpl;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.gradle.AppModelGradleResolver;
import io.quarkus.gradle.builder.GradleApplicationModelBuilder;
import io.quarkus.gradle.tasks.QuarkusGradleUtils;
import io.quarkus.runtime.LaunchMode;

public class QuarkusPluginExtension {

    private final Project project;

    private File outputDirectory;

    private String finalName;

    private File sourceDir;

    private File workingDir;

    private File outputConfigDirectory;

    private final SourceSetExtension sourceSetExtension;

    public QuarkusPluginExtension(Project project) {
        this.project = project;
        this.sourceSetExtension = new SourceSetExtension();
    }

    public void beforeTest(Test task) {
        try {
            final Map<String, Object> props = task.getSystemProperties();

            final ApplicationModel appModel = getApplicationModel(LaunchMode.TEST);
            final Path serializedModel = QuarkusGradleUtils.serializeAppModel(appModel, task, true);
            props.put(BootstrapConstants.SERIALIZED_TEST_APP_MODEL, serializedModel.toString());

            StringJoiner outputSourcesDir = new StringJoiner(",");
            for (File outputSourceDir : combinedOutputSourceDirs()) {
                outputSourcesDir.add(outputSourceDir.getAbsolutePath());
            }
            props.put(BootstrapConstants.OUTPUT_SOURCES_DIR, outputSourcesDir.toString());

            // Identify the folder containing the sources associated with this test task
            String fileList = getSourceSets().stream()
                    .filter(sourceSet -> Objects.equals(
                            task.getTestClassesDirs().getAsPath(),
                            sourceSet.getOutput().getClassesDirs().getAsPath()))
                    .flatMap(sourceSet -> sourceSet.getOutput().getClassesDirs().getFiles().stream())
                    .filter(File::exists)
                    .distinct()
                    .map(testSrcDir -> String.format("%s:%s",
                            project.relativePath(testSrcDir),
                            project.relativePath(outputDirectory())))
                    .collect(Collectors.joining(","));
            task.environment(BootstrapConstants.TEST_TO_MAIN_MAPPINGS, fileList);

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
            final SourceSet mainSourceSet = getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
            if (mainSourceSet != null) {
                final String classesPath = QuarkusGradleUtils.getClassesDir(mainSourceSet, jarTask.getTemporaryDir(), false);
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
            outputDirectory = getLastFile(getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getOutput()
                    .getClassesDirs());
        }
        return outputDirectory;
    }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = new File(outputDirectory);
    }

    public File outputConfigDirectory() {
        if (outputConfigDirectory == null) {
            outputConfigDirectory = getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getOutput()
                    .getResourcesDir();
        }
        return outputConfigDirectory;
    }

    public void setOutputConfigDirectory(String outputConfigDirectory) {
        this.outputConfigDirectory = new File(outputConfigDirectory);
    }

    public File sourceDir() {
        if (sourceDir == null) {
            sourceDir = getLastFile(getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getAllJava()
                    .getSourceDirectories());
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

    public void sourceSets(Action<? super SourceSetExtension> action) {
        action.execute(this.sourceSetExtension);
    }

    public SourceSetExtension sourceSetExtension() {
        return sourceSetExtension;
    }

    public Set<File> resourcesDir() {
        return getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getResources().getSrcDirs();
    }

    public Set<File> combinedOutputSourceDirs() {
        Set<File> sourcesDirs = new LinkedHashSet<>();
        sourcesDirs.addAll(getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getOutput().getClassesDirs().getFiles());
        sourcesDirs.addAll(getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME).getOutput().getClassesDirs().getFiles());
        return sourcesDirs;
    }

    public AppModelResolver getAppModelResolver() {
        return getAppModelResolver(LaunchMode.NORMAL);
    }

    public AppModelResolver getAppModelResolver(LaunchMode mode) {
        return new AppModelGradleResolver(project, getApplicationModel(mode));
    }

    public ApplicationModel getApplicationModel() {
        return getApplicationModel(LaunchMode.NORMAL);
    }

    public ApplicationModel getApplicationModel(LaunchMode mode) {
        return create(project, mode);
    }

    private ApplicationModel create(Project project, LaunchMode mode) {
        final ModelParameter params = new ModelParameterImpl();
        params.setMode(mode.toString());
        return (ApplicationModel) new GradleApplicationModelBuilder().buildAll(ApplicationModel.class.getName(), params,
                project);
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

    /**
     * Convenience method to get the source sets associated with the current project.
     *
     * @return the source sets associated with the current project.
     */
    private SourceSetContainer getSourceSets() {
        return project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets();
    }
}
