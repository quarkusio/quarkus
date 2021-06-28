package io.quarkus.gradle.tasks;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.Convention;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.CompileClasspath;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.deployment.CodeGenerator;

public class QuarkusGenerateCode extends QuarkusTask {

    public static final String QUARKUS_GENERATED_SOURCES = "quarkus-generated-sources";
    public static final String QUARKUS_TEST_GENERATED_SOURCES = "quarkus-test-generated-sources";
    // TODO dynamically load generation provider, or make them write code directly in quarkus-generated-sources
    public static final String[] CODE_GENERATION_PROVIDER = new String[] { "grpc", "avdl", "avpr", "avsc" };
    public static final String[] CODE_GENERATION_INPUT = new String[] { "proto", "avro" };

    public static final String INIT_AND_RUN = "initAndRun";
    private Set<Path> sourcesDirectories;
    private Consumer<Path> sourceRegistrar = (p) -> {
    };
    private boolean test = false;

    public QuarkusGenerateCode() {
        super("Performs Quarkus pre-build preparations, such as sources generation");
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

    @InputFiles
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
        return QuarkusGradleUtils.getSourceSet(getProject(), generatedSourceSetName).getJava().getOutputDir();
    }

    @TaskAction
    public void prepareQuarkus() {
        getLogger().lifecycle("preparing quarkus application");

        final AppArtifact appArtifact = extension().getAppArtifact();
        appArtifact.setPaths(QuarkusGradleUtils.getOutputPaths(getProject()));

        final AppModelResolver modelResolver = extension().getAppModelResolver();
        final Properties realProperties = getBuildSystemProperties(appArtifact);

        Path buildDir = getProject().getBuildDir().toPath();
        try (CuratedApplication appCreationContext = QuarkusBootstrap.builder()
                .setBaseClassLoader(getClass().getClassLoader())
                .setAppModelResolver(modelResolver)
                .setTargetDirectory(buildDir)
                .setBaseName(extension().finalName())
                .setBuildSystemProperties(realProperties)
                .setAppArtifact(appArtifact)
                .setLocalProjectDiscovery(false)
                .setIsolateDeployment(true)
                .build().bootstrap()) {

            final Convention convention = getProject().getConvention();
            JavaPluginConvention javaConvention = convention.findPlugin(JavaPluginConvention.class);
            if (javaConvention != null) {
                final String generateSourcesDir = test ? QUARKUS_TEST_GENERATED_SOURCES : QUARKUS_GENERATED_SOURCES;
                final SourceSet generatedSources = javaConvention.getSourceSets().findByName(generateSourcesDir);
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

                Optional<Method> initAndRun = Arrays.stream(codeGenerator.getMethods())
                        .filter(m -> m.getName().equals(INIT_AND_RUN))
                        .findAny();
                if (!initAndRun.isPresent()) {
                    throw new GradleException("Failed to find " + INIT_AND_RUN + " method in " + CodeGenerator.class.getName());
                }
                initAndRun.get().invoke(null, deploymentClassLoader,
                        PathsCollection.from(sourcesDirectories),
                        paths.iterator().next(),
                        buildDir,
                        sourceRegistrar,
                        appCreationContext.getAppModel(),
                        realProperties);

            }
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
}
