package io.quarkus.gradle.application.internal.modelgen;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.CompileClasspath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.model.DefaultApplicationModel;
import io.quarkus.gradle.model.pom.DeclaredDependencyEnrichmentMode;
import io.quarkus.gradle.model.pom.DeclaredDepsResult;
import io.quarkus.gradle.model.pom.KnownPomResolver;
import io.quarkus.gradle.model.pom.PomClosureResult;
import io.quarkus.gradle.model.pom.PomClosureResultCodec;
import io.quarkus.gradle.model.pom.StrictDependencyDataCollector;
import io.quarkus.gradle.model.tasks.SerializedApplicationModel;
import io.quarkus.gradle.model.tasks.TaskInputFingerprint;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.runtime.LaunchMode;

@DisableCachingByDefault(because = "The serialized application model contains resolved file-system paths and is not relocatable")
public abstract class GenerateModelTask extends DefaultTask {

    private final ResolvedClasspath compileOnlyClasspath;
    private final ResolvedClasspath deploymentClasspath;

    public GenerateModelTask() {
        compileOnlyClasspath = getObjects().newInstance(ResolvedClasspath.class);
        deploymentClasspath = getObjects().newInstance(ResolvedClasspath.class);
        getLocalClassOutputArtifacts().convention(Set.of());
        getLocalResourceOutputArtifacts().convention(Set.of());
        getLocalClassOutputMetadata().convention(List.of());
        getLocalResourceOutputMetadata().convention(List.of());
        getTestClassesDirectoryPaths().convention(List.of());
        getTestResourcesDirectoryPaths().convention(List.of());
        getTestSourceDirectoryPaths().convention(List.of());
        getTestResourceSourceDirectoryPaths().convention(List.of());
        getDeclaredDependencyEnrichmentMode().convention(DeclaredDependencyEnrichmentMode.NONE);
    }

    @Inject
    protected abstract ObjectFactory getObjects();

    @Inject
    protected abstract ProviderFactory getProviderFactory();

    @Input
    public abstract Property<LaunchMode> getLaunchMode();

    @Input
    public abstract Property<String> getProjectGroup();

    @Input
    public abstract Property<String> getProjectName();

    @Input
    public abstract Property<String> getProjectVersion();

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getProjectBuildFile();

    @Internal
    public abstract DirectoryProperty getProjectDirectory();

    @Internal
    public abstract DirectoryProperty getBuildDirectory();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getApplicationClassesDirectories();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getApplicationResourcesDirectories();

    @Input
    public abstract ListProperty<String> getApplicationSourceDirectoryPaths();

    @Input
    public abstract ListProperty<String> getApplicationResourceSourceDirectoryPaths();

    @Input
    public abstract ListProperty<String> getTestClassesDirectoryPaths();

    @Input
    public abstract ListProperty<String> getTestResourcesDirectoryPaths();

    @Input
    public abstract ListProperty<String> getTestSourceDirectoryPaths();

    @Input
    public abstract ListProperty<String> getTestResourceSourceDirectoryPaths();

    @CompileClasspath
    public abstract ConfigurableFileCollection getOriginalClasspath();

    @CompileClasspath
    public abstract ConfigurableFileCollection getDeploymentClasspathFiles();

    @Internal
    public abstract SetProperty<ResolvedArtifactResult> getLocalClassOutputArtifacts();

    @Internal
    public abstract SetProperty<ResolvedArtifactResult> getLocalResourceOutputArtifacts();

    @Input
    public abstract ListProperty<String> getLocalClassOutputMetadata();

    @Input
    public abstract ListProperty<String> getLocalResourceOutputMetadata();

    @CompileClasspath
    public abstract ConfigurableFileCollection getLocalComponentOutputFiles();

    @Nested
    public abstract ResolvedClasspath getPlatformConfiguration();

    @Nested
    public abstract ResolvedClasspath getAppClasspath();

    @Nested
    public ResolvedClasspath getDeploymentClasspath() {
        return deploymentClasspath;
    }

    @Nested
    public ResolvedClasspath getCompileOnlyClasspath() {
        return compileOnlyClasspath;
    }

    @Nested
    public abstract PlatformInfo getPlatformInfo();

    @Input
    public abstract ListProperty<String> getMavenLocalRepositoryRoots();

    @Input
    public abstract Property<DeclaredDependencyEnrichmentMode> getDeclaredDependencyEnrichmentMode();

    @Internal
    public Map<String, String> getMavenModelSystemProperties() {
        return getProviderFactory().systemPropertiesPrefixedBy("").get();
    }

    @Input
    public String getMavenModelSystemPropertiesFingerprint() {
        return getDeclaredDependencyEnrichmentMode().get() == DeclaredDependencyEnrichmentMode.SELECTED_MODULE_POMS
                ? TaskInputFingerprint.ofMap(getMavenModelSystemProperties())
                : "";
    }

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getPomClosureFile();

    @OutputFile
    public abstract RegularFileProperty getApplicationModel();

    @TaskAction
    public void execute() throws IOException {
        ApplicationModelBuilder modelBuilder = new ApplicationModelAssembler()
                .assemble(new GradleApplicationModelInputCollector().collect(this));
        ResolvedDependencyBuilder appArtifact = modelBuilder.getApplicationArtifact();

        if (getDeclaredDependencyEnrichmentMode().get() == DeclaredDependencyEnrichmentMode.SELECTED_MODULE_POMS) {
            Map<ArtifactKey, DeclaredDepsResult> declaredDependencies = collectExternalDeclaredDependencies();
            StrictDependencyDataCollector.setDirectDeps(appArtifact, modelBuilder, declaredDependencies, getLogger());
            for (ResolvedDependencyBuilder dep : modelBuilder.getDependencies()) {
                StrictDependencyDataCollector.setDirectDeps(dep, modelBuilder, declaredDependencies, getLogger());
            }
        }

        DefaultApplicationModel model = modelBuilder.build();
        SerializedApplicationModel.write(model, getApplicationModel().get().getAsFile().toPath());
    }

    private List<File> mavenLocalRepositoryRoots() {
        return getMavenLocalRepositoryRoots().get().stream()
                .filter(root -> !root.isBlank())
                .map(File::new)
                .toList();
    }

    private Map<ArtifactKey, DeclaredDepsResult> collectExternalDeclaredDependencies() throws IOException {
        PomClosureResult pomClosure = PomClosureResultCodec.read(getPomClosureFile().get().getAsFile().toPath());
        var collector = new StrictDependencyDataCollector(
                KnownPomResolver.fromPomClosure(pomClosure.resolvedPoms(), pomClosure.missingPoms(),
                        mavenLocalRepositoryRoots()),
                this::getMavenModelSystemProperties);
        return new HashMap<>(collector.collectExternalDeclaredDependencies(getLogger(),
                StrictDependencyDataCollector.externalModuleDeclaredDependencyInputs(Stream.concat(
                        getAppClasspath().getResolvedArtifacts().get().stream(),
                        getDeploymentClasspath().getResolvedArtifacts().get().stream()).toList())));
    }

}
