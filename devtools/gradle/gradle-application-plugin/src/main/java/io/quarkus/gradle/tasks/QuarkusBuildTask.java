package io.quarkus.gradle.tasks;

import static io.smallrye.common.expression.Expression.Flag.DOUBLE_COLON;
import static io.smallrye.common.expression.Expression.Flag.LENIENT_SYNTAX;
import static io.smallrye.common.expression.Expression.Flag.NO_SMART_BRACES;
import static io.smallrye.common.expression.Expression.Flag.NO_TRIM;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.StopExecutionException;
import org.gradle.util.GradleVersion;
import org.gradle.workers.WorkQueue;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.gradle.tasks.services.ForcedPropertieBuildService;
import io.quarkus.gradle.tasks.worker.BuildWorker;
import io.quarkus.gradle.tooling.ToolingUtils;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.smallrye.common.expression.Expression;
import io.smallrye.config.Expressions;
import io.smallrye.config.SmallRyeConfig;

/**
 * Base class for the {@link QuarkusBuildDependencies}, {@link QuarkusBuildCacheableAppParts}, {@link QuarkusBuild} tasks
 */
public abstract class QuarkusBuildTask extends QuarkusTaskWithExtensionView {
    private static final String QUARKUS_BUILD_DIR = "quarkus-build";
    private static final String QUARKUS_BUILD_GEN_DIR = QUARKUS_BUILD_DIR + "/gen";
    private static final String QUARKUS_BUILD_APP_DIR = QUARKUS_BUILD_DIR + "/app";
    private static final String QUARKUS_BUILD_DEP_DIR = QUARKUS_BUILD_DIR + "/dep";
    static final String QUARKUS_ARTIFACT_PROPERTIES = "quarkus-artifact.properties";
    static final String NATIVE_SOURCES = "native-sources";

    @Internal
    public abstract Property<ForcedPropertieBuildService> getAdditionalForcedProperties();

    QuarkusBuildTask(String description, boolean compatible) {
        super(description, compatible);
    }

    @Inject
    protected abstract FileSystemOperations getFileSystemOperations();

    @Classpath
    public FileCollection getClasspath() {
        return classpath;
    }

    private FileCollection classpath = getProject().getObjects().fileCollection();

    public void setCompileClasspath(FileCollection compileClasspath) {
        this.classpath = compileClasspath;
    }

    @Input
    public abstract MapProperty<String, String> getCachingRelevantInput();

    @Input
    public abstract Property<Boolean> getJarEnabled();

    @Input
    public abstract Property<Boolean> getNativeEnabled();

    @Input
    public abstract Property<Boolean> getNativeSourcesOnly();

    @Internal
    public abstract Property<String> getRunnerSuffix();

    @Internal
    public abstract Property<String> getRunnerName();

    @Internal
    public abstract Property<Path> getOutputDirectory();

    @Input
    public abstract Property<PackageConfig.JarConfig.JarType> getJarType();

    PackageConfig.JarConfig.JarType jarType() {
        return getJarType().get();
    }

    boolean jarEnabled() {
        return getJarEnabled().get();
    }

    boolean nativeEnabled() {
        return getNativeEnabled().get();
    }

    boolean nativeSourcesOnly() {
        return getNativeSourcesOnly().get();
    }

    Path gradleBuildDir() {
        return buildDir.toPath();
    }

    Path genBuildDir() {
        return gradleBuildDir().resolve(QUARKUS_BUILD_GEN_DIR);
    }

    Path appBuildDir() {
        return gradleBuildDir().resolve(QUARKUS_BUILD_APP_DIR);
    }

    Path depBuildDir() {
        return gradleBuildDir().resolve(QUARKUS_BUILD_DEP_DIR);
    }

    File artifactProperties() {
        return new File(buildDir, QUARKUS_ARTIFACT_PROPERTIES);
    }

    File nativeSources() {
        return new File(buildDir, NATIVE_SOURCES);
    }

    /**
     * "final" location of the "fast-jar".
     */
    File fastJar() {
        return new File(buildDir, outputDirectory());
    }

    /**
     * "final" location of the "uber-jar".
     */
    File runnerJar() {
        return new File(buildDir, runnerJarFileName());
    }

    /**
     * "final" location of the "native" runner.
     */
    File nativeRunner() {
        return new File(buildDir, nativeRunnerFileName());
    }

    String runnerJarFileName() {
        return runnerName() + ".jar";
    }

    String nativeRunnerFileName() {
        return runnerName();
    }

    String runnerName() {
        return runnerBaseName() + runnerSuffix();
    }

    String nativeImageSourceJarDirName() {
        return runnerBaseName() + "-native-image-source-jar";
    }

