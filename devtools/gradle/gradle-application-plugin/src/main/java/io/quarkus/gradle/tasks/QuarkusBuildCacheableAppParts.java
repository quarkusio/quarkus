package io.quarkus.gradle.tasks;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectories;
import org.gradle.api.tasks.TaskAction;

import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.gradle.QuarkusPlugin;

@CacheableTask
public abstract class QuarkusBuildCacheableAppParts extends QuarkusBuildTask {
    static final String QUARKUS_ARTIFACT_PROPERTIES = "quarkus-artifact.properties";

    @Inject
    public QuarkusBuildCacheableAppParts() {
        super("Quarkus application build with the ability to cache the built artifacts, excluding dependencies." +
                " Do not use this task directly, use '" + QuarkusPlugin.QUARKUS_BUILD_TASK_NAME + "'");
    }

    @Internal
    public boolean isCachedByDefault() {
        switch (packageType()) {
            case JAR:
            case FAST_JAR:
            case LEGACY_JAR:
            case LEGACY:
                return true;
            default:
                return false;
        }
    }

    /**
     * Points to {@code build/quarkus-build/app} and includes the uber-jar, native runner and "quarkus-app" directory
     * w/o the `lib/` folder.
     */
    @OutputDirectories
    public Map<String, File> getOutputDirectories() {
        Map<String, File> outputs = new HashMap<>();
        PackageConfig.BuiltInType packageType = packageType();
        switch (packageType) {
            case JAR:
            case FAST_JAR:
            case NATIVE:
            case LEGACY_JAR:
            case LEGACY:
                outputs.put("app-build-dir", appBuildDir().toFile());
                break;
            case MUTABLE_JAR:
            case UBER_JAR:
            case NATIVE_SOURCES:
                break;
            default:
                throw new GradleException("Unsupported package type " + packageType);
        }
        return outputs;
    }

    @TaskAction
    public void performQuarkusBuild() {
        Path appDir = appBuildDir();

        // Caching and "up-to-date" checks depend on the inputs, this 'delete()' should ensure that the up-to-date
        // checks work against "clean" outputs, considering that the outputs depend on the package-type.
        getFileSystemOperations().delete(delete -> delete.delete(appDir));

        PackageConfig.BuiltInType packageType = packageType();
        switch (packageType) {
            case JAR:
            case FAST_JAR:
            case NATIVE:
                fastJarBuild();
                break;
            case LEGACY_JAR:
            case LEGACY:
                legacyJarBuild();
                break;
            case MUTABLE_JAR:
            case UBER_JAR:
            case NATIVE_SOURCES:
                getLogger().info(
                        "Falling back to 'full quarkus application build' for package type {}, this task's output is empty for this package type",
                        packageType);
                break;
            default:
                throw new GradleException("Unsupported package type " + packageType);
        }
    }

    private void legacyJarBuild() {
        generateBuild();

        Path genDir = genBuildDir();
        Path appDir = appBuildDir();

        getLogger().info("Synchronizing Quarkus legacy-jar app for package type {} into {}", packageType(),
                appDir);

        getFileSystemOperations().sync(sync -> {
            sync.into(appDir);
            sync.from(genDir);
            sync.include("**", QUARKUS_ARTIFACT_PROPERTIES);
            sync.exclude("lib/**");
        });
        getFileSystemOperations().copy(copy -> {
            copy.into(appDir);
            copy.from(genDir);
            copy.include("lib/modified-*");
        });
    }

    private void fastJarBuild() {
        generateBuild();

        String outputDirectory = outputDirectory();
        Path genDir = genBuildDir();
        Path appDir = appBuildDir();

        getLogger().info("Synchronizing Quarkus fast-jar-like app for package type {} into {}", packageType(),
                appDir);

        getFileSystemOperations().sync(sync -> {
            sync.into(appDir);
            sync.from(genDir);
            sync.exclude(outputDirectory + "/lib/**");
        });
    }
}
