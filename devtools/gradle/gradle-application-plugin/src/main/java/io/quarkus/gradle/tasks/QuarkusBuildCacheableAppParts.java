package io.quarkus.gradle.tasks;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectories;
import org.gradle.api.tasks.TaskAction;

import io.quarkus.gradle.QuarkusPlugin;

@CacheableTask
public abstract class QuarkusBuildCacheableAppParts extends QuarkusBuildTask {
    static final String QUARKUS_ARTIFACT_PROPERTIES = "quarkus-artifact.properties";

    @Inject
    public QuarkusBuildCacheableAppParts() {
        super("Quarkus application build with the ability to cache the built artifacts, excluding dependencies." +
                " Do not use this task directly, use '" + QuarkusPlugin.QUARKUS_BUILD_TASK_NAME + "'", true);
    }

    @SuppressWarnings("deprecation") // legacy JAR
    @Internal
    public boolean isCachedByDefault() {
        if (nativeEnabled()) {
            return false;
        }
        return switch (jarType()) {
            case FAST_JAR, LEGACY_JAR -> true;
            default -> false;
        };
    }

    /**
     * Points to {@code build/quarkus-build/app} and includes the uber-jar, native runner and "quarkus-app" directory
     * w/o the `lib/` folder.
     */
    @SuppressWarnings("deprecation") // legacy JAR
    @OutputDirectories
    public Map<String, File> getOutputDirectories() {
        Map<String, File> outputs = new HashMap<>();
        if (nativeEnabled()) {
            if (nativeSourcesOnly()) {
                // nothing
            } else {
                outputs.put("app-build-dir", appBuildDir().toFile());
            }
        } else {
            switch (jarType()) {
                case FAST_JAR, LEGACY_JAR -> outputs.put("app-build-dir", appBuildDir().toFile());
                case MUTABLE_JAR, UBER_JAR -> {
                }
            }
        }
        return outputs;
    }

    @SuppressWarnings("deprecation") // legacy JAR
    @TaskAction
    public void performQuarkusBuild() {
        Path appDir = appBuildDir();

        // Caching and "up-to-date" checks depend on the inputs, this 'delete()' should ensure that the up-to-date
        // checks work against "clean" outputs, considering that the outputs depend on the package-type.
        getFileSystemOperations().delete(delete -> delete.delete(appDir));

        if (nativeEnabled()) {
            if (nativeSourcesOnly()) {
                getLogger().info(
                        "Falling back to 'full quarkus application build' for native sources, this task's output is empty for this package type");
            } else {
                fastJarBuild();
            }
        } else {
            switch (jarType()) {
                case FAST_JAR -> fastJarBuild();
                case LEGACY_JAR -> legacyJarBuild();
                case MUTABLE_JAR, UBER_JAR -> getLogger().info(
                        "Falling back to 'full quarkus application build' for JAR type {}, this task's output is empty for this package type",
                        jarType());
            }
        }
    }

    private void legacyJarBuild() {
        generateBuild();

        Path genDir = genBuildDir();
        Path appDir = appBuildDir();

        getLogger().info("Synchronizing Quarkus legacy-jar app for JAR type {} into {}", jarType(),
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

        if (nativeEnabled()) {
            if (nativeSourcesOnly()) {
                getLogger().info("Synchronizing Quarkus fast-jar-like app for native sources into {}",
                        appDir);
            } else {
                getLogger().info("Synchronizing Quarkus fast-jar-like app for native into {}",
                        appDir);
            }
        } else {
            getLogger().info("Synchronizing Quarkus fast-jar-like app for JAR type {} into {}", jarType(),
                    appDir);
        }

        getFileSystemOperations().sync(sync -> {
            sync.into(appDir);
            sync.from(genDir);
            sync.exclude(outputDirectory + "/lib/**");
        });
    }
}
