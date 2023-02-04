package io.quarkus.gradle.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import io.quarkus.gradle.dsl.Manifest;
import io.quarkus.runtime.util.StringUtil;

@CacheableTask
public abstract class QuarkusBuild extends QuarkusBuildTask {

    @Inject
    @SuppressWarnings("CdiInjectionPointsInspection")
    public QuarkusBuild(QuarkusBuildConfiguration buildConfiguration) {
        super(buildConfiguration, "Quarkus builds a runner jar based on the build jar");
    }

    public QuarkusBuild nativeArgs(Action<Map<String, ?>> action) {
        Map<String, ?> nativeArgsMap = new HashMap<>();
        action.execute(nativeArgsMap);
        for (Map.Entry<String, ?> nativeArg : nativeArgsMap.entrySet()) {
            System.setProperty(expandConfigurationKey(nativeArg.getKey()), nativeArg.getValue().toString());
        }
        return this;
    }

    public QuarkusBuild manifest(Action<Manifest> action) {
        action.execute(this.getManifest());
        return this;
    }

    @Option(description = "When using the uber-jar option, this option can be used to "
            + "specify one or more entries that should be excluded from the final jar", option = "ignored-entry")
    public void setIgnoredEntries(List<String> ignoredEntries) {
        buildConfiguration.ignoredEntries.addAll(ignoredEntries);
    }

    @OutputFile
    public File getRunnerJar() {
        return effectiveConfig().runnerJar();
    }

    @OutputFile
    public File getNativeRunner() {
        return effectiveConfig().nativeRunner();
    }

    @OutputDirectory
    public File getFastJar() {
        return effectiveConfig().fastJar();
    }

    @OutputFile
    public File getArtifactProperties() {
        return new File(getProject().getBuildDir(), QUARKUS_ARTIFACT_PROPERTIES);
    }

    @TaskAction
    public void buildQuarkus() {
        String packageType = effectiveConfig().packageType();

        cleanup();

        if (QuarkusBuildConfiguration.isFastJarPackageType(packageType)) {
            assembleFastJar();
        } else if (QuarkusBuildConfiguration.isLegacyJarPackageType(packageType)) {
            assembleLegacyJar();
        } else if (QuarkusBuildConfiguration.isMutableJarPackageType(packageType)) {
            assembleFullBuild();
        } else if (QuarkusBuildConfiguration.isUberJarPackageType(packageType)) {
            assembleFullBuild();
        } else {
            throw new GradleException("Unsupported package type " + packageType);
        }
    }

    private void cleanup() {
        // This 'cleanup' removes all _potential_ result artifacts (runners, fast-jar-directory), which is important,
        // because if output artifacts for a 'fast-jar' build are left "behind" and the next run uses the 'uber-jar'
        // package type, then the (developer machine) local cache artifact would contain the (potentially outdated)
        // fast-jar directory _and_ the uber-jar. Gradle could restore the result with the "newest" uber-jar
        // _including_ the potentially (quite outdated) fast-jar, which may likely confuse users.
        //
        // Previous versions of the Quarkus Gradle plugin left artifacts built with for example different
        // package types, output directory, output name where they were.
        if (extension().getCleanupBuildOutput().get()) {
            getLogger().info("Removing potentially existing runner files and fast-jar directory.");
            File fastJar = effectiveConfig().fastJar();
            getFileSystemOperations().delete(delete -> delete.delete(getRunnerJar(), getNativeRunner(), fastJar));
        }
    }

