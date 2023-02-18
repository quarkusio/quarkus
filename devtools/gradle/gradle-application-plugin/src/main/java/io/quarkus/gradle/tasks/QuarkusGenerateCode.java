package io.quarkus.gradle.tasks;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.gradle.process.JavaForkOptions;
import org.gradle.workers.WorkQueue;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.gradle.tasks.worker.CodeGenWorker;
import io.quarkus.runtime.LaunchMode;

@CacheableTask
public abstract class QuarkusGenerateCode extends QuarkusTask {

    public static final String QUARKUS_GENERATED_SOURCES = "quarkus-generated-sources";
    public static final String QUARKUS_TEST_GENERATED_SOURCES = "quarkus-test-generated-sources";
    // TODO dynamically load generation provider, or make them write code directly in quarkus-generated-sources
    public static final String[] CODE_GENERATION_PROVIDER = new String[] { "grpc", "avdl", "avpr", "avsc" };
    public static final String[] CODE_GENERATION_INPUT = new String[] { "proto", "avro" };

    private Set<Path> sourcesDirectories;
    private Configuration compileClasspath;
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
    public void generateCode() {
        LaunchMode launchMode = test ? LaunchMode.TEST : devMode ? LaunchMode.DEVELOPMENT : LaunchMode.NORMAL;
        ApplicationModel appModel = extension().getApplicationModel(launchMode);
        EffectiveConfig effectiveConfig = extension().buildEffectiveConfiguration(appModel.getAppArtifact());

        SourceSetContainer sourceSets = getProject().getExtensions().getByType(SourceSetContainer.class);
        final String generateSourcesDir = test ? QUARKUS_TEST_GENERATED_SOURCES : QUARKUS_GENERATED_SOURCES;
        final SourceSet generatedSources = sourceSets.getByName(generateSourcesDir);
        List<File> paths = new ArrayList<>();
        generatedSources.getOutput()
                .filter(f -> f.getName().equals(generateSourcesDir))
                .forEach(paths::add);
        if (paths.isEmpty()) {
            throw new GradleException("Failed to create quarkus-generated-sources");
        }
        File outputPath = paths.get(0);

        getLogger().debug("Will trigger preparing sources for source directory: {} buildDir: {}",
                sourcesDirectories, getProject().getBuildDir().getAbsolutePath());

        WorkQueue workQueue = getWorkerExecutor().processIsolation(processWorkerSpec -> {
            JavaForkOptions forkOptions = processWorkerSpec.getForkOptions();
            extension().codeGenForkOptions.forEach(a -> a.execute(forkOptions));

            // It's kind of a "very big hammer" here, but this way we ensure that all 'quarkus.*' properties from
            // all configuration sources are (forcefully) used in the Quarkus build - even properties defined on the
            // QuarkusPluginExtension.
            // This prevents that settings from e.g. a application.properties takes precedence over an explicit
            // setting in Gradle project properties, the Quarkus extension or even via the environment or system
            // properties.
            // Note that we MUST NOT mess with the system properties of the JVM running the build! And that is the
            // main reason why build and code generation happen in a separate process.
            effectiveConfig.configMap().entrySet().stream().filter(e -> e.getKey().startsWith("quarkus."))
                    .forEach(e -> forkOptions.systemProperty(e.getKey(), e.getValue()));

            // populate worker classpath with additional content?
            // or maybe remove some dependencies from the plugin and make those exclusively available to the worker?
            // processWorkerSpec.getClasspath().from();
        });

        workQueue.submit(CodeGenWorker.class, params -> {
            params.getBuildSystemProperties().putAll(effectiveConfig.configMap());
            params.getBaseName().set(extension().finalName());
            params.getTargetDirectory().set(getProject().getBuildDir());
            params.getAppModel().set(appModel);
            params.getSourceDirectories().setFrom(sourcesDirectories.stream().map(Path::toFile).collect(Collectors.toList()));
            params.getOutputPath().set(outputPath);
            params.getLaunchMode().set(launchMode);
            params.getTest().set(test);
        });

        workQueue.await();

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
