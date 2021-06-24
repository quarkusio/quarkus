package io.quarkus.registry.client.maven;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.Constants;
import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.client.RegistryClient;
import io.quarkus.registry.client.RegistryClientDispatcher;
import io.quarkus.registry.client.RegistryClientFactory;
import io.quarkus.registry.client.RegistryNonPlatformExtensionsResolver;
import io.quarkus.registry.client.RegistryPlatformsResolver;
import io.quarkus.registry.config.RegistryArtifactConfig;
import io.quarkus.registry.config.RegistryConfig;
import io.quarkus.registry.config.RegistryDescriptorConfig;
import io.quarkus.registry.config.RegistryMavenConfig;
import io.quarkus.registry.config.RegistryMavenRepoConfig;
import io.quarkus.registry.config.RegistryNonPlatformExtensionsConfig;
import io.quarkus.registry.config.RegistryPlatformsConfig;
import io.quarkus.registry.config.json.JsonRegistryConfig;
import io.quarkus.registry.config.json.JsonRegistryMavenConfig;
import io.quarkus.registry.config.json.JsonRegistryMavenRepoConfig;
import io.quarkus.registry.config.json.JsonRegistryPlatformsConfig;
import io.quarkus.registry.config.json.RegistriesConfigMapperHelper;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferListener;

public class MavenRegistryClientFactory implements RegistryClientFactory {

    private static final String CLEANUP_TIMESTAMPED_ARTIFACTS = "cleanup-timestamped-artifacts";

    private MessageWriter log;
    private MavenArtifactResolver originalResolver;
    private List<RemoteRepository> singleRegistryRepos = new ArrayList<RemoteRepository>();

    public MavenRegistryClientFactory(MavenArtifactResolver resolver, MessageWriter log) {
        this.originalResolver = Objects.requireNonNull(resolver);
        this.log = Objects.requireNonNull(log);
    }

