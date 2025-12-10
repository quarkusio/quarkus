package io.quarkus.registry.client.maven;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactResult;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.maven.dependency.ArtifactCoords;
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
import io.quarkus.registry.config.RegistryQuarkusVersionsConfig;

public class MavenRegistryClientFactory implements RegistryClientFactory {

    private static final String CLEANUP_TIMESTAMPED_ARTIFACTS = "cleanup-timestamped-artifacts";

    private final MessageWriter log;
    private final MavenArtifactResolver originalResolver;

    public MavenRegistryClientFactory(MavenArtifactResolver resolver, MessageWriter log) {
        this.originalResolver = Objects.requireNonNull(resolver);
        this.log = Objects.requireNonNull(log);
    }

    @Override
    public RegistryClient buildRegistryClient(RegistryConfig config) throws RegistryResolutionException {
        Objects.requireNonNull(config, "The registry config is null");

        final Artifact registryDescriptorCoords = getDescriptorCoords(config);

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
            final List<RemoteRepository> aggregatedRepos = applyMirrorsAndProxies(registryRepos);
            resolver = newResolver(originalResolver, aggregatedRepos, config, log);
            try {
                result = MavenRegistryArtifactResolverWithCleanup.resolveAndCleanupOldTimestampedVersions(resolver,
                        registryDescriptorCoords, cleanupTimestampedArtifacts);
            } catch (BootstrapMavenException e) {
                if (areMatching(registryRepos, aggregatedRepos)) {
                    // the original and aggregated repos are matching, meaning no mirrors/proxies have been applied
                    // there is nothing to fallback to
                    throw new RegistryResolutionException(getDescriptorResolutionFailureMessage(config, resolver, e), e);
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

        final RegistryConfig.Mutable descriptor;
        try {
            // Do not fix or add any missing bits.
            descriptor = RegistryConfig.mutableFromFile(result.getArtifact().getFile().toPath());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to deserialize registries descriptor " + result.getArtifact().getFile(), e);
        }

        if (!isComplete(config, descriptor)) {
            config = completeRegistryConfig(config, descriptor);
        }

        final MavenRegistryArtifactResolver registryArtifactResolver = newRegistryArtifactResolver(resolver,
                cleanupTimestampedArtifacts);

        return new RegistryClientDispatcher(config,
                getPlatformsResolver(config, registryArtifactResolver),
                getPlatformExtensionsResolver(config, registryArtifactResolver, cleanupTimestampedArtifacts),
                getNonPlatformExtensionsResolver(config, registryArtifactResolver),
                new MavenRegistryCache(config, registryArtifactResolver, log));
    }

    private List<RemoteRepository> applyMirrorsAndProxies(List<RemoteRepository> registryRepos) {
        return originalResolver.getRemoteRepositoryManager().aggregateRepositories(
                originalResolver.getSession(), List.of(), registryRepos, true);
    }

    private MavenPlatformExtensionsResolver getPlatformExtensionsResolver(RegistryConfig config,
            MavenRegistryArtifactResolver registryArtifactResolver, boolean cleanupTimestampedArtifacts) {
        var platformsConfig = config.getPlatforms();
        if (platformsConfig != null && platformsConfig.getExtensionCatalogsIncluded() != null
                && platformsConfig.getExtensionCatalogsIncluded()) {
            // re-use the registry artifact resolver
            return new MavenPlatformExtensionsResolver(registryArtifactResolver, log);
        }
        final RegistryMavenRepoConfig repoConfig = platformsConfig == null ? null
                : (platformsConfig.getMaven() == null ? null
                        : (platformsConfig.getMaven().getRepository() == null ? null
                                : platformsConfig.getMaven().getRepository()));
        if (repoConfig != null) {
            // initialize a resolver with the repository configured by the registry
            final List<RemoteRepository> resolverRepos = applyMirrorsAndProxies(List.of(
                    new RemoteRepository.Builder(repoConfig.getId(), "default", repoConfig.getUrl()).build()));
            return new MavenPlatformExtensionsResolver(newRegistryArtifactResolver(
                    newResolver(originalResolver, resolverRepos, config, log), cleanupTimestampedArtifacts), log);
        }
        // use the original user Maven resolver
        return new MavenPlatformExtensionsResolver(newRegistryArtifactResolver(originalResolver, cleanupTimestampedArtifacts),
                log);
    }

    private RegistryPlatformsResolver getPlatformsResolver(RegistryConfig config,
            MavenRegistryArtifactResolver registryArtifactResolver) {
        final RegistryPlatformsConfig platformsConfig = config.getPlatforms();
        if (platformsConfig == null || platformsConfig.isDisabled()) {
            log.debug("Platform catalogs were disabled for registry %s", config.getId());
            return null;
        }
        return new MavenPlatformsResolver(platformsConfig, registryArtifactResolver, log);
    }

    private RegistryNonPlatformExtensionsResolver getNonPlatformExtensionsResolver(RegistryConfig config,
            MavenRegistryArtifactResolver registryArtifactResolver) {
        final RegistryNonPlatformExtensionsConfig nonPlatformExtensions = config.getNonPlatformExtensions();
        if (nonPlatformExtensions == null || nonPlatformExtensions.isDisabled()) {
            log.debug("Non-platform extension catalogs were disabled for registry %s", config.getId());
            return null;
        }
        return new MavenNonPlatformExtensionsResolver(nonPlatformExtensions, registryArtifactResolver, log);
    }

    private static Artifact getDescriptorCoords(RegistryConfig config) {
        final RegistryDescriptorConfig descriptorConfig = config.getDescriptor();
        if (descriptorConfig == null) {
            throw new IllegalArgumentException("The registry descriptor configuration is missing for " + config.getId());
        }
        final ArtifactCoords originalDescrCoords = descriptorConfig.getArtifact();
        return new DefaultArtifact(originalDescrCoords.getGroupId(),
                originalDescrCoords.getArtifactId(), originalDescrCoords.getClassifier(), originalDescrCoords.getType(),
                originalDescrCoords.getVersion());
    }

    private static boolean isCleanupTimestampedArtifacts(RegistryConfig config) {
        final Object o = config.getExtra().get(CLEANUP_TIMESTAMPED_ARTIFACTS);
        return o == null || Boolean.parseBoolean(o.toString());
    }

    private static MavenRegistryArtifactResolver newRegistryArtifactResolver(MavenArtifactResolver resolver,
            boolean cleanupTimestampedArtifacts) {
        return new MavenRegistryArtifactResolverWithCleanup(resolver, cleanupTimestampedArtifacts);
    }

    /**
     * Merges remote and local registry client configurations, prioritizing the local configuration values.
     *
     * @param local local client configuration
     * @param remote default configuration provided by the registry itself
     * @return complete registry client configuration
     */
    static RegistryConfig.Mutable completeRegistryConfig(RegistryConfig local, RegistryConfig remote) {
        RegistryConfig.Mutable complete = RegistryConfig.builder();

        complete.setId(local.getId() == null ? remote.getId() : local.getId());

        if (local.getDescriptor() == null) {
            complete.setDescriptor(remote.getDescriptor());
        } else {
            complete.setDescriptor(local.getDescriptor());
        }
        if (local.getPlatforms() == null) {
            complete.setPlatforms(remote.getPlatforms());
        } else {
            complete.setPlatforms(completeRegistryPlatformConfig(local.getPlatforms(), remote.getPlatforms()));
        }
        if (local.getNonPlatformExtensions() == null) {
            complete.setNonPlatformExtensions(remote.getNonPlatformExtensions());
        } else {
            complete.setNonPlatformExtensions(local.getNonPlatformExtensions());
        }
        if (local.getUpdatePolicy() == null) {
            complete.setUpdatePolicy(remote.getUpdatePolicy());
        } else {
            complete.setUpdatePolicy(local.getUpdatePolicy());
        }

        if (local.getMaven() == null) {
            complete.setMaven(remote.getMaven());
        } else if (isMavenConfigComplete(local.getMaven())) {
            complete.setMaven(local.getMaven());
        } else {
            complete.setMaven(RegistryMavenConfig.builder()
                    .setRepository(
                            completeMavenRepoConfig(local.getMaven().getRepository(), remote.getMaven().getRepository())));
        }

        complete.setQuarkusVersions(complete(local.getQuarkusVersions(), remote.getQuarkusVersions()));

        if (local.getExtra().isEmpty()) {
            complete.setExtra(remote.getExtra());
        } else if (remote.getExtra().isEmpty()) {
            complete.setExtra(local.getExtra());
        } else {
            var extra = new HashMap<>(remote.getExtra());
            extra.putAll(local.getExtra());
            complete.setExtra(extra);
        }
        return complete;
    }

    /**
     * Merges remote and local registry Quarkus versions configuration, prioritizing the local configuration values.
     *
     * @param local local configuration
     * @param remote default configuration provided by the registry
     * @return final configuration
     */
    private static RegistryQuarkusVersionsConfig complete(RegistryQuarkusVersionsConfig local,
            RegistryQuarkusVersionsConfig remote) {
        if (local == null) {
            return remote;
        }
        if (remote == null || local.equals(remote)) {
            return local;
        }
        var complete = RegistryQuarkusVersionsConfig.builder();
        complete.setExclusiveProvider(local.isExclusiveProvider() || remote.isExclusiveProvider());
        complete.setRecognizedGroupIds(local.getRecognizedGroupIds().isEmpty()
                ? remote.getRecognizedGroupIds()
                : local.getRecognizedGroupIds());
        complete.setRecognizedVersionsExpression(local.getRecognizedVersionsExpression() == null
                ? remote.getRecognizedVersionsExpression()
                : local.getRecognizedVersionsExpression());
        return complete;
    }

    private static RegistryPlatformsConfig completeRegistryPlatformConfig(RegistryPlatformsConfig local,
            RegistryPlatformsConfig remote) {
        if (local == null) {
            return remote;
        }
        if (isPlatformsConfigComplete(local, remote)) {
            return local;
        }

        Boolean extensionCatalogsIncluded = local.getExtensionCatalogsIncluded();
        RegistryMavenConfig mavenConfig = null;
        if (local.getMaven() != null && local.getMaven().getRepository() != null) {
            if (extensionCatalogsIncluded != null && extensionCatalogsIncluded) {
                throw new IllegalArgumentException(
                        "Either extension-catalogs-included or mavenConfig/repository configuration can be enabled at the same time");
            }
            mavenConfig = local.getMaven();
        }

        if (mavenConfig == null) {
            if (extensionCatalogsIncluded == null || !extensionCatalogsIncluded) {
                if (extensionCatalogsIncluded == null) {
                    extensionCatalogsIncluded = remote.getExtensionCatalogsIncluded();
                    if (extensionCatalogsIncluded == null || !extensionCatalogsIncluded) {
                        mavenConfig = remote.getMaven();
                    }
                }
            }
        } else if (remote.getMaven() != null && !isMavenConfigComplete(mavenConfig)) {
            mavenConfig = RegistryMavenConfig.builder()
                    .setRepository(completeMavenRepoConfig(mavenConfig.getRepository(), remote.getMaven().getRepository()));
        }

        return RegistryPlatformsConfig.builder()
                .setArtifact(local.getArtifact() == null ? remote.getArtifact() : local.getArtifact())
                .setDisabled(local.isDisabled())
                .setExtensionCatalogsIncluded(extensionCatalogsIncluded)
                .setMaven(mavenConfig);
    }

    private static RegistryMavenRepoConfig completeMavenRepoConfig(RegistryMavenRepoConfig local,
            RegistryMavenRepoConfig remote) {
        if (local == null) {
            return remote;
        }
        if (isMavenRepoComplete(local) || remote == null) {
            return local;
        }
        return RegistryMavenRepoConfig.builder()
                .setId(local.getId() != null ? local.getId() : remote.getId())
                .setUrl(local.getUrl() != null ? local.getUrl() : remote.getUrl());
    }

    private static boolean isComplete(RegistryConfig client, RegistryConfig descriptor) {
        if (!client.isEnabled()) {
            return true;
        }
        if (client.getDescriptor() == null) {
            return false;
        }
        if (!isPlatformsConfigComplete(client.getPlatforms(), descriptor.getPlatforms())) {
            return false;
        }
        if (!isArtifactConfigComplete(client.getNonPlatformExtensions())) {
            return false;
        }
        if (!isMavenConfigComplete(client.getMaven())) {
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

    private static boolean isMavenConfigComplete(RegistryMavenConfig config) {
        return config != null && isMavenRepoComplete(config.getRepository());
    }

    private static boolean isPlatformsConfigComplete(RegistryPlatformsConfig client, RegistryPlatformsConfig descriptor) {
        if (!isArtifactConfigComplete(client)) {
            return false;
        }
        if (client.getMaven() != null && client.getMaven().getRepository() != null
                && client.getMaven().getRepository().getUrl() != null
                || client.getExtensionCatalogsIncluded() != null) {
            return true;
        }
        if (descriptor != null) {
            return !Boolean.TRUE.equals(descriptor.getExtensionCatalogsIncluded()) && descriptor.getMaven() == null;
        }
        return true;
    }

    private static boolean isArtifactConfigComplete(RegistryArtifactConfig config) {
        return config != null && (config.isDisabled() || config.getArtifact() != null);
    }

    private static boolean isMavenRepoComplete(RegistryMavenRepoConfig config) {
        return config != null && config.getId() != null && config.getUrl() != null;
    }

    private static MavenArtifactResolver newResolver(MavenArtifactResolver resolver, List<RemoteRepository> aggregatedRepos,
            RegistryConfig config, MessageWriter log) {
        try {
            final LocalProject currentProject = resolver.getMavenContext().getCurrentProject();
            final BootstrapMavenContext mvnCtx = new BootstrapMavenContext(
                    BootstrapMavenContext.config()
                            .setRepositorySystem(resolver.getSystem())
                            .setRepositorySystemSession(setRegistryTransferListener(config, log, resolver.getSession()))
                            .setRemoteRepositoryManager(resolver.getRemoteRepositoryManager())
                            .setRemoteRepositories(aggregatedRepos)
                            .setLocalRepository(resolver.getMavenContext().getLocalRepo())
                            .setCurrentProject(currentProject)
                            // if the currentProject is null, workspace discovery should still be disabled
                            .setWorkspaceDiscovery(currentProject != null));
            return new MavenArtifactResolver(mvnCtx);
        } catch (BootstrapMavenException e) {
            throw new IllegalStateException("Failed to initialize Maven context", e);
        }
    }

    private static DefaultRepositorySystemSession setRegistryTransferListener(RegistryConfig config, MessageWriter log,
            RepositorySystemSession session) {
        final DefaultRepositorySystemSession newSession = new DefaultRepositorySystemSession(session);
        newSession.setTransferListener(new RegistryCacheRefreshLogger(config, log, newSession.getTransferListener()));
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

        return List.of(repoBuilder.build());
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
        buf.append(". Re-trying with the original ").append(config.getId()).append(" repository configuration.");
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
