package io.quarkus.registry.client.maven;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.maven.ArtifactCoords;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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

    public MavenRegistryClientFactory(MavenArtifactResolver resolver, MessageWriter log) {
        this.originalResolver = Objects.requireNonNull(resolver);
        this.log = Objects.requireNonNull(log);
    }

    @Override
    public RegistryClient buildRegistryClient(RegistryConfig config) throws RegistryResolutionException {
        Objects.requireNonNull(config, "The registry config is null");

        final RegistryDescriptorConfig descriptorConfig = config.getDescriptor();
        if (descriptorConfig == null) {
            throw new IllegalArgumentException("The registry descriptor configuration is missing for " + config.getId());
        }

        final ArtifactCoords originalDescrCoords = descriptorConfig.getArtifact();
        final Artifact registryDescriptorCoords = new DefaultArtifact(originalDescrCoords.getGroupId(),
                originalDescrCoords.getArtifactId(), originalDescrCoords.getClassifier(), originalDescrCoords.getType(),
                originalDescrCoords.getVersion());

        final boolean cleanupTimestampedArtifacts = isCleanupTimestampedArtifacts(config);

        // Determine the original registry Maven repository configuration
        // If the user settings already contain a Maven repository configuration with either an ID matching the registry ID
        // or a URL matching the registry URL, the original Maven resolver will be assumed to be already properly initialized.
        // Otherwise, a new registry Maven repository will be configured and a new resolver will be initialized for the registry.
        final List<RemoteRepository> registryRepos = determineRegistryRepos(config, originalResolver.getRepositories());
        MavenArtifactResolver resolver;
        ArtifactResult result;
        if (!registryRepos.isEmpty()) {
            // first, we try applying the mirrors and proxies found in the user settings
            final List<RemoteRepository> aggregatedRepos = originalResolver.getRemoteRepositoryManager().aggregateRepositories(
                    originalResolver.getSession(),
                    Collections.emptyList(), registryRepos, true);
            resolver = newResolver(originalResolver, aggregatedRepos, config, log);
            try {
                result = MavenRegistryArtifactResolverWithCleanup.resolveAndCleanupOldTimestampedVersions(resolver,
                        registryDescriptorCoords, cleanupTimestampedArtifacts);
            } catch (BootstrapMavenException e) {
                if (areMatching(registryRepos, aggregatedRepos)) {
                    // the original and aggregated repos are matching, meaning no mirrors/proxies have been applied
                    // there is nothing to fallback to
                    throw new RegistryResolutionException(getDescriptorResolutionFailureMessage(config, resolver, e));
                }
                // if the mirror and proxies in the user settings were configured w/o taking the extension registry into account
                // we will warn the user and try the original registry repos as a fallback
                log.warn(getDescriptorResolutionFailureFromMirrorMessage(config, resolver, e, registryRepos));
                resolver = newResolver(originalResolver, registryRepos, config, log);
                try {
                    result = MavenRegistryArtifactResolverWithCleanup.resolveAndCleanupOldTimestampedVersions(resolver,
                            registryDescriptorCoords, cleanupTimestampedArtifacts);
                } catch (BootstrapMavenException e1) {
                    throw new RegistryResolutionException(getDescriptorResolutionFailureMessage(config, resolver, e));
                }
            }
        } else {
            resolver = newResolver(originalResolver, originalResolver.getRepositories(), config, log);
            try {
                result = MavenRegistryArtifactResolverWithCleanup.resolveAndCleanupOldTimestampedVersions(resolver,
                        registryDescriptorCoords, cleanupTimestampedArtifacts);
            } catch (BootstrapMavenException e) {
                throw new RegistryResolutionException(getDescriptorResolutionFailureMessage(config, resolver, e));
            }
        }

        final String srcRepoId = result.getRepository() == null ? "n/a" : result.getRepository().getId();
        log.debug("Resolved registry descriptor %s from %s", registryDescriptorCoords, srcRepoId);
        if (!registryRepos.isEmpty()) {
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

        final MavenRegistryArtifactResolver defaultResolver = defaultResolver(resolver, cleanupTimestampedArtifacts);
        final RegistryNonPlatformExtensionsResolver nonPlatformExtensionsResolver;
        final RegistryNonPlatformExtensionsConfig nonPlatformExtensions = config.getNonPlatformExtensions();
        if (nonPlatformExtensions == null || nonPlatformExtensions.isDisabled()) {
            log.debug("Non-platform extension catalogs were disabled for registry %s", config.getId());
            nonPlatformExtensionsResolver = null;
        } else {
            nonPlatformExtensionsResolver = new MavenNonPlatformExtensionsResolver(nonPlatformExtensions, defaultResolver, log);
        }

        final RegistryPlatformsResolver platformsResolver;
        final RegistryPlatformsConfig platformsConfig = config.getPlatforms();
        if (platformsConfig == null || platformsConfig.isDisabled()) {
            log.debug("Platform catalogs were disabled for registry %s", config.getId());
            platformsResolver = null;
        } else {
            platformsResolver = new MavenPlatformsResolver(platformsConfig, defaultResolver, log);
        }

        return new RegistryClientDispatcher(config, platformsResolver,
                Boolean.TRUE.equals(platformsConfig == null ? Boolean.FALSE : platformsConfig.getExtensionCatalogsIncluded())
                        ? new MavenPlatformExtensionsResolver(defaultResolver, log)
                        : new MavenPlatformExtensionsResolver(defaultResolver(originalResolver, cleanupTimestampedArtifacts),
                                log),
                nonPlatformExtensionsResolver,
                new MavenRegistryCache(config, defaultResolver, log));
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
        if (!client.isEnabled()) {
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
            throw new IllegalStateException("Failed to initialize Maven context", e);
        }
    }

    private static DefaultRepositorySystemSession setRegistryTransferListener(RegistryConfig config, MessageWriter log,
            RepositorySystemSession session) {
        final DefaultRepositorySystemSession newSession = new DefaultRepositorySystemSession(session);
        final TransferListener tl = newSession.getTransferListener();
        newSession.setTransferListener(new TransferListener() {

            boolean loggedCatalogRefreshMsg;

            @Override
            public void transferInitiated(TransferEvent event) throws TransferCancelledException {
                if (!loggedCatalogRefreshMsg && !event.getResource().getResourceName()
                        .contains(config.getDescriptor().getArtifact().getArtifactId())) {
                    loggedCatalogRefreshMsg = true;
                    log.info("Looking for the newly published extensions in " + config.getId());
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

    private List<RemoteRepository> determineRegistryRepos(RegistryConfig config, List<RemoteRepository> configuredRepos) {
        final RegistryMavenConfig mavenConfig = config.getMaven() == null ? null : config.getMaven();
        final RegistryMavenRepoConfig repoConfig = mavenConfig == null ? null : mavenConfig.getRepository();
        final String repoId = repoConfig == null || repoConfig.getId() == null ? config.getId() : repoConfig.getId();
        String repoUrl = repoConfig == null ? null : repoConfig.getUrl();
        if (repoUrl == null || repoUrl.isBlank()) {
            // if the repo URL wasn't configured and there is a repository in the Maven config
            // whose ID matches the registry ID, this is what we are going to use
            for (RemoteRepository r : configuredRepos) {
                if (r.getId().equals(repoId)) {
                    return Collections.emptyList();
                }
            }
            // derive the repo URL from the registry ID
            try {
                repoUrl = new URL("https", config.getId(), "/maven").toExternalForm();
            } catch (MalformedURLException e) {
                throw new IllegalStateException("Failed to derive the Maven repository URL for registry " + config.getId(), e);
            }
        } else {
            // if the configured registry URL is already present in the Maven config
            // we are going use it
            for (RemoteRepository repo : configuredRepos) {
                if (repo.getUrl().equals(repoUrl)) {
                    return Collections.emptyList();
                }
            }
        }

        final RemoteRepository.Builder repoBuilder = new RemoteRepository.Builder(repoId, "default", repoUrl);

        final String updatePolicy = config.getUpdatePolicy();
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

        return Collections.singletonList(repoBuilder.build());
    }

    private static String getDescriptorResolutionFailureFromMirrorMessage(RegistryConfig config,
            MavenArtifactResolver resolver, BootstrapMavenException e, List<RemoteRepository> originalRegistryRepos) {
        final StringBuilder buf = new StringBuilder();
        buf.append(getDescriptorResolutionFailureMessage(config, resolver, e));
        buf.append(" having applied the mirrors and/or proxies from the Maven settings to ");
        appendRepoInfo(buf, originalRegistryRepos.get(0));
        for (int i = 1; i < originalRegistryRepos.size(); ++i) {
            buf.append(", ");
            appendRepoInfo(buf, originalRegistryRepos.get(i));
        }
        buf.append(". Re-trying with the original " + config.getId() + " repository configuration.");
        return buf.toString();
    }

    private static String getDescriptorResolutionFailureMessage(RegistryConfig config,
            MavenArtifactResolver resolver, BootstrapMavenException e) {
        final StringWriter buf = new StringWriter();
        try (BufferedWriter writer = new BufferedWriter(buf)) {
            writer.write("Failed to resolve the Quarkus extension registry descriptor of ");
            writer.write(config.getId());
            writer.write(" from ");
            final List<RemoteRepository> repos = resolver.getRepositories();
            appendRepoInfo(writer, repos.get(0));
            for (int i = 1; i < repos.size(); ++i) {
                writer.append(", ");
                appendRepoInfo(writer, repos.get(i));
            }
        } catch (IOException e1) {
            buf.append(e.getLocalizedMessage());
        }
        return buf.toString();
    }

    private static void appendRepoInfo(Appendable writer, RemoteRepository repo) {
        try {
            writer.append(repo.getId());
            writer.append(" (");
            writer.append(repo.getUrl());
            writer.append(")");
        } catch (Exception e) {
            throw new RuntimeException("Failed to compose an error message", e);
        }
    }

    private static boolean areMatching(final List<RemoteRepository> registryRepos,
            final List<RemoteRepository> aggregatedRepos) {
        if (registryRepos.size() != aggregatedRepos.size()) {
            return false;
        }
        for (int i = 0; i < registryRepos.size(); ++i) {
            final RemoteRepository original = registryRepos.get(i);
            final RemoteRepository aggregated = aggregatedRepos.get(i);
            if (!original.getId().equals(aggregated.getId()) || !original.getUrl().equals(aggregated.getUrl())) {
                return false;
            }
        }
        return true;
    }
}
