package io.quarkus.gradle.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.workers.WorkQueue;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.gradle.QuarkusPlugin;
import io.quarkus.gradle.tasks.worker.BuildWorker;
import io.quarkus.maven.dependency.GACTV;

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

    QuarkusBuildTask(String description) {
        super(description);
    }

    @Inject
    protected abstract FileSystemOperations getFileSystemOperations();

    @Classpath
    public FileCollection getClasspath() {
        return extension().classpath();
    }

    @Input
    public Map<String, String> getCachingRelevantInput() {
        return extension().baseConfig().quarkusProperties();
    }

    PackageConfig.BuiltInType packageType() {
        return extension().baseConfig().packageType();
    }

    Path genBuildDir() {
        return getProject().getBuildDir().toPath().resolve(QUARKUS_BUILD_GEN_DIR);
    }

    Path appBuildDir() {
        return getProject().getBuildDir().toPath().resolve(QUARKUS_BUILD_APP_DIR);
    }

    Path depBuildDir() {
        return getProject().getBuildDir().toPath().resolve(QUARKUS_BUILD_DEP_DIR);
    }

    File artifactProperties() {
        return new File(getProject().getBuildDir(), QUARKUS_ARTIFACT_PROPERTIES);
    }

    File nativeSources() {
        return new File(getProject().getBuildDir(), NATIVE_SOURCES);
    }

    /**
     * "final" location of the "fast-jar".
     */
    File fastJar() {
        return new File(getProject().getBuildDir(), outputDirectory());
    }

    /**
     * "final" location of the "uber-jar".
     */
    File runnerJar() {
        return new File(getProject().getBuildDir(), runnerJarFileName());
    }

    /**
     * "final" location of the "native" runner.
     */
    File nativeRunner() {
        return new File(getProject().getBuildDir(), nativeRunnerFileName());
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
            GACTV gactv = new GACTV(getProject().getGroup().toString(), getProject().getName(),
                    getProject().getVersion().toString());
            appModel = extension().getAppModelResolver().resolveModel(gactv);
        } catch (AppModelResolverException e) {
            throw new GradleException("Failed to resolve Quarkus application model for " + getProject().getPath(), e);
        }
        return appModel;
    }

    void generateBuild() {
        Path genDir = genBuildDir();
        PackageConfig.BuiltInType packageType = packageType();
        getLogger().info("Building Quarkus app for package type {} in {}", packageType, genDir);

        // Caching and "up-to-date" checks depend on the inputs, this 'delete()' should ensure that the up-to-date
        // checks work against "clean" outputs, considering that the outputs depend on the package-type.
        getFileSystemOperations().delete(delete -> delete.delete(genDir));
        try {
            Files.createDirectories(genDir);
        } catch (IOException e) {
            throw new GradleException("Could not create directory " + genDir, e);
        }

        ApplicationModel appModel = resolveAppModelForBuild();
        EffectiveConfig effectiveConfig = extension().buildEffectiveConfiguration(appModel.getAppArtifact());

        getLogger().info("Starting Quarkus application build for package type {}", packageType);

        if (getLogger().isEnabled(LogLevel.INFO)) {
            getLogger().info("Effective properties: {}",
                    effectiveConfig.configMap().entrySet().stream()
                            .filter(e -> e.getKey().startsWith("quarkus.")).map(e -> "" + e)
                            .sorted()
                            .collect(Collectors.joining("\n    ", "\n    ", "")));
        }

        WorkQueue workQueue = getWorkerExecutor()
                .processIsolation(processWorkerSpec -> configureProcessWorkerSpec(processWorkerSpec, effectiveConfig,
                        extension().buildForkOptions));

        workQueue.submit(BuildWorker.class, params -> {
            params.getBuildSystemProperties().putAll(effectiveConfig.configMap());
            params.getBaseName().set(extension().finalName());
            params.getTargetDirectory().set(genDir.toFile());
            params.getAppModel().set(appModel);
        });

        workQueue.await();
    }
}
