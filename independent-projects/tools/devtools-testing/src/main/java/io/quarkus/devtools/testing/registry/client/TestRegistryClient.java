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
import io.quarkus.registry.config.RegistriesConfigMapperHelper;
import io.quarkus.registry.config.RegistryConfig;
import io.quarkus.registry.config.json.JsonRegistryConfig;
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
    private final boolean enableMavenResolver;

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

        final Object o = clientConfig.getExtra().get("enable-maven-resolver");
        enableMavenResolver = o == null ? false : Boolean.parseBoolean(o.toString());
        this.config = registryConfig;
    }

    @Override
    public ExtensionCatalog resolveNonPlatformExtensions(String quarkusVersion) throws RegistryResolutionException {
        if (config.getNonPlatformExtensions() == null || config.getNonPlatformExtensions().isDisabled()) {
            return null;
        }
        log.debug("%s resolveNonPlatformExtensions %s", config.getId(), quarkusVersion);
        final Path json = TestRegistryClientBuilder.getRegistryNonPlatformCatalogPath(registryDir, quarkusVersion);
        if (Files.exists(json)) {
            return deserializeExtensionCatalog(json);
        }
        return null;
    }

    @Override
    public ExtensionCatalog resolvePlatformExtensions(ArtifactCoords platformCoords)
            throws RegistryResolutionException {
        final ArtifactCoords coords = PlatformArtifacts.ensureCatalogArtifact(platformCoords);
        log.debug("%s resolvePlatformExtensions %s", config.getId(), coords);

        Path p = TestRegistryClientBuilder.getRegistryMemberCatalogPath(registryDir,
                PlatformArtifacts.ensureBomArtifact(coords));
        if (!Files.exists(p)) {
            if (enableMavenResolver) {
                try {
                    p = resolver
                            .resolve(new DefaultArtifact(coords.getGroupId(), coords.getArtifactId(),
                                    coords.getClassifier(), coords.getType(), coords.getVersion()))
                            .getArtifact().getFile().toPath();
                } catch (BootstrapMavenException e) {
                    throw new RegistryResolutionException("Failed to resolve " + coords, e);
                }
            } else {
                return null;
            }
        }
        return deserializeExtensionCatalog(p);
    }

    private ExtensionCatalog deserializeExtensionCatalog(Path p) throws RegistryResolutionException {
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
        if (!Files.exists(json)) {
            return null;
        }
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

    @Override
    public void clearCache() {
        log.debug("% clearCache not supported", config.getId());
    }

    public static RegistryConfig getDefaultConfig() {

        final JsonRegistryConfig config = new JsonRegistryConfig();
        config.setId("test.quarkus.registry");
        config.setAny("client-factory-url",
                TestRegistryClient.class.getProtectionDomain().getCodeSource().getLocation().toExternalForm());
        return config;
    }
}
