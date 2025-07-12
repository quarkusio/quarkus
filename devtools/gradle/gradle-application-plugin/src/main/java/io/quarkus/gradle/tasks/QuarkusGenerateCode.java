package io.quarkus.gradle.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.CompileClasspath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.util.GradleVersion;
import org.gradle.workers.WorkQueue;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.gradle.tasks.worker.CodeGenWorker;
import io.quarkus.gradle.tooling.ToolingUtils;
import io.quarkus.runtime.LaunchMode;

@CacheableTask
public abstract class QuarkusGenerateCode extends QuarkusTaskWithExtensionView {

    public static final String QUARKUS_GENERATED_SOURCES = "quarkus-generated-sources";
    public static final String QUARKUS_TEST_GENERATED_SOURCES = "quarkus-test-generated-sources";

    private Set<Path> sourcesDirectories;
    private FileCollection compileClasspath;

    private final LaunchMode launchMode;
    private final String inputSourceSetName;

    private final List<String> codeGenInput;

    @Inject
    public QuarkusGenerateCode(LaunchMode launchMode, String inputSourceSetName, List<String> codeGenInput) {
        super("Performs Quarkus pre-build preparations, such as sources generation", true);
        this.launchMode = launchMode;
        this.inputSourceSetName = inputSourceSetName;
        this.codeGenInput = codeGenInput;

    }

    /**
     * Create a dependency on classpath resolution. This makes sure included build are build this task runs.
     *
     * @return resolved compile classpath
     */
    @CompileClasspath
    public FileCollection getClasspath() {
        return compileClasspath;
    }

    public void setCompileClasspath(Configuration compileClasspath) {
        this.compileClasspath = compileClasspath;
    }

    @Input
    Map<String, String> getInternalTaskConfig() {
        // Necessary to distinguish the different `quarkusGenerateCode*` tasks, because the task path is _not_
        // an input to the cache key. We need to declare these properties as inputs, because those influence the
        // execution.
        // Documented here: https://docs.gradle.org/current/userguide/build_cache.html#sec:task_output_caching_inputs
        return Collections.unmodifiableMap(
                new TreeMap<>(Map.of("launchMode", launchMode.name(), "inputSourceSetName", inputSourceSetName)));
    }

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public Set<File> getInputDirectory() {
        Set<File> inputDirectories = new HashSet<>();

        Path src = projectDir.toPath().resolve("src").resolve(inputSourceSetName);

        for (String input : codeGenInput) {
            Path providerSrcDir = src.resolve(input);
            if (Files.exists(providerSrcDir)) {
                inputDirectories.add(providerSrcDir.toFile());
            }
        }

        return inputDirectories;
    }

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getApplicationModel();

    @OutputDirectory
    public abstract DirectoryProperty getGeneratedOutputDirectory();

    @TaskAction
    public void generateCode() throws IOException {
        ApplicationModel appModel = ToolingUtils.deserializeAppModel(getApplicationModel().get().getAsFile().toPath());
        Map<String, String> configMap = effectiveProvider()
                .buildEffectiveConfiguration(appModel, new HashMap<>()).getValues();

        File outputPath = getGeneratedOutputDirectory().get().getAsFile();

        getLogger().debug("Will trigger preparing sources for source directories: {} buildDir: {}",
                sourcesDirectories, buildDir.getAbsolutePath());

        WorkQueue workQueue = workQueue(configMap, getExtensionView().getCodeGenForkOptions().get());

        workQueue.submit(CodeGenWorker.class, params -> {
            params.getBuildSystemProperties().putAll(configMap);
            params.getBaseName().set(getExtensionView().getFinalName());
            params.getTargetDirectory().set(buildDir);
            params.getAppModel().set(appModel);
            params
                    .getSourceDirectories()
                    .setFrom(sourcesDirectories.stream().map(Path::toFile).collect(Collectors.toList()));
            params.getOutputPath().set(outputPath);
            params.getLaunchMode().set(launchMode);
            params.getGradleVersion().set(GradleVersion.current().getVersion());
        });

        workQueue.await();

    }

    public void setSourcesDirectories(Set<Path> sourcesDirectories) {
        this.sourcesDirectories = sourcesDirectories;
    }
}
