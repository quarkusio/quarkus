package io.quarkus.gradle.tasks;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.CompileClasspath;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.deployment.CodeGenerator;
import io.quarkus.paths.PathCollection;
import io.quarkus.paths.PathList;
import io.quarkus.runtime.LaunchMode;

@CacheableTask
public class QuarkusGenerateCode extends QuarkusTask {

    public static final String QUARKUS_GENERATED_SOURCES = "quarkus-generated-sources";
    public static final String QUARKUS_TEST_GENERATED_SOURCES = "quarkus-test-generated-sources";
    // TODO dynamically load generation provider, or make them write code directly in quarkus-generated-sources
    public static final String[] CODE_GENERATION_PROVIDER = new String[] { "grpc", "avdl", "avpr", "avsc" };
    public static final String[] CODE_GENERATION_INPUT = new String[] { "proto", "avro" };

    public static final String INIT_AND_RUN = "initAndRun";
    private Set<Path> sourcesDirectories;
    private Configuration compileClasspath;
    private Consumer<Path> sourceRegistrar = (p) -> {
    };
    private boolean test = false;
    private boolean devMode = false;

    public QuarkusGenerateCode() {
        super("Performs Quarkus pre-build preparations, such as sources generation");
    }

    /**
     * Create a dependency on classpath resolution. This makes sure included build are build this task runs.
     *
     * @return resolved compile classpath
     */
    @CompileClasspath
    public Configuration getClasspath() {
        return compileClasspath;
    }

    public void setCompileClasspath(Configuration compileClasspath) {
        this.compileClasspath = compileClasspath;
    }

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public Set<File> getInputDirectory() {
        Set<File> inputDirectories = new HashSet<>();

        final String inputSourceSetName = test ? SourceSet.TEST_SOURCE_SET_NAME : SourceSet.MAIN_SOURCE_SET_NAME;
        Path src = getProject().getProjectDir().toPath().resolve("src").resolve(inputSourceSetName);

        for (String input : CODE_GENERATION_INPUT) {
            Path providerSrcDir = src.resolve(input);
            if (Files.exists(providerSrcDir)) {
                inputDirectories.add(providerSrcDir.toFile());
            }
        }

        return inputDirectories;
    }

    @OutputDirectory
    public File getGeneratedOutputDirectory() {
        final String generatedSourceSetName = test ? QUARKUS_TEST_GENERATED_SOURCES : QUARKUS_GENERATED_SOURCES;
        return QuarkusGradleUtils.getSourceSet(getProject(), generatedSourceSetName).getJava().getClassesDirectory().get()
                .getAsFile();
    }

    @TaskAction
    public void prepareQuarkus() {
        LaunchMode launchMode = test ? LaunchMode.TEST : devMode ? LaunchMode.DEVELOPMENT : LaunchMode.NORMAL;
        final ApplicationModel appModel = extension().getApplicationModel(launchMode);
        final Properties realProperties = getBuildSystemProperties(appModel.getAppArtifact());

        Path buildDir = getProject().getBuildDir().toPath();
        try (CuratedApplication appCreationContext = QuarkusBootstrap.builder()
                .setBaseClassLoader(getClass().getClassLoader())
                .setExistingModel(appModel)
                .setTargetDirectory(buildDir)
                .setBaseName(extension().finalName())
                .setBuildSystemProperties(realProperties)
                .setAppArtifact(appModel.getAppArtifact())
                .setLocalProjectDiscovery(false)
                .setIsolateDeployment(true)
                .build().bootstrap()) {

            SourceSetContainer sourceSets = getProject().getExtensions().getByType(SourceSetContainer.class);
            final String generateSourcesDir = test ? QUARKUS_TEST_GENERATED_SOURCES : QUARKUS_GENERATED_SOURCES;
            final SourceSet generatedSources = sourceSets.getByName(generateSourcesDir);
            List<Path> paths = new ArrayList<>();
            generatedSources.getOutput()
                    .filter(f -> f.getName().equals(generateSourcesDir))
                    .forEach(f -> paths.add(f.toPath()));
            if (paths.isEmpty()) {
                throw new GradleException("Failed to create quarkus-generated-sources");
            }

            getLogger().debug("Will trigger preparing sources for source directory: {} buildDir: {}",
                    sourcesDirectories, getProject().getBuildDir().getAbsolutePath());

            QuarkusClassLoader deploymentClassLoader = appCreationContext.createDeploymentClassLoader();
            Class<?> codeGenerator = deploymentClassLoader.loadClass(CodeGenerator.class.getName());

            Method initAndRun;
            try {
                initAndRun = codeGenerator.getMethod(INIT_AND_RUN, QuarkusClassLoader.class, PathCollection.class,
                        Path.class, Path.class,
                        Consumer.class, ApplicationModel.class, Properties.class, String.class,
                        boolean.class);
            } catch (Exception e) {
                throw new GradleException("Quarkus code generation phase has failed", e);
            }

            initAndRun.invoke(null, deploymentClassLoader,
                    PathList.from(sourcesDirectories),
                    paths.get(0),
                    buildDir,
                    sourceRegistrar,
                    appCreationContext.getApplicationModel(),
                    realProperties,
                    launchMode.name(),
                    test);
        } catch (BootstrapException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
            throw new GradleException("Failed to generate sources in the QuarkusPrepare task", e);
        }
    }

    public void setSourcesDirectories(Set<Path> sourcesDirectories) {
        this.sourcesDirectories = sourcesDirectories;
    }

    public void setTest(boolean test) {
        this.test = test;
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }
}