    private void assembleLegacyJar() {
        getLogger().info("Finalizing Quarkus build for {} packaging", effectiveConfig().packageType());

        Path buildDir = getProject().getBuildDir().toPath();
        Path libDir = buildDir.resolve("lib");
        Path depBuildDir = effectiveConfig().depBuildDir();
        Path appBuildDir = effectiveConfig().appBuildDir();

        getLogger().info("Removing potentially existing legacy-jar lib/ directory.");
        getFileSystemOperations().delete(delete -> delete.delete(libDir));

        getLogger().info("Copying lib/ directory from {} into {}", depBuildDir, buildDir);
        getFileSystemOperations().copy(copy -> {
            copy.into(buildDir);
            copy.from(depBuildDir);
            copy.include("lib/**");
        });

        getLogger().info("Copying lib/ directory from {} into {}", appBuildDir, buildDir);
        getFileSystemOperations().copy(copy -> {
            copy.into(buildDir);
            copy.from(appBuildDir);
            copy.include("lib/**");
        });

        // Quarkus' 'legacy-jar' package type produces 'lib/modified-*.jar' files for some dependencies.
        // The following code block removes the non-modified jars.
        getLogger().info("Cleaning up lib/ directory in {}", buildDir);
        try (Stream<Path> files = Files.walk(libDir)) {
            files.filter(Files::isRegularFile).filter(f -> f.getFileName().toString().startsWith("modified-"))
                    .map(f -> f.getParent().resolve(f.getFileName().toString().substring("modified-".length())))
                    .collect(Collectors.toList()) // necessary :(
                    .forEach(f -> {
                        try {
                            Files.deleteIfExists(f);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (IOException e) {
            throw new GradleException("Failed to clean up non-modified jars in lib/");
        }

        copyRunnersAndArtifactProperties(effectiveConfig().appBuildDir());
    }

    private void assembleFullBuild() {
        File targetDir = getProject().getBuildDir();

        // build/quarkus-build/gen
        Path genBuildDir = effectiveConfig().genBuildDir();

        getLogger().info("Copying Quarkus build for {} packaging from {} into {}", effectiveConfig().packageType(),
                genBuildDir, targetDir);
        getFileSystemOperations().copy(copy -> {
            copy.into(targetDir);
            copy.from(genBuildDir);
        });

        copyRunnersAndArtifactProperties(effectiveConfig().genBuildDir());
    }

    private void assembleFastJar() {
        File appTargetDir = effectiveConfig().fastJar();

        // build/quarkus-build/app
        Path appBuildBaseDir = effectiveConfig().appBuildDir();
        // build/quarkus-build/app/quarkus-app
        Path appBuildDir = appBuildBaseDir.resolve(effectiveConfig().outputDirectory());
        // build/quarkus-build/dep
        Path depBuildDir = effectiveConfig().depBuildDir();

        getLogger().info("Synchronizing Quarkus build for {} packaging from {} and {} into {}", effectiveConfig().packageType(),
                appBuildDir, depBuildDir, appTargetDir);
        getFileSystemOperations().sync(sync -> {
            sync.into(appTargetDir);
            sync.from(appBuildDir, depBuildDir);
        });

        copyRunnersAndArtifactProperties(effectiveConfig().appBuildDir());
    }

    private void copyRunnersAndArtifactProperties(Path sourceDir) {
        File buildDir = getProject().getBuildDir();

        getLogger().info("Copying remaining Quarkus application artifacts for {} packaging from {} into {}",
                effectiveConfig().packageType(), sourceDir, buildDir);
        getFileSystemOperations().copy(
                copy -> copy.into(buildDir).from(sourceDir).include(QUARKUS_ARTIFACT_PROPERTIES,
                        effectiveConfig().nativeRunnerFileName(),
                        effectiveConfig().runnerJarFileName(), "jib-image*",
                        effectiveConfig().runnerBaseName() + "-native-image-source-jar/**"));
    }

    private String expandConfigurationKey(String shortKey) {
        final String hyphenatedKey = StringUtil.hyphenate(shortKey);
        if (hyphenatedKey.startsWith(NATIVE_PROPERTY_NAMESPACE)) {
            return hyphenatedKey;
        }
        return String.format("%s.%s", NATIVE_PROPERTY_NAMESPACE, hyphenatedKey);
    }
}
