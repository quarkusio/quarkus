package io.quarkus.devtools.testing;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.Constants;
import io.quarkus.registry.catalog.json.JsonCatalogMapperHelper;
import io.quarkus.registry.catalog.json.JsonPlatform;
import io.quarkus.registry.catalog.json.JsonPlatformCatalog;
import io.quarkus.registry.catalog.json.JsonPlatformRelease;
import io.quarkus.registry.catalog.json.JsonPlatformStream;
import io.quarkus.registry.config.RegistriesConfigLocator;
import io.quarkus.registry.config.json.JsonRegistriesConfig;
import io.quarkus.registry.config.json.JsonRegistryConfig;
import io.quarkus.registry.config.json.JsonRegistryMavenConfig;
import io.quarkus.registry.config.json.JsonRegistryMavenRepoConfig;
import io.quarkus.registry.config.json.JsonRegistryNonPlatformExtensionsConfig;
import io.quarkus.registry.config.json.JsonRegistryPlatformsConfig;
import io.quarkus.registry.config.json.RegistriesConfigMapperHelper;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Properties;

public class RegistryClientTestHelper {

    public static void enableRegistryClientTestConfig() {
        enableRegistryClientTestConfig(System.getProperties());
    }

    public static void enableRegistryClientTestConfig(Properties properties) {
        enableRegistryClientTestConfig(Paths.get("").normalize().toAbsolutePath().resolve("target"),
                properties);
    }

    public static void enableRegistryClientTestConfig(Path outputDir, Properties properties) {
        final Path toolsConfigPath = outputDir.resolve(RegistriesConfigLocator.CONFIG_RELATIVE_PATH);
        final Path registryRepoPath = outputDir.resolve("test-registry-repo");
        final Path groupIdDir = registryRepoPath.resolve("io/quarkus/registry/test");

        generateToolsConfig(toolsConfigPath, registryRepoPath);
        generateRegistryDescriptor(groupIdDir);
        generatePlatformCatalog(groupIdDir);

        properties.setProperty("io.quarkus.maven.secondary-local-repo", registryRepoPath.toString());
        properties.setProperty(RegistriesConfigLocator.CONFIG_FILE_PATH_PROPERTY, toolsConfigPath.toString());
        properties.setProperty("quarkusRegistryClient", "true");
    }

    private static void generatePlatformCatalog(final Path groupIdDir) {
        final String projectVersion = System.getProperty("project.version");
        if (projectVersion == null) {
            throw new IllegalStateException("System property project.version isn't set");
        }
        final Path platformsPath = groupIdDir.resolve(Constants.DEFAULT_REGISTRY_PLATFORMS_CATALOG_ARTIFACT_ID)
                .resolve(Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION)
                .resolve(Constants.DEFAULT_REGISTRY_PLATFORMS_CATALOG_ARTIFACT_ID + "-"
                        + Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION + ".json");
        final Path versionedPlatformsPath = groupIdDir.resolve(Constants.DEFAULT_REGISTRY_PLATFORMS_CATALOG_ARTIFACT_ID)
                .resolve(Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION)
                .resolve(Constants.DEFAULT_REGISTRY_PLATFORMS_CATALOG_ARTIFACT_ID + "-"
                        + Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION + "-" + projectVersion + ".json");
        if (Files.exists(platformsPath) && Files.exists(versionedPlatformsPath)) {
            return;
        }
        final ArtifactCoords bom = new ArtifactCoords("io.quarkus", "quarkus-bom", null, "pom", projectVersion);
        final JsonPlatformCatalog platforms = new JsonPlatformCatalog();
        final JsonPlatform platform = new JsonPlatform();
        platforms.addPlatform(platform);
        platform.setPlatformKey(bom.getGroupId());
        final JsonPlatformStream stream = new JsonPlatformStream();
        platform.setStreams(Collections.singletonList(stream));
        stream.setId(projectVersion);
        final JsonPlatformRelease release = new JsonPlatformRelease();
        stream.setReleases(Collections.singletonList(release));
        release.setMemberBoms(Collections.singletonList(bom));
        release.setQuarkusCoreVersion(projectVersion);
        try {
            JsonCatalogMapperHelper.serialize(platforms, platformsPath);
            Files.copy(platformsPath, versionedPlatformsPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to persist registry platforms config", e);
        }
    }

    private static void generateRegistryDescriptor(Path repoGroupIdDir) {
        final Path descriptorPath = repoGroupIdDir.resolve(Constants.DEFAULT_REGISTRY_DESCRIPTOR_ARTIFACT_ID)
                .resolve(Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION).resolve(Constants.DEFAULT_REGISTRY_DESCRIPTOR_ARTIFACT_ID
                        + "-" + Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION + ".json");
        if (Files.exists(descriptorPath)) {
            return;
        }
        final JsonRegistryConfig descriptor = new JsonRegistryConfig();
        final JsonRegistryPlatformsConfig platformsConfig = new JsonRegistryPlatformsConfig();
        descriptor.setPlatforms(platformsConfig);
        platformsConfig.setArtifact(
                new ArtifactCoords("io.quarkus.registry.test", Constants.DEFAULT_REGISTRY_PLATFORMS_CATALOG_ARTIFACT_ID, null,
                        "json", Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION));
        final JsonRegistryNonPlatformExtensionsConfig nonPlatformsConfig = new JsonRegistryNonPlatformExtensionsConfig();
        descriptor.setNonPlatformExtensions(nonPlatformsConfig);
        nonPlatformsConfig.setDisabled(true);
        try {
            RegistriesConfigMapperHelper.serialize(descriptor, descriptorPath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to persist the registry descriptor", e);
        }
    }

    private static void generateToolsConfig(Path toolsConfigPath, Path registryRepoPath) {
        if (Files.exists(toolsConfigPath)) {
            return;
        }
        final JsonRegistryConfig registryConfig = new JsonRegistryConfig();
        registryConfig.setId("test.registry.quarkus.io");
        final JsonRegistryMavenConfig mavenConfig = new JsonRegistryMavenConfig();
        registryConfig.setMaven(mavenConfig);
        final JsonRegistryMavenRepoConfig repoConfig = new JsonRegistryMavenRepoConfig();
        mavenConfig.setRepository(repoConfig);
        try {
            repoConfig.setUrl(registryRepoPath.toUri().toURL().toString());
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Failed to translate a path to url", e);
        }

        final JsonRegistriesConfig toolsConfig = new JsonRegistriesConfig();
        toolsConfig.addRegistry(registryConfig);
        toolsConfig.setDebug(false);

        try {
            RegistriesConfigMapperHelper.serialize(toolsConfig,
                    toolsConfigPath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to persist the tools config", e);
        }
    }

    public static void disableRegistryClientTestConfig() {
        disableRegistryClientTestConfig(System.getProperties());
    }

    public static void disableRegistryClientTestConfig(Properties properties) {
        properties.remove("io.quarkus.maven.secondary-local-repo");
        properties.remove(RegistriesConfigLocator.CONFIG_FILE_PATH_PROPERTY);
        properties.remove("quarkusRegistryClient");
    }
}