    String runnerBaseName() {
        return getRunnerName().get();
    }

    String outputDirectory() {
        return getOutputDirectory().get().toString();
    }

    private String runnerSuffix() {
        return getRunnerSuffix().get();

    }

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getApplicationModel();

    ApplicationModel resolveAppModelForBuild() {
        try {
            return ToolingUtils.deserializeAppModel(getApplicationModel().get().getAsFile().toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Runs the Quarkus-build in the "well known" location, Gradle's {@code build/} directory.
     *
     * <p>
     * It would be easier to run the Quarkus-build directly in {@code build/quarkus-build/gen} to have a "clean
     * target directory", but that breaks already existing Gradle builds for users, which have for example
     * {@code Dockerfile}s that rely on the fact that build artifacts are present in {@code build/}.
     *
     * <p>
     * This requires this method to
     * <ol>
     * <li>"properly" clean the directories that are going to be populated by the Quarkus build, then
     * <li>run the Quarkus build with the target directory {@code build/} and then
     * <li>populate the
     * </ol>
     */
    @SuppressWarnings("deprecation") // legacy JAR
    void generateBuild() {
        Path buildDir = gradleBuildDir();
        Path genDir = genBuildDir();
        if (nativeEnabled()) {
            if (nativeSourcesOnly()) {
                getLogger().info("Building Quarkus app for native (sources only) packaging in {}", genDir);
            } else {
                getLogger().info("Building Quarkus app for native packaging in {}", genDir);
            }
        } else {
            getLogger().info("Building Quarkus app for JAR type {} in {}", jarType(), genDir);
        }

        // Need to delete app-cds.jsa specially, because it's usually read-only and Gradle's delete file-system
        // operation doesn't delete "read only" files :(
        deleteFileIfExists(genDir.resolve(outputDirectory()).resolve("app-cds.jsa"));
        getFileSystemOperations().delete(delete -> {
            // Caching and "up-to-date" checks depend on the inputs, this 'delete()' should ensure that the up-to-date
            // checks work against "clean" outputs, considering that the outputs depend on the package-type.
            delete.delete(genDir);

            // Delete directories inside Gradle's build/ dir that are going to be populated by the Quarkus build.
            if (nativeEnabled()) {
                if (jarEnabled()) {
                    throw QuarkusBuild.nativeAndJar();
                }
                delete.delete(fastJar());
            } else if (jarEnabled()) {
                switch (jarType()) {
                    case FAST_JAR -> {
                        delete.delete(buildDir.resolve(nativeImageSourceJarDirName()));
                        delete.delete(fastJar());
                    }
                    case LEGACY_JAR -> delete.delete(buildDir.resolve("lib"));
                    case MUTABLE_JAR, UBER_JAR -> {
                    }
                }
            }
        });

        ApplicationModel appModel = resolveAppModelForBuild();
        SmallRyeConfig config = effectiveProvider()
                .buildEffectiveConfiguration(appModel, getAdditionalForcedProperties().get().getProperties())
                .getConfig();
        Map<String, String> quarkusProperties = Expressions.withoutExpansion(() -> {
            Map<String, String> values = new HashMap<>();
            for (String key : config.getMapKeys("quarkus").values()) {
                values.put(key, config.getConfigValue(key).getValue());
            }
            for (String key : config.getMapKeys("platform.quarkus").values()) {
                values.put(key, config.getConfigValue(key).getValue());
            }
            return values;
        });

        if (nativeEnabled()) {
            if (nativeSourcesOnly()) {
                getLogger().info("Starting Quarkus application build for native (sources only) packaging");
            } else {
                getLogger().info("Starting Quarkus application build for native packaging");
            }
        } else {
            getLogger().info("Starting Quarkus application build for JAR type {}", jarType());
        }

        if (getLogger().isEnabled(LogLevel.INFO)) {
            getLogger().info("Effective properties: {}",
                    quarkusProperties.entrySet().stream()
                            .map(Object::toString)
                            .sorted()
                            .collect(Collectors.joining("\n    ", "\n    ", "")));
        }

        WorkQueue workQueue = workQueue(quarkusProperties, getExtensionView().getBuildForkOptions().get());

        workQueue.submit(BuildWorker.class, params -> {
            params.getBuildSystemProperties().putAll(buildSystemProperties(appModel.getAppArtifact(), quarkusProperties));
            params.getBaseName().set(getExtensionView().getFinalName());
            params.getTargetDirectory().set(buildDir.toFile());
            params.getAppModel().set(appModel);
            params.getGradleVersion().set(GradleVersion.current().getVersion());
        });

        workQueue.await();

        // Copy built artifacts from `build/` into `build/quarkus-build/gen/`
        getFileSystemOperations().copy(copy -> {
            copy.from(buildDir);
            copy.into(genDir);
            copy.eachFile(new CopyActionDeleteNonWriteableTarget(genDir));
            if (nativeEnabled()) {
                if (jarEnabled()) {
                    throw QuarkusBuild.nativeAndJar();
                }
                if (nativeSourcesOnly()) {
                    copy.include(QUARKUS_ARTIFACT_PROPERTIES);
                    copy.include(nativeImageSourceJarDirName() + "/**");
                } else {
                    copy.include(nativeRunnerFileName());
                    copy.include(nativeImageSourceJarDirName() + "/**");
                    copy.include(outputDirectory() + "/**");
                    copy.include(QUARKUS_ARTIFACT_PROPERTIES);
                }
            } else if (jarEnabled()) {
                switch (jarType()) {
                    case FAST_JAR -> {
                        copy.include(outputDirectory() + "/**");
                        copy.include(QUARKUS_ARTIFACT_PROPERTIES);
                    }
                    case LEGACY_JAR -> {
                        copy.include("lib/**");
                        copy.include(QUARKUS_ARTIFACT_PROPERTIES);
                        copy.include(runnerJarFileName());
                    }
                    case MUTABLE_JAR, UBER_JAR -> {
                        copy.include(QUARKUS_ARTIFACT_PROPERTIES);
                        copy.include(runnerJarFileName());
                    }
                }
            }
        });
    }

    void abort(String message, Object... args) {
        getLogger().warn(message, args);
        getProject().getTasks().stream()
                .filter(t -> t != this)
                .filter(t -> !t.getState().getExecuted()).forEach(t -> {
                    t.setEnabled(false);
                });
        throw new StopExecutionException();
    }

    public static final class CopyActionDeleteNonWriteableTarget implements Action<FileCopyDetails> {
        private final Path destDir;

        public CopyActionDeleteNonWriteableTarget(Path destDir) {
            this.destDir = destDir;
        }

        @Override
        public void execute(FileCopyDetails details) {
            // Delete a pre-existing non-writeable file, otherwise a copy or sync operation would fail.
            // This situation happens for 'app-cds.jsa' files, which are created as "read only" files,
            // prefer to keep those files read-only.

            Path destFile = destDir.resolve(details.getPath());
            if (Files.exists(destFile) && !Files.isWritable(destFile)) {
                deleteFileIfExists(destFile);
            }
        }
    }

    protected static void deleteFileIfExists(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> buildSystemProperties(ResolvedDependency appArtifact, Map<String, String> quarkusProperties) {
        Map<String, String> buildSystemProperties = new HashMap<>();
        buildSystemProperties.putIfAbsent("quarkus.application.name", appArtifact.getArtifactId());
        buildSystemProperties.putIfAbsent("quarkus.application.version", appArtifact.getVersion());

        for (Map.Entry<String, String> entry : getExtensionView().getForcedProperties().get().entrySet()) {
            if (entry.getKey().startsWith("quarkus.") || entry.getKey().startsWith("platform.quarkus.")) {
                buildSystemProperties.put(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<String, String> entry : getExtensionView().getQuarkusBuildProperties().get().entrySet()) {
            if (entry.getKey().startsWith("quarkus.") || entry.getKey().startsWith("platform.quarkus.")) {
                buildSystemProperties.put(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<String, ?> entry : getExtensionView().getProjectProperties().get().entrySet()) {
            if ((entry.getKey().startsWith("quarkus.") || entry.getKey().startsWith("platform.quarkus."))
                    && entry.getValue() != null) {
                buildSystemProperties.put(entry.getKey(), entry.getValue().toString());
            }
        }

        Set<String> quarkusValues = new HashSet<>();
        quarkusValues.addAll(quarkusProperties.values());
        quarkusValues.addAll(buildSystemProperties.values());

        for (String value : quarkusValues) {
            Expression expression = Expression.compile(value, LENIENT_SYNTAX, NO_TRIM, NO_SMART_BRACES, DOUBLE_COLON);
            for (String reference : expression.getReferencedStrings()) {
                String expanded = getExtensionView().getForcedProperties().get().get(reference);
                if (expanded != null) {
                    buildSystemProperties.put(reference, expanded);
                    continue;
                }

                expanded = getExtensionView().getQuarkusBuildProperties().get().get(reference);
                if (expanded != null) {
                    buildSystemProperties.put(reference, expanded);
                    continue;
                }
                expanded = (String) getExtensionView().getProjectProperties().get().get(reference);
                if (expanded != null) {
                    buildSystemProperties.put(reference, expanded);
                }
            }
        }
        return buildSystemProperties;
    }
}
