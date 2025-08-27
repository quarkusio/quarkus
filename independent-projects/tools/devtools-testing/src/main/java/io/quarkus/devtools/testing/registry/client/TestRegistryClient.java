package io.quarkus.devtools.testing.registry.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.util.repository.ChainedLocalRepositoryManager;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.PlatformCatalog;
import io.quarkus.registry.client.RegistryClient;
import io.quarkus.registry.client.spi.RegistryClientEnvironment;
import io.quarkus.registry.config.RegistriesConfigLocator;
import io.quarkus.registry.config.RegistryConfig;
import io.quarkus.registry.util.PlatformArtifacts;

public class TestRegistryClient implements RegistryClient {

    private final MavenArtifactResolver resolver;
    private final MessageWriter log;
    private final RegistryConfig config;
    private final Path registryDir;
    private final boolean enableMavenResolver;

    public TestRegistryClient(RegistryClientEnvironment env, RegistryConfig clientConfig) {
        this.resolver = configureMavenResolver(env.resolver(), clientConfig);
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
        final RegistryConfig.Mutable registryConfig;
        try {
            registryConfig = RegistryConfig.mutableFromFile(configPath);
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
        if (!clientConfig.getExtra().isEmpty()) {
            if (registryConfig.getExtra().isEmpty()) {
                registryConfig.setExtra(clientConfig.getExtra());
            } else {
                for (Map.Entry<String, Object> e : clientConfig.getExtra().entrySet()) {
                    registryConfig.getExtra().put(e.getKey(), e.getValue());
                }
            }
        }
        enableMavenResolver = Boolean.parseBoolean(String.valueOf(clientConfig.getExtra().get("enable-maven-resolver")));
        this.config = registryConfig;
    }

    private static MavenArtifactResolver configureMavenResolver(MavenArtifactResolver originalResolver,
            RegistryConfig registryConfig) {
        Object testLocalMavenRepo = registryConfig.getExtra().get("test-local-maven-repo");
        if (testLocalMavenRepo == null) {
            return originalResolver;
        }
        try {
            var session = new DefaultRepositorySystemSession(originalResolver.getSession());
            session.setLocalRepositoryManager(new ChainedLocalRepositoryManager(
                    originalResolver.getSystem().newLocalRepositoryManager(originalResolver.getSession(),
                            new LocalRepository(testLocalMavenRepo.toString())),
                    List.of(originalResolver.getSystem().newLocalRepositoryManager(originalResolver.getSession(),
                            new LocalRepository(originalResolver.getMavenContext().getLocalRepo()))),
                    false));
            return new MavenArtifactResolver(new BootstrapMavenContext(BootstrapMavenContext.config()
                    .setWorkspaceDiscovery(false)
                    .setRepositorySystemSession(session)
                    .setRemoteRepositoryManager(originalResolver.getRemoteRepositoryManager())
                    .setRemoteRepositories(originalResolver.getRepositories())
                    .setRepositorySystem(originalResolver.getSystem())));
        } catch (BootstrapMavenException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ExtensionCatalog.Mutable resolveNonPlatformExtensions(String quarkusVersion) throws RegistryResolutionException {
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
    public ExtensionCatalog.Mutable resolvePlatformExtensions(ArtifactCoords platformCoords)
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

    private ExtensionCatalog.Mutable deserializeExtensionCatalog(Path p) throws RegistryResolutionException {
        try {
            return ExtensionCatalog.mutableFromFile(p);
        } catch (IOException e) {
            throw new RegistryResolutionException("Failed to deserialize " + p, e);
        }
    }

    @Override
    public PlatformCatalog.Mutable resolvePlatforms(String quarkusVersion) throws RegistryResolutionException {
        final Path json = TestRegistryClientBuilder.getRegistryPlatformsCatalogPath(registryDir, quarkusVersion);
        log.debug("%s resolvePlatforms %s", config.getId(), json);
        if (!Files.exists(json)) {
            return null;
        }
        try {
            return PlatformCatalog.mutableFromFile(json);
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
        return RegistryConfig.builder()
                .setId("test.quarkus.registry")
                .setExtra("client-factory-url",
                        TestRegistryClient.class.getProtectionDomain().getCodeSource().getLocation().toExternalForm())
                .build();
    }
}
