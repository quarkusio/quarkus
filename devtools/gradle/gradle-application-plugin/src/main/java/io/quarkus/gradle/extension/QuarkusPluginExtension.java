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

import javax.annotation.Nullable;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.tasks.Jar;
import org.gradle.process.JavaForkOptions;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.gradle.AppModelGradleResolver;
import io.quarkus.gradle.QuarkusPlugin;
import io.quarkus.gradle.dsl.Manifest;
import io.quarkus.gradle.tasks.AbstractQuarkusExtension;
import io.quarkus.gradle.tasks.QuarkusBuild;
import io.quarkus.gradle.tasks.QuarkusGradleUtils;
import io.quarkus.gradle.tooling.ToolingUtils;
import io.quarkus.runtime.LaunchMode;

public abstract class QuarkusPluginExtension extends AbstractQuarkusExtension {
    private final SourceSetExtension sourceSetExtension;

    private final Property<Boolean> cacheLargeArtifacts;
    private final Property<Boolean> cleanupBuildOutput;

    public QuarkusPluginExtension(Project project) {
        super(project);

        this.cleanupBuildOutput = project.getObjects().property(Boolean.class)
                .convention(true);
        this.cacheLargeArtifacts = project.getObjects().property(Boolean.class)
                .convention(!System.getenv().containsKey("CI"));

        this.sourceSetExtension = new SourceSetExtension();
    }

    public Manifest getManifest() {
        return manifest();
    }

    @SuppressWarnings("unused")
    public void manifest(Action<Manifest> action) {
        action.execute(this.getManifest());
    }

    public void beforeTest(Test task) {
        try {
            final Map<String, Object> props = task.getSystemProperties();

            final ApplicationModel appModel = getApplicationModel(LaunchMode.TEST);
            final Path serializedModel = ToolingUtils.serializeAppModel(appModel, task, true);
            props.put(BootstrapConstants.SERIALIZED_TEST_APP_MODEL, serializedModel.toString());

            StringJoiner outputSourcesDir = new StringJoiner(",");
            for (File outputSourceDir : combinedOutputSourceDirs()) {
                outputSourcesDir.add(outputSourceDir.getAbsolutePath());
            }
            props.put(BootstrapConstants.OUTPUT_SOURCES_DIR, outputSourcesDir.toString());

            final SourceSetContainer sourceSets = getSourceSets();
            final SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);

            final File outputDirectoryAsFile = getLastFile(mainSourceSet.getOutput().getClassesDirs());

            Path projectDirPath = projectDir.toPath();

            // Identify the folder containing the sources associated with this test task
            String fileList = sourceSets.stream()
                    .filter(sourceSet -> Objects.equals(
                            task.getTestClassesDirs().getAsPath(),
                            sourceSet.getOutput().getClassesDirs().getAsPath()))
                    .flatMap(sourceSet -> sourceSet.getOutput().getClassesDirs().getFiles().stream())
                    .filter(File::exists)
                    .distinct()
                    .map(testSrcDir -> String.format("%s:%s",
                            projectDirPath.relativize(testSrcDir.toPath()),
                            projectDirPath.relativize(outputDirectoryAsFile.toPath())))
                    .collect(Collectors.joining(","));
            task.environment(BootstrapConstants.TEST_TO_MAIN_MAPPINGS, fileList);
            task.getLogger().debug("test dir mapping - {}", fileList);

            QuarkusBuild quarkusBuild = project.getTasks().named(QuarkusPlugin.QUARKUS_BUILD_TASK_NAME, QuarkusBuild.class)
                    .get();
            String nativeRunner = quarkusBuild.getNativeRunner().toPath().toAbsolutePath().toString();
            props.put("native.image.path", nativeRunner);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resolve deployment classpath", e);
        }
    }

    public Property<String> getFinalName() {
        return finalName;
    }

    /**
     * Whether the build output, build/*-runner[.jar] and build/quarkus-app, for other package types than the
     * currently configured one are removed, default is 'true'.
     */
    public Property<Boolean> getCleanupBuildOutput() {
        return cleanupBuildOutput;
    }

    public void setCleanupBuildOutput(boolean cleanupBuildOutput) {
        this.cleanupBuildOutput.set(cleanupBuildOutput);
    }

    /**
     * Whether large build artifacts, like uber-jar and native runners, are cached. Defaults to 'false' if the 'CI' environment
     * variable is set, otherwise defaults to 'true'.
     */
    public Property<Boolean> getCacheLargeArtifacts() {
        return cacheLargeArtifacts;
    }

    public void setCacheLargeArtifacts(boolean cacheLargeArtifacts) {
        this.cacheLargeArtifacts.set(cacheLargeArtifacts);
    }

    public String finalName() {
        return getFinalName().get();
    }

    public void setFinalName(String finalName) {
        getFinalName().set(finalName);
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
        SourceSetContainer sourceSets = getSourceSets();
        sourcesDirs.addAll(sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getOutput().getClassesDirs().getFiles());
        sourcesDirs.addAll(sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME).getOutput().getClassesDirs().getFiles());
        return sourcesDirs;
    }

    public AppModelResolver getAppModelResolver() {
        return getAppModelResolver(LaunchMode.NORMAL);
    }

    public AppModelResolver getAppModelResolver(LaunchMode mode) {
        return new AppModelGradleResolver(project, mode);
    }

    public ApplicationModel getApplicationModel() {
        return getApplicationModel(LaunchMode.NORMAL);
    }

    public ApplicationModel getApplicationModel(LaunchMode mode) {
        return ToolingUtils.create(project, mode);
    }

    /**
     * Adds an action to configure the {@code JavaForkOptions} to build a Quarkus application.
     *
     * @param action configuration action
     */
    @SuppressWarnings("unused")
    public void buildForkOptions(Action<? super JavaForkOptions> action) {
        buildForkOptions.add(action);
    }

    /**
     * Adds an action to configure the {@code JavaForkOptions} to generate Quarkus code.
     *
     * @param action configuration action
     */
    @SuppressWarnings("unused")
    public void codeGenForkOptions(Action<? super JavaForkOptions> action) {
        codeGenForkOptions.add(action);
    }

    /**
     * Returns the last file from the specified {@link FileCollection}.
     */
    public static File getLastFile(FileCollection fileCollection) {
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
        return project.getExtensions().getByType(SourceSetContainer.class);
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
            final String classesPath = QuarkusGradleUtils.getClassesDir(mainSourceSet, jarTask.getTemporaryDir(), false);
            if (classesPath != null) {
                classesDir = Paths.get(classesPath);
            }
        }
        if (classesDir == null) {
            throw new RuntimeException("Failed to locate project's classes directory");
        }
        return classesDir;
    }

    @SuppressWarnings("unused")
    public MapProperty<String, String> getQuarkusBuildProperties() {
        return quarkusBuildProperties;
    }

    public void set(String name, @Nullable String value) {
        quarkusBuildProperties.put(String.format("quarkus.%s", name), value);
    }

    public void set(String name, Property<String> value) {
        quarkusBuildProperties.put(String.format("quarkus.%s", name), value);
    }
}
