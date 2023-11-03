package io.quarkus.gradle.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectories;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.gradle.dsl.Manifest;
import io.quarkus.runtime.util.StringUtil;

@CacheableTask
public abstract class QuarkusBuild extends QuarkusBuildTask {

    private static final String NATIVE_PROPERTY_NAMESPACE = "quarkus.native";
    public static final String QUARKUS_IGNORE_LEGACY_DEPLOY_BUILD = "quarkus.ignore.legacy.deploy.build";

    @Inject
    public QuarkusBuild() {
        super("Builds a Quarkus application.");
    }

    @SuppressWarnings("unused")
    public QuarkusBuild nativeArgs(Action<Map<String, ?>> action) {
        Map<String, ?> nativeArgsMap = new HashMap<>();
        action.execute(nativeArgsMap);
        for (Map.Entry<String, ?> nativeArg : nativeArgsMap.entrySet()) {
            getForcedProperties().put(expandConfigurationKey(nativeArg.getKey()), nativeArg.getValue().toString());
        }
        return this;
    }

    @Internal
    public MapProperty<String, String> getForcedProperties() {
        return extension().forcedPropertiesProperty();
    }

    @Internal
    public ListProperty<String> getIgnoredEntries() {
        return extension().ignoredEntriesProperty();
    }

    @Option(description = "When using the uber-jar option, this option can be used to "
            + "specify one or more entries that should be excluded from the final jar", option = "ignored-entry")
    public void setIgnoredEntries(List<String> ignoredEntries) {
        getIgnoredEntries().addAll(ignoredEntries);
    }

    @Internal
    public Manifest getManifest() {
        return extension().manifest();
    }

    @SuppressWarnings("unused")
    public QuarkusBuild manifest(Action<Manifest> action) {
        action.execute(this.getManifest());
        return this;
    }

    @Internal
    public File getRunnerJar() {
        return runnerJar();
    }

    @Internal
    public File getNativeRunner() {
        return nativeRunner();
    }

    @Internal
    public File getFastJar() {
        return fastJar();
    }

    @Internal
    public File getArtifactProperties() {
        return artifactProperties();
    }

    @OutputDirectories
    protected Map<String, File> getBuildOutputDirectories() {
        Map<String, File> outputs = new HashMap<>();
        PackageConfig.BuiltInType packageType = packageType();
        switch (packageType) {
            case LEGACY_JAR:
            case LEGACY:
                outputs.put("fast-jar", fastJar());
                outputs.put("legacy-lib", gradleBuildDir().resolve("lib").toFile());
                break;
            case NATIVE:
                outputs.put("native-source", nativeSources());
                outputs.put("fast-jar", fastJar());
                break;
            case JAR:
            case FAST_JAR:
                outputs.put("fast-jar", fastJar());
                break;
            case MUTABLE_JAR:
            case UBER_JAR:
                outputs.put("fast-jar", fastJar());
                outputs.put("generated", genBuildDir().toFile());
                break;
            case NATIVE_SOURCES:
                outputs.put("fast-jar", fastJar());
                outputs.put("generated", genBuildDir().toFile());
                outputs.put("native-source", nativeSources());
                break;
            default:
                throw new GradleException("Unsupported package type " + packageType);
        }
        return outputs;
    }

    @OutputFiles
    protected Map<String, File> getBuildOutputFiles() {
        Map<String, File> outputs = new HashMap<>();
        PackageConfig.BuiltInType packageType = packageType();
        switch (packageType) {
            case UBER_JAR:
            case LEGACY_JAR:
            case LEGACY:
                outputs.put("runner-jar", runnerJar());
                outputs.put("artifact-properties", artifactProperties());
                break;
            case NATIVE:
                outputs.put("native-runner", nativeRunner());
                outputs.put("artifact-properties", artifactProperties());
                break;
            case JAR:
            case FAST_JAR:
            case MUTABLE_JAR:
            case NATIVE_SOURCES:
                outputs.put("artifact-properties", artifactProperties());
                break;
            default:
                throw new GradleException("Unsupported package type " + packageType);
        }
        return outputs;
    }

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    protected Collection<File> getBuildInputFiles() {
        List<File> inputs = new ArrayList<>();
        PackageConfig.BuiltInType packageType = packageType();
        switch (packageType) {
            case JAR:
            case FAST_JAR:
            case NATIVE:
                Path appBuildBaseDir = appBuildDir();
                inputs.add(genBuildDir().toFile());
                inputs.add(appBuildBaseDir.resolve(outputDirectory()).toFile());
                runnerAndArtifactsInputs(inputs::add, appBuildBaseDir);
                break;
            case LEGACY_JAR:
            case LEGACY:
                inputs.add(depBuildDir().resolve("lib").toFile());
                inputs.add(appBuildDir().resolve("lib").toFile());
                runnerAndArtifactsInputs(inputs::add, appBuildDir());
                break;
            case MUTABLE_JAR:
            case NATIVE_SOURCES:
            case UBER_JAR:
                break;
            default:
                throw new GradleException("Unsupported package type " + packageType);
        }
        return inputs;
    }

