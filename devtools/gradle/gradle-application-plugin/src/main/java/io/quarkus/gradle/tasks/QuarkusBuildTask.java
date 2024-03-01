package io.quarkus.gradle.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.StopExecutionException;
import org.gradle.workers.WorkQueue;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.gradle.QuarkusPlugin;
import io.quarkus.gradle.tasks.worker.BuildWorker;
import io.quarkus.maven.dependency.GACTV;
import io.smallrye.config.Expressions;
import io.smallrye.config.SmallRyeConfig;

/**
 * Base class for the {@link QuarkusBuildDependencies}, {@link QuarkusBuildCacheableAppParts}, {@link QuarkusBuild} tasks
 */
abstract class QuarkusBuildTask extends QuarkusTask {
    private static final String QUARKUS_BUILD_DIR = "quarkus-build";
    private static final String QUARKUS_BUILD_GEN_DIR = QUARKUS_BUILD_DIR + "/gen";
    private static final String QUARKUS_BUILD_APP_DIR = QUARKUS_BUILD_DIR + "/app";
    private static final String QUARKUS_BUILD_DEP_DIR = QUARKUS_BUILD_DIR + "/dep";
    static final String QUARKUS_ARTIFACT_PROPERTIES = "quarkus-artifact.properties";
    static final String NATIVE_SOURCES = "native-sources";

    private final GACTV gactv;

    QuarkusBuildTask(String description) {
        super(description);

        gactv = new GACTV(getProject().getGroup().toString(), getProject().getName(),
                getProject().getVersion().toString());
    }

    @Inject
    protected abstract FileSystemOperations getFileSystemOperations();

    @Classpath
    public FileCollection getClasspath() {
        return extension().classpath();
    }

    @Input
    public Map<String, String> getCachingRelevantInput() {
        ListProperty<String> vars = extension().getCachingRelevantProperties();
        return extension().baseConfig().cachingRelevantProperties(vars.get());
    }

    PackageConfig.BuiltInType packageType() {
        return extension().baseConfig().packageType();
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
        BaseConfig baseConfig = extension().baseConfig();
        return baseConfig.packageConfig().outputName.orElseGet(() -> extension().finalName());
    }

    String outputDirectory() {
        BaseConfig baseConfig = extension().baseConfig();
        return baseConfig.packageConfig().outputDirectory.orElse(QuarkusPlugin.DEFAULT_OUTPUT_DIRECTORY);
    }

    private String runnerSuffix() {
        BaseConfig baseConfig = extension().baseConfig();
        return baseConfig.packageConfig().getRunnerSuffix();
    }

    ApplicationModel resolveAppModelForBuild() {
        ApplicationModel appModel;
        try {
            appModel = extension().getAppModelResolver().resolveModel(gactv);
        } catch (AppModelResolverException e) {
            throw new GradleException("Failed to resolve Quarkus application model for " + getPath(), e);
        }
        return appModel;
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
    void generateBuild() {
        Path buildDir = gradleBuildDir();
        Path genDir = genBuildDir();
        PackageConfig.BuiltInType packageType = packageType();
        getLogger().info("Building Quarkus app for package type {} in {}", packageType, genDir);

        // Need to delete app-cds.jsa specially, because it's usually read-only and Gradle's delete file-system
        // operation doesn't delete "read only" files :(
        deleteFileIfExists(genDir.resolve(outputDirectory()).resolve("app-cds.jsa"));
        getFileSystemOperations().delete(delete -> {
            // Caching and "up-to-date" checks depend on the inputs, this 'delete()' should ensure that the up-to-date
            // checks work against "clean" outputs, considering that the outputs depend on the package-type.
            delete.delete(genDir);

            // Delete directories inside Gradle's build/ dir that are going to be populated by the Quarkus build.
            switch (packageType) {
                case JAR:
                case FAST_JAR:
                    delete.delete(buildDir.resolve(nativeImageSourceJarDirName()));
                    // fall through
                case NATIVE:
                case NATIVE_SOURCES:
                    delete.delete(fastJar());
                    break;
                case LEGACY_JAR:
                case LEGACY:
                    delete.delete(buildDir.resolve("lib"));
                    break;
                case MUTABLE_JAR:
                case UBER_JAR:
                    break;
                default:
                    throw new GradleException("Unsupported package type " + packageType);
            }
        });

        ApplicationModel appModel = resolveAppModelForBuild();
        SmallRyeConfig config = extension().buildEffectiveConfiguration(appModel.getAppArtifact()).getConfig();
        Map<String, String> quarkusProperties = Expressions.withoutExpansion(() -> {
            Map<String, String> values = new HashMap<>();
            config.getValues("quarkus", String.class, String.class)
                    .forEach((key, value) -> values.put("quarkus." + key, value));
            return values;
        });

        getLogger().info("Starting Quarkus application build for package type {}", packageType);

        if (getLogger().isEnabled(LogLevel.INFO)) {
            getLogger().info("Effective properties: {}",
                    quarkusProperties.entrySet().stream()
                            .map(Object::toString)
                            .sorted()
                            .collect(Collectors.joining("\n    ", "\n    ", "")));
        }

        WorkQueue workQueue = workQueue(quarkusProperties, () -> extension().buildForkOptions);

        workQueue.submit(BuildWorker.class, params -> {
            params.getBuildSystemProperties()
                    .putAll(extension().buildSystemProperties(appModel.getAppArtifact(), quarkusProperties));
            params.getBaseName().set(extension().finalName());
            params.getTargetDirectory().set(buildDir.toFile());
            params.getAppModel().set(appModel);
            params.getGradleVersion().set(getProject().getGradle().getGradleVersion());
        });

        workQueue.await();

        // Copy built artifacts from `build/` into `build/quarkus-build/gen/`
        getFileSystemOperations().copy(copy -> {
            copy.from(buildDir);
            copy.into(genDir);
            copy.eachFile(new CopyActionDeleteNonWriteableTarget(genDir));
            switch (packageType) {
                case NATIVE:
                    copy.include(nativeRunnerFileName());
                    copy.include(nativeImageSourceJarDirName() + "/**");
                    // fall through
                case JAR:
                case FAST_JAR:
                    copy.include(outputDirectory() + "/**");
                    copy.include(QUARKUS_ARTIFACT_PROPERTIES);
                    break;
                case LEGACY_JAR:
                case LEGACY:
                    copy.include("lib/**");
                    // fall through
                case MUTABLE_JAR:
                case UBER_JAR:
                    copy.include(QUARKUS_ARTIFACT_PROPERTIES);
                    copy.include(runnerJarFileName());
                    break;
                case NATIVE_SOURCES:
                    copy.include(QUARKUS_ARTIFACT_PROPERTIES);
                    copy.include(nativeImageSourceJarDirName() + "/**");
                    break;
                default:
                    throw new GradleException("Unsupported package type " + packageType);
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
}
