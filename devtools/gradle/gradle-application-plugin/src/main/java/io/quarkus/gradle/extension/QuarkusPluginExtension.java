package io.quarkus.gradle.extension;

import static io.quarkus.runtime.LaunchMode.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.jvm.tasks.Jar;
import org.gradle.process.JavaForkOptions;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.gradle.AppModelGradleResolver;
import io.quarkus.gradle.dsl.Manifest;
import io.quarkus.gradle.tasks.AbstractQuarkusExtension;
import io.quarkus.gradle.tasks.QuarkusGradleUtils;
import io.quarkus.gradle.tooling.ToolingUtils;
import io.quarkus.runtime.LaunchMode;

public abstract class QuarkusPluginExtension extends AbstractQuarkusExtension {
    // TODO dynamically load generation provider, or make them write code directly in quarkus-generated-sources
    public static final String[] CODE_GENERATION_PROVIDER = new String[] { "grpc", "avdl", "avpr", "avsc" };
    public static final String[] CODE_GENERATION_INPUT = new String[] { "proto", "avro" };
    private final SourceSetExtension sourceSetExtension;

    private final Property<Boolean> cacheLargeArtifacts;
    private final Property<Boolean> cleanupBuildOutput;
    private final ListProperty<String> codeGenerationProviders;
    private final ListProperty<String> codeGenerationInputs;

    public QuarkusPluginExtension(Project project) {
        super(project);

        this.cleanupBuildOutput = project.getObjects().property(Boolean.class)
                .convention(true);
        this.cacheLargeArtifacts = project.getObjects().property(Boolean.class)
                .convention(!System.getenv().containsKey("CI"));
        this.codeGenerationProviders = project.getObjects().listProperty(String.class)
                .convention(List.of(CODE_GENERATION_PROVIDER));
        this.codeGenerationInputs = project.getObjects().listProperty(String.class)
                .convention(List.of(CODE_GENERATION_INPUT));

        this.sourceSetExtension = new SourceSetExtension();
    }

    public Manifest getManifest() {
        return manifest();
    }

    @SuppressWarnings("unused")
    public void manifest(Action<Manifest> action) {
        action.execute(this.getManifest());
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

    public void setCodeGenerationInputs(List<String> codeGenerationInputs) {
        this.codeGenerationInputs.set(codeGenerationInputs);
    }

    /**
     * The directories of code generation inputs, only needed if using a customer extension that provides its own code
     * generator.
     */
    public ListProperty<String> getCodeGenerationInputs() {
        return codeGenerationInputs;
    }

    public void setCodeGenerationProviders(List<String> codeGenerationProviders) {
        this.codeGenerationProviders.set(codeGenerationProviders);
    }

    /**
     * The identifiers of the code generation providers, only needed if using a customer extension that provides its own code
     * generator.
     */
    public ListProperty<String> getCodeGenerationProviders() {
        return codeGenerationProviders;
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

    public static FileCollection combinedOutputSourceDirs(Project project) {
        ConfigurableFileCollection classesDirs = project.files();
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        classesDirs.from(sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getOutput().getClassesDirs());
        classesDirs.from(sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME).getOutput().getClassesDirs());
        return classesDirs;
    }

    public Set<File> combinedOutputSourceDirs() {
        Set<File> sourcesDirs = new LinkedHashSet<>();
        SourceSetContainer sourceSets = getSourceSets();
        sourcesDirs.addAll(sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getOutput().getClassesDirs().getFiles());
        sourcesDirs.addAll(sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME).getOutput().getClassesDirs().getFiles());
        return sourcesDirs;
    }

    public AppModelResolver getAppModelResolver() {
        return getAppModelResolver(NORMAL);
    }

    public AppModelResolver getAppModelResolver(LaunchMode mode) {
        return new AppModelGradleResolver(project, mode);
    }

    public ApplicationModel getApplicationModel() {
        return getApplicationModel(NORMAL);
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
     *
     * @param fileCollection the collection of files present in the directory
     * @return result returns the last file
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

    public ListProperty<String> getCachingRelevantProperties() {
        return cachingRelevantProperties;
    }

    public void set(String name, @Nullable String value) {
        quarkusBuildProperties.put(addQuarkusBuildPropertyPrefix(name), value);
    }

    public void set(String name, Provider<String> value) {
        quarkusBuildProperties.put(addQuarkusBuildPropertyPrefix(name), value);
    }

    private String addQuarkusBuildPropertyPrefix(String name) {
        return String.format("quarkus.%s", name);
    }
}
