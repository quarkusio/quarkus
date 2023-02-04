package io.quarkus.gradle.tasks;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.GradleException;
import org.gradle.api.java.archives.Attributes;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.ArtifactResult;
import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.JarResult;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.gradle.QuarkusPlugin;
import io.quarkus.maven.dependency.GACTV;

@CacheableTask
public abstract class QuarkusBuildApp extends QuarkusBuildTask {

    @Inject
    @SuppressWarnings("CdiInjectionPointsInspection")
    public QuarkusBuildApp(QuarkusBuildConfiguration buildConfiguration) {
        super(buildConfiguration, "Quarkus builds a runner jar based on the build jar");
    }

    /**
     * Points to {@code build/quarkus-build/app} and includes the uber-jar, native runner and "quarkus-app" directory
     * w/o the `lib/` folder.
     */
    @OutputDirectory
    public File getAppBuildDir() {
        return effectiveConfig().appBuildDir().toFile();
    }

    @TaskAction
    public void finalizeQuarkusBuild() {
        Path appDir = effectiveConfig().appBuildDir();

        // Caching and "up-to-date" checks depend on the inputs, this 'delete()' should ensure that the up-to-date
        // checks work against "clean" outputs, considering that the outputs depend on the package-type.
        getFileSystemOperations().delete(delete -> delete.delete(appDir));

        String packageType = effectiveConfig().packageType();

        if (QuarkusBuildConfiguration.isFastJarPackageType(packageType)) {
            fastJarBuild();
        } else if (QuarkusBuildConfiguration.isLegacyJarPackageType(packageType)) {
            legacyJarBuild();
        } else if (QuarkusBuildConfiguration.isMutableJarPackageType(packageType)) {
            generateBuild();
        } else if (QuarkusBuildConfiguration.isUberJarPackageType(packageType)) {
            generateBuild();
        } else {
            throw new GradleException("Unsupported package type " + packageType);
        }
    }