    private void runnerAndArtifactsInputs(Consumer<File> buildInputs, Path sourceDir) {
        buildInputs.accept(sourceDir.resolve(QUARKUS_ARTIFACT_PROPERTIES).toFile());
        buildInputs.accept(sourceDir.resolve(nativeRunnerFileName()).toFile());
        buildInputs.accept(sourceDir.resolve(runnerJarFileName()).toFile());
        // TODO jib-image* ??
        buildInputs.accept(sourceDir.resolve(nativeImageSourceJarDirName()).toFile());
    }

    @TaskAction
    public void finalizeQuarkusBuild() {
        if (extension().forcedPropertiesProperty().get().containsKey(QUARKUS_IGNORE_LEGACY_DEPLOY_BUILD)) {
            getLogger().info("SKIPPING finalizedBy deploy build");
            return;
        }
        // This 'cleanup' removes all result artifacts (runners, fast-jar-directory), depending on the configured
        // Quarkus package type.
        getLogger().info("Removing output files and directories (provide a clean state).");
        getFileSystemOperations().delete(delete -> delete.delete(getBuildOutputFiles().values()));

        PackageConfig.BuiltInType packageType = packageType();
        switch (packageType) {
            case JAR:
            case FAST_JAR:
            case NATIVE:
                assembleFastJar();
                break;
            case LEGACY_JAR:
            case LEGACY:
                assembleLegacyJar();
                break;
            case MUTABLE_JAR:
            case UBER_JAR:
            case NATIVE_SOURCES:
                generateBuild();
                assembleFullBuild();
                break;
            default:
                throw new GradleException("Unsupported package type " + packageType);
        }
    }

    private void assembleLegacyJar() {
        getLogger().info("Finalizing Quarkus build for {} packaging", packageType());

        Path buildDir = this.buildDir.toPath();
        Path libDir = buildDir.resolve("lib");
        Path depBuildDir = depBuildDir();
        Path appBuildDir = appBuildDir();

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

        copyRunnersAndArtifactProperties(appBuildDir);
    }

    private void assembleFullBuild() {
        File targetDir = buildDir;

        // build/quarkus-build/gen
        Path genBuildDir = genBuildDir();

        getLogger().info("Copying Quarkus build for {} packaging from {} into {}", packageType(),
                genBuildDir, targetDir);
        getFileSystemOperations().copy(copy -> {
            copy.into(targetDir);
            copy.from(genBuildDir);
        });

        copyRunnersAndArtifactProperties(genBuildDir());
    }

    private void assembleFastJar() {
        File appTargetDir = fastJar();

        // build/quarkus-build/app
        Path appBuildBaseDir = appBuildDir();
        // build/quarkus-build/app/quarkus-app
        Path appBuildDir = appBuildBaseDir.resolve(outputDirectory());
        // build/quarkus-build/dep
        Path depBuildDir = depBuildDir();

        getLogger().info("Synchronizing Quarkus build for {} packaging from {} and {} into {}", packageType(),
                appBuildDir, depBuildDir, appTargetDir);
        getFileSystemOperations().sync(sync -> {
            sync.eachFile(new CopyActionDeleteNonWriteableTarget(appTargetDir.toPath()));
            sync.into(appTargetDir);
            sync.from(appBuildDir, depBuildDir);
        });

        copyRunnersAndArtifactProperties(appBuildBaseDir);
    }

    private void copyRunnersAndArtifactProperties(Path sourceDir) {
        getLogger().info("Copying remaining Quarkus application artifacts for {} packaging from {} into {}",
                packageType(), sourceDir, buildDir);
        getFileSystemOperations().copy(
                copy -> copy.into(buildDir).from(sourceDir)
                        .include(QUARKUS_ARTIFACT_PROPERTIES,
                                nativeRunnerFileName(),
                                runnerJarFileName(),
                                "jib-image*",
                                NATIVE_SOURCES,
                                nativeImageSourceJarDirName() + "/**"));
    }

    private String expandConfigurationKey(String shortKey) {
        final String hyphenatedKey = StringUtil.hyphenate(shortKey);
        if (hyphenatedKey.startsWith(NATIVE_PROPERTY_NAMESPACE)) {
            return hyphenatedKey;
        }
        return String.format("%s.%s", NATIVE_PROPERTY_NAMESPACE, hyphenatedKey);
    }
}
