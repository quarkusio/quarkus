package io.quarkus.devtools.testing.registry.client;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.PlatformCatalog;
import io.quarkus.registry.catalog.json.JsonCatalogMapperHelper;
import io.quarkus.registry.catalog.json.JsonExtensionCatalog;
import io.quarkus.registry.catalog.json.JsonPlatformCatalog;
import io.quarkus.registry.client.RegistryClient;
import io.quarkus.registry.client.spi.RegistryClientEnvironment;
import io.quarkus.registry.config.RegistriesConfigLocator;
import io.quarkus.registry.config.RegistryConfig;
import io.quarkus.registry.config.json.JsonRegistryConfig;
import io.quarkus.registry.config.json.RegistriesConfigMapperHelper;
import io.quarkus.registry.util.PlatformArtifacts;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.aether.artifact.DefaultArtifact;

public class TestRegistryClient implements RegistryClient {

    private final MavenArtifactResolver resolver;
    private final MessageWriter log;
    private final RegistryConfig config;
    private final Path registryDir;

    public TestRegistryClient(RegistryClientEnvironment env, RegistryConfig clientConfig) {
        this.resolver = env.resolver();
        this.log = env.log();
        final Path configYaml = RegistriesConfigLocator.locateConfigYaml();
        if (configYaml == null) {
            throw new IllegalStateException(
                    "Failed to locate the dev tools config file (" + RegistriesConfigLocator.CONFIG_RELATIVE_PATH + ")");
        }
        registryDir = TestRegistryClientBuilder.getRegistryDir(configYaml.getParent(), clientConfig.getId());
        if (!Files.exists(registryDir)) {
            throw new IllegalStateException("The test registry directory " + registryDir + " does not exist");
        }

        final Path configPath = TestRegistryClientBuilder.getRegistryDescriptorPath(registryDir);
        final JsonRegistryConfig registryConfig;
        try {
            registryConfig = RegistriesConfigMapperHelper.deserialize(configPath, JsonRegistryConfig.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to deserialize registry configuration from " + configPath, e);
        }
        registryConfig.setId(clientConfig.getId());
        if (clientConfig.getNonPlatformExtensions() != null) {
            registryConfig.setNonPlatformExtensions(clientConfig.getNonPlatformExtensions());
        }

        if (clientConfig.getPlatforms() != null) {
            registryConfig.setPlatforms(clientConfig.getPlatforms());
        }
        if (clientConfig.getDescriptor() != null) {
            registryConfig.setDescriptor(clientConfig.getDescriptor());
        }
        if (clientConfig.getQuarkusVersions() != null) {
            registryConfig.setQuarkusVersions(clientConfig.getQuarkusVersions());
        }
        this.config = registryConfig;
    }

    @Override
    public ExtensionCatalog resolveNonPlatformExtensions(String quarkusVersion) throws RegistryResolutionException {
        if (config.getNonPlatformExtensions() == null || config.getNonPlatformExtensions().isDisabled()) {
            return null;
        }
        log.info("%s resolveNonPlatformExtensions %s", config.getId(), quarkusVersion);
        return null;
    }

    @Override
    public ExtensionCatalog resolvePlatformExtensions(ArtifactCoords platformCoords)
            throws RegistryResolutionException {
        final ArtifactCoords coords = PlatformArtifacts.ensureCatalogArtifact(platformCoords);
        log.debug("%s resolvePlatformExtensions %s", config.getId(), coords);
        final Path p;
        try {
            p = resolver.resolve(new DefaultArtifact(coords.getGroupId(), coords.getArtifactId(), coords.getClassifier(),
                    coords.getType(), coords.getVersion())).getArtifact().getFile().toPath();
        } catch (BootstrapMavenException e) {
            throw new RegistryResolutionException("Failed to resolve " + coords, e);
        }
        try {
            return JsonCatalogMapperHelper.deserialize(p, JsonExtensionCatalog.class);
        } catch (IOException e) {
            throw new RegistryResolutionException("Failed to deserialize " + p, e);
        }
    }

    @Override
    public PlatformCatalog resolvePlatforms(String quarkusVersion) throws RegistryResolutionException {
        final Path json = TestRegistryClientBuilder.getRegistryPlatformsCatalogPath(registryDir, quarkusVersion);
        log.debug("%s resolvePlatforms %s", config.getId(), json);
        try {
            return JsonCatalogMapperHelper.deserialize(json, JsonPlatformCatalog.class);
        } catch (IOException e) {
            throw new RegistryResolutionException("Failed to deserialize " + json, e);
        }
    }

    @Override
    public RegistryConfig resolveRegistryConfig() throws RegistryResolutionException {
        return config;
    }

    public static RegistryConfig getDefaultConfig() {

        final JsonRegistryConfig config = new JsonRegistryConfig();
        config.setId("test.quarkus.registry");
        config.setAny("client-factory-url",
                TestRegistryClient.class.getProtectionDomain().getCodeSource().getLocation().toExternalForm());
        return config;
    }
}