    @Override
    public RegistryClient buildRegistryClient(RegistryConfig config) throws RegistryResolutionException {
        Objects.requireNonNull(config, "The registry config is null");

        final RegistryDescriptorConfig descriptorConfig = Objects.requireNonNull(config.getDescriptor(),
                "The registry descriptor configuration is missing");

        MavenArtifactResolver resolver = originalResolver;

        singleRegistryRepos.clear();
        determineExtraRepos(config, resolver.getRepositories());

        List<RemoteRepository> aggregatedRepos = resolver.getRepositories();
        if (!singleRegistryRepos.isEmpty()) {
            aggregatedRepos = resolver.getRemoteRepositoryManager().aggregateRepositories(resolver.getSession(),
                    Collections.emptyList(), singleRegistryRepos, true);
            aggregatedRepos = resolver.getRemoteRepositoryManager().aggregateRepositories(resolver.getSession(),
                    aggregatedRepos, resolver.getRepositories(), false);
            resolver = newResolver(resolver, aggregatedRepos, config, log);
        } else {
            resolver = newResolver(resolver, resolver.getRepositories(), config, log);
        }

        final boolean cleanupTimestampedArtifacts = isCleanupTimestampedArtifacts(config);

        final ArtifactCoords originalDescrCoords = descriptorConfig.getArtifact();
        final Artifact registryDescriptorCoords = new DefaultArtifact(originalDescrCoords.getGroupId(),
                originalDescrCoords.getArtifactId(), originalDescrCoords.getClassifier(), originalDescrCoords.getType(),
                originalDescrCoords.getVersion());
        ArtifactResult result;
        try {
            result = MavenRegistryArtifactResolverWithCleanup.resolveAndCleanupOldTimestampedVersions(resolver,
                    registryDescriptorCoords, cleanupTimestampedArtifacts);
        } catch (BootstrapMavenException e) {
            final StringWriter buf = new StringWriter();
            try (BufferedWriter writer = new BufferedWriter(buf)) {
                writer.write("Failed to resolve Quarkus extensions registry descriptor ");
                writer.write(registryDescriptorCoords.toString());
                writer.write(" using the following repositories:");
                for (RemoteRepository repo : aggregatedRepos) {
                    writer.newLine();
                    writer.write("- ");
                    writer.write(repo.getId());
                    writer.write(" (");
                    writer.write(repo.getUrl());
                    writer.write(")");
                }
            } catch (IOException e1) {
                buf.append(e.getLocalizedMessage());
            }
            throw new RegistryResolutionException(buf.toString());
        }

        final String srcRepoId = result.getRepository() == null ? "n/a" : result.getRepository().getId();
        log.debug("Resolved registry descriptor %s from %s", registryDescriptorCoords, srcRepoId);
        if (!singleRegistryRepos.isEmpty()) {
            if (srcRepoId != null && !"local".equals(srcRepoId)) {
                String srcRepoUrl = null;
                for (RemoteRepository repo : resolver.getRepositories()) {
                    if (repo.getId().equals(srcRepoId)) {
                        srcRepoUrl = repo.getUrl();
                        break;
                    }
                }
                if (srcRepoUrl == null) {
                    throw new IllegalStateException(
                            "Failed to locate the repository URL corresponding to repository " + srcRepoId);
                }
            } else {
                log.debug("Failed to determine the remote repository for %s registry descriptor %s", config.getId(),
                        registryDescriptorCoords);
            }
        }

        final RegistryConfig descriptor;
        try {
            descriptor = RegistriesConfigMapperHelper.deserialize(result.getArtifact().getFile().toPath(),
                    JsonRegistryConfig.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to deserialize registries descriptor " + result.getArtifact().getFile(), e);
        }

        if (!isComplete(config, descriptor)) {
            final JsonRegistryConfig complete = new JsonRegistryConfig();
            complete(complete, config, descriptor);
            config = complete;
        }

        MavenRegistryArtifactResolver defaultResolver = null;
        final RegistryNonPlatformExtensionsResolver nonPlatformExtensionsResolver;
        final RegistryNonPlatformExtensionsConfig nonPlatformExtensions = config.getNonPlatformExtensions();
        if (nonPlatformExtensions == null || nonPlatformExtensions.isDisabled()) {
            log.debug("Non-platform extension catalogs were disabled for registry %s", config.getId());
            nonPlatformExtensionsResolver = null;
        } else {
            nonPlatformExtensionsResolver = new MavenNonPlatformExtensionsResolver(nonPlatformExtensions,
                    defaultResolver = defaultResolver(resolver, cleanupTimestampedArtifacts), log);
        }

        final RegistryPlatformsResolver platformsResolver;
        final RegistryPlatformsConfig platformsConfig = config.getPlatforms();
        if (platformsConfig == null || platformsConfig.isDisabled()) {
            log.debug("Platform catalogs were disabled for registry %s", config.getId());
            platformsResolver = null;
        } else {
            platformsResolver = new MavenPlatformsResolver(platformsConfig,
                    defaultResolver == null ? defaultResolver = defaultResolver(resolver, cleanupTimestampedArtifacts)
                            : defaultResolver,
                    log);
        }

        return new RegistryClientDispatcher(config, platformsResolver,
                Boolean.TRUE.equals(config.getPlatforms().getExtensionCatalogsIncluded())
                        ? new MavenPlatformExtensionsResolver(
                                defaultResolver == null ? defaultResolver(resolver, cleanupTimestampedArtifacts)
                                        : defaultResolver,
                                log)
                        : new MavenPlatformExtensionsResolver(defaultResolver(originalResolver, cleanupTimestampedArtifacts),
                                log),
                nonPlatformExtensionsResolver);
    }

    private static boolean isCleanupTimestampedArtifacts(RegistryConfig config) {
        final Object o = config.getExtra().get(CLEANUP_TIMESTAMPED_ARTIFACTS);
        return o == null ? true : Boolean.parseBoolean(o.toString());
    }

    private static MavenRegistryArtifactResolver defaultResolver(MavenArtifactResolver resolver,
            boolean cleanupTimestampedArtifacts) {
        return new MavenRegistryArtifactResolverWithCleanup(resolver, cleanupTimestampedArtifacts);
    }

    private static void complete(JsonRegistryConfig complete, RegistryConfig original, RegistryConfig descriptor) {
        complete.setId(original.getId() == null ? descriptor.getId() : original.getId());

        if (original.getDescriptor() == null) {
            complete.setDescriptor(descriptor.getDescriptor());
        } else {
            complete.setDescriptor(original.getDescriptor());
        }
        if (original.getPlatforms() == null) {
            complete.setPlatforms(descriptor.getPlatforms());
        } else {
            complete.setPlatforms(complete(original.getPlatforms(), descriptor.getPlatforms()));
        }
        if (original.getNonPlatformExtensions() == null) {
            complete.setNonPlatformExtensions(descriptor.getNonPlatformExtensions());
        } else {
            complete.setNonPlatformExtensions(original.getNonPlatformExtensions());
        }

        if (original.getUpdatePolicy() == null) {
            complete.setUpdatePolicy(descriptor.getUpdatePolicy());
        } else {
            complete.setUpdatePolicy(original.getUpdatePolicy());
        }

        if (original.getMaven() == null) {
            complete.setMaven(descriptor.getMaven());
        } else if (isComplete(original.getMaven())) {
            complete.setMaven(original.getMaven());
        } else {
            final JsonRegistryMavenConfig completeMavenConfig = new JsonRegistryMavenConfig();
            complete.setMaven(completeMavenConfig);
            complete(completeMavenConfig, original.getMaven(),
                    descriptor.getMaven() == null ? completeMavenConfig : descriptor.getMaven());
        }
        if (original.getQuarkusVersions() == null) {
            complete.setQuarkusVersions(descriptor.getQuarkusVersions());
        }
    }

    private static RegistryPlatformsConfig complete(RegistryPlatformsConfig client, RegistryPlatformsConfig descriptor) {
        if (client == null) {
            return descriptor;
        }
        if (isComplete(client)) {
            return client;
        }
        JsonRegistryPlatformsConfig complete = new JsonRegistryPlatformsConfig();
        complete.setArtifact(client.getArtifact() == null ? descriptor.getArtifact() : client.getArtifact());
        complete.setDisabled(client.isDisabled());
        complete.setExtensionCatalogsIncluded(
                client.getExtensionCatalogsIncluded() == null ? descriptor.getExtensionCatalogsIncluded()
                        : client.getExtensionCatalogsIncluded());
        return complete;
    }

    private static void complete(JsonRegistryMavenConfig complete, RegistryMavenConfig original,
            RegistryMavenConfig descriptor) {
        if (original.getRepository() == null) {
            complete.setRepository(descriptor.getRepository());
        } else if (isComplete(original.getRepository()) || descriptor.getRepository() == null) {
            complete.setRepository(original.getRepository());
        } else {
            final JsonRegistryMavenRepoConfig completeRepo = new JsonRegistryMavenRepoConfig();
            complete.setRepository(completeRepo);
            complete(completeRepo, original.getRepository(), descriptor.getRepository());
        }
    }

    private static void complete(JsonRegistryMavenRepoConfig complete, RegistryMavenRepoConfig original,
            RegistryMavenRepoConfig descriptor) {
        complete.setId(original.getId() == null ? descriptor.getId() : original.getId());
        complete.setUrl(original.getUrl() == null ? descriptor.getUrl() : original.getUrl());
    }

    private static boolean isComplete(RegistryConfig client, RegistryConfig descriptor) {
        if (client.isDisabled()) {
            return true;
        }
        if (client.getDescriptor() == null) {
            return false;
        }
        if (!isComplete(client.getPlatforms(), descriptor.getPlatforms())) {
            return false;
        }
        if (!isComplete(client.getNonPlatformExtensions())) {
            return false;
        }
        if (!isComplete(client.getMaven())) {
            return false;
        }
        if (client.getQuarkusVersions() == null && descriptor.getQuarkusVersions() != null) {
            return false;
        }
        if (client.getUpdatePolicy() == null && descriptor.getUpdatePolicy() != null) {
            return false;
        }
        return true;
    }

    private static boolean isComplete(RegistryMavenConfig config) {
        if (config == null) {
            return false;
        }
        if (!isComplete(config.getRepository())) {
            return false;
        }
        return true;
    }

    private static boolean isComplete(RegistryPlatformsConfig client, RegistryPlatformsConfig descriptor) {
        if (!isComplete(client)) {
            return false;
        }
        if (descriptor != null && Boolean.TRUE.equals(descriptor.getExtensionCatalogsIncluded())
                && client.getExtensionCatalogsIncluded() == null) {
            return false;
        }
        return true;
    }

    private static boolean isComplete(RegistryArtifactConfig config) {
        if (config == null) {
            return false;
        }
        if (!config.isDisabled() && config.getArtifact() == null) {
            return false;
        }
        return true;
    }

    private static boolean isComplete(RegistryMavenRepoConfig config) {
        if (config == null) {
            return false;
        }
        if (config.getId() == null) {
            return false;
        }
        if (config.getUrl() == null) {
            return false;
        }
        return true;
    }

    private static MavenArtifactResolver newResolver(MavenArtifactResolver resolver, List<RemoteRepository> aggregatedRepos,
            RegistryConfig config, MessageWriter log) {
        try {
            final BootstrapMavenContext mvnCtx = new BootstrapMavenContext(
                    BootstrapMavenContext.config()
                            .setRepositorySystem(resolver.getSystem())
                            .setRepositorySystemSession(setRegistryTransferListener(config, log, resolver.getSession()))
                            .setRemoteRepositoryManager(resolver.getRemoteRepositoryManager())
                            .setRemoteRepositories(aggregatedRepos)
                            .setLocalRepository(resolver.getMavenContext().getLocalRepo())
                            .setCurrentProject(resolver.getMavenContext().getCurrentProject()));
            return new MavenArtifactResolver(mvnCtx);
        } catch (BootstrapMavenException e) {
            throw new IllegalStateException("Failed to initialize maven context", e);
        }
    }

    private static DefaultRepositorySystemSession setRegistryTransferListener(RegistryConfig config, MessageWriter log,
            RepositorySystemSession session) {
        final DefaultRepositorySystemSession newSession = new DefaultRepositorySystemSession(session);
        final TransferListener tl = newSession.getTransferListener();
        newSession.setTransferListener(new TransferListener() {

            boolean refreshingLocalCache;

            @Override
            public void transferInitiated(TransferEvent event) throws TransferCancelledException {
                if (!refreshingLocalCache) {
                    refreshingLocalCache = true;
                    log.info("Refreshing the local extension catalog cache of " + config.getId());
                }
                if (tl != null) {
                    tl.transferInitiated(event);
                }
            }

            @Override
            public void transferStarted(TransferEvent event) throws TransferCancelledException {
                if (tl != null) {
                    tl.transferStarted(event);
                }
            }

            @Override
            public void transferProgressed(TransferEvent event) throws TransferCancelledException {
                if (tl != null) {
                    tl.transferProgressed(event);
                }
            }

            @Override
            public void transferCorrupted(TransferEvent event) throws TransferCancelledException {
                if (tl != null) {
                    tl.transferCorrupted(event);
                }
            }

            @Override
            public void transferSucceeded(TransferEvent event) {
                if (tl != null) {
                    tl.transferSucceeded(event);
                }
            }

            @Override
            public void transferFailed(TransferEvent event) {
                if (tl != null) {
                    tl.transferFailed(event);
                }
            }
        });
        return newSession;
    }

    private void determineExtraRepos(RegistryConfig config,
            List<RemoteRepository> configuredRepos) {
        final RegistryMavenConfig mavenConfig = config.getMaven() == null ? null : config.getMaven();
        final RegistryMavenRepoConfig repoConfig = mavenConfig == null ? null : mavenConfig.getRepository();
        final String repoUrl = repoConfig == null || repoConfig.getUrl() == null
                ? Constants.DEFAULT_REGISTRY_BACKUP_MAVEN_REPO_URL
                : repoConfig.getUrl();
        addRegistryRepo(repoUrl, repoConfig == null || repoConfig.getId() == null ? config.getId() : repoConfig.getId(),
                config.getUpdatePolicy(),
                configuredRepos);
    }

    private void addRegistryRepo(final String repoUrl, String defaultRepoId, String updatePolicy,
            List<RemoteRepository> configuredRepos) {
        final Set<String> ids = new HashSet<>(configuredRepos.size());
        for (RemoteRepository repo : configuredRepos) {
            if (repo.getUrl().equals(repoUrl)) {
                return;
            }
            ids.add(repo.getId());
        }

        String repoId = defaultRepoId;
        if (ids.contains(repoId)) {
            int i = 2;
            String tmp;
            do {
                tmp = repoId + "-" + i++;
            } while (!ids.contains(tmp));
            repoId = tmp;
        }

        final RemoteRepository.Builder repoBuilder = new RemoteRepository.Builder(repoId, "default", repoUrl);

        if (updatePolicy != null) {
            if (updatePolicy.equalsIgnoreCase(RepositoryPolicy.UPDATE_POLICY_DAILY)
                    || updatePolicy.equalsIgnoreCase(RepositoryPolicy.UPDATE_POLICY_ALWAYS)
                    || updatePolicy.equalsIgnoreCase(RepositoryPolicy.UPDATE_POLICY_NEVER)
                    || updatePolicy.startsWith(RepositoryPolicy.UPDATE_POLICY_INTERVAL)) {
                repoBuilder.setPolicy(new RepositoryPolicy(true, updatePolicy, RepositoryPolicy.CHECKSUM_POLICY_WARN));
            } else {
                throw new IllegalStateException("Unrecognized update policy '" + updatePolicy + "' for repository " + repoId);
            }
        }

        singleRegistryRepos.add(repoBuilder.build());
    }
}