    private void legacyJarBuild() {
        generateBuild();

        Path genDir = effectiveConfig().genBuildDir();
        Path appDir = effectiveConfig().appBuildDir();

        getLogger().info("Synchronizing Quarkus legacy-jar app for package type {} into {}", effectiveConfig().packageType(),
                appDir);

        getFileSystemOperations().sync(sync -> {
            sync.into(appDir);
            sync.from(genDir);
            sync.include("**", QuarkusPlugin.QUARKUS_ARTIFACT_PROPERTIES);
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

        String outputDirectory = effectiveConfig().outputDirectory();
        Path genDir = effectiveConfig().genBuildDir();
        Path appDir = effectiveConfig().appBuildDir();

        getLogger().info("Synchronizing Quarkus fast-jar-like app for package type {} into {}", effectiveConfig().packageType(),
                appDir);

        getFileSystemOperations().sync(sync -> {
            sync.into(appDir);
            sync.from(genDir);
            sync.exclude(outputDirectory + "/lib/**");
        });
    }

    void generateBuild() {
        Path genDir = effectiveConfig().genBuildDir();
        String packageType = effectiveConfig().packageType();
        getLogger().info("Building Quarkus app for package type {} in {}", packageType, genDir);

        // Caching and "up-to-date" checks depend on the inputs, this 'delete()' should ensure that the up-to-date
        // checks work against "clean" outputs, considering that the outputs depend on the package-type.
        getFileSystemOperations().delete(delete -> delete.delete(genDir));

        ApplicationModel appModel;
        Map<String, String> forcedProperties = getForcedProperties().getOrElse(Collections.emptyMap());

        try {
            appModel = extension().getAppModelResolver().resolveModel(new GACTV(getProject().getGroup().toString(),
                    getProject().getName(), getProject().getVersion().toString()));
        } catch (AppModelResolverException e) {
            throw new GradleException("Failed to resolve Quarkus application model for " + getProject().getPath(), e);
        }

        final Properties effectiveProperties = getBuildSystemProperties(appModel.getAppArtifact());
        effectiveProperties.putAll(forcedProperties);
        List<String> ignoredEntries = getIgnoredEntries();
        if (!ignoredEntries.isEmpty()) {
            String joinedEntries = String.join(",", ignoredEntries);
            effectiveProperties.setProperty("quarkus.package.user-configured-ignored-entries", joinedEntries);
        }

        exportCustomManifestProperties(effectiveProperties);

        getLogger().info("Starting Quarkus application build for package type {}", packageType);

        if (getLogger().isEnabled(LogLevel.DEBUG)) {
            getLogger().debug("Effective properties: {}",
                    effectiveProperties.entrySet().stream().map(e -> "" + e)
                            .collect(Collectors.joining("\n    ", "\n    ", "")));
        }

        try (CuratedApplication appCreationContext = QuarkusBootstrap.builder()
                .setBaseClassLoader(getClass().getClassLoader())
                .setExistingModel(appModel)
                .setTargetDirectory(genDir)
                .setBaseName(extension().finalName())
                .setBuildSystemProperties(effectiveProperties)
                .setAppArtifact(appModel.getAppArtifact())
                .setLocalProjectDiscovery(false)
                .setIsolateDeployment(true)
                .build().bootstrap()) {

            // Processes launched from within the build task of Gradle (daemon) lose content
            // generated on STDOUT/STDERR by the process (see https://github.com/gradle/gradle/issues/13522).
            // We overcome this by letting build steps know that the STDOUT/STDERR should be explicitly
            // streamed, if they need to make available that generated data.
            // The io.quarkus.deployment.pkg.builditem.ProcessInheritIODisabled$Factory
            // does the necessary work to generate such a build item which the build step(s) can rely on
            AugmentAction augmentor = appCreationContext
                    .createAugmentor("io.quarkus.deployment.pkg.builditem.ProcessInheritIODisabled$Factory",
                            Collections.emptyMap());

            AugmentResult result = augmentor.createProductionApplication();
            if (result == null) {
                getLogger().warn("createProductionApplication() returned 'null' AugmentResult");
            } else {
                Path nativeResult = result.getNativeResult();
                getLogger().info("AugmentResult.nativeResult = {}", nativeResult);
                List<ArtifactResult> results = result.getResults();
                if (results == null) {
                    getLogger().info("AugmentResult.results = null");
                } else {
                    getLogger().info("AugmentResult.results = {}", results.stream().map(ArtifactResult::getPath)
                            .map(Object::toString).collect(Collectors.joining("\n    ", "\n    ", "")));
                }
                JarResult jar = result.getJar();
                if (jar == null) {
                    getLogger().info("AugmentResult.jar = null");
                } else {
                    getLogger().info("AugmentResult.jar.path = {}", jar.getPath());
                    getLogger().info("AugmentResult.jar.libraryDir = {}", jar.getLibraryDir());
                    getLogger().info("AugmentResult.jar.originalArtifact = {}", jar.getOriginalArtifact());
                    getLogger().info("AugmentResult.jar.uberJar = {}", jar.isUberJar());
                }
            }

            getLogger().debug("Quarkus application built successfully");
        } catch (BootstrapException e) {
            throw new GradleException("Failed to build Quarkus application", e);
        }
    }

    private void exportCustomManifestProperties(Properties buildSystemProperties) {
        for (Map.Entry<String, Object> attribute : effectiveConfig().manifest.getAttributes().entrySet()) {
            buildSystemProperties.put(toManifestAttributeKey(attribute.getKey()),
                    attribute.getValue());
        }

        for (Map.Entry<String, Attributes> section : effectiveConfig().manifest.getSections().entrySet()) {
            for (Map.Entry<String, Object> attribute : section.getValue().entrySet()) {
                buildSystemProperties
                        .put(toManifestSectionAttributeKey(section.getKey(), attribute.getKey()), attribute.getValue());
            }
        }
    }

    private String toManifestAttributeKey(String key) {
        if (key.contains("\"")) {
            throw new GradleException("Manifest entry name " + key + " is invalid. \" characters are not allowed.");
        }
        return String.format("%s.\"%s\"", MANIFEST_ATTRIBUTES_PROPERTY_PREFIX, key);
    }

    private String toManifestSectionAttributeKey(String section, String key) {
        if (section.contains("\"")) {
            throw new GradleException("Manifest section name " + section + " is invalid. \" characters are not allowed.");
        }
        if (key.contains("\"")) {
            throw new GradleException("Manifest entry name " + key + " is invalid. \" characters are not allowed.");
        }
        return String.format("%s.\"%s\".\"%s\"", MANIFEST_SECTIONS_PROPERTY_PREFIX, section,
                key);
    }
}
