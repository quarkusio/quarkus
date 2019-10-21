package io.quarkus.creator.curator;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.BootstrapDependencyProcessingException;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;
import io.quarkus.bootstrap.util.ZipUtils;
import io.quarkus.creator.AppCreatorException;
import io.quarkus.creator.CuratedApplicationCreator;
import io.quarkus.creator.DependenciesOrigin;
import io.quarkus.creator.VersionUpdate;

/**
 *
 * @author Alexey Loubyansky
 */
public class Curator {

    private static final Logger log = Logger.getLogger(Curator.class);

    public static CurateOutcome run(CuratedApplicationCreator ctx) throws AppCreatorException {

        log.info("provideOutcome depsOrigin=" + ctx.getDepsOrigin() + ", versionUpdate=" + ctx.getUpdate()
                + ", versionUpdateNumber="
                + ctx.getUpdateNumber());

        final AppArtifact appArtifact = ctx.getAppArtifact();
        if (appArtifact == null) {
            throw new AppCreatorException("Application artifact has not been provided");
        }
        Path appJar;
        try {
            appJar = ctx.getArtifactResolver().resolve(appArtifact);
        } catch (AppModelResolverException e) {
            throw new AppCreatorException("Failed to resolve artifact", e);
        }
        if (!Files.exists(appJar)) {
            throw new AppCreatorException("Application " + appJar + " does not exist on disk");
        }

        final CurateOutcome.Builder outcome = CurateOutcome.builder();

        AppModelResolver modelResolver = ctx.getArtifactResolver();
        final AppModel initialDepsList;
        try {
            if (modelResolver == null) {
                final BootstrapAppModelResolver bsResolver = new BootstrapAppModelResolver(
                        MavenArtifactResolver.builder()
                                .setRepoHome(ctx.getLocalRepo() == null ? ctx.getWorkPath("repo") : ctx.getLocalRepo())
                                .build());
                bsResolver.relink(appArtifact, appJar);
                final List<RemoteRepository> artifactRepos = bsResolver.resolveArtifactRepos(appArtifact);
                if (!artifactRepos.isEmpty()) {
                    bsResolver.addRemoteRepositories(artifactRepos);
                    final List<Repository> modelRepos = new ArrayList<>(artifactRepos.size());
                    for (RemoteRepository repo : artifactRepos) {
                        final Repository modelRepo = new Repository();
                        modelRepo.setId(repo.getId());
                        modelRepo.setUrl(repo.getUrl());
                        modelRepo.setLayout(repo.getContentType());
                        RepositoryPolicy policy = repo.getPolicy(true);
                        if (policy != null) {
                            modelRepo.setSnapshots(toMavenRepoPolicy(policy));
                        }
                        policy = repo.getPolicy(false);
                        if (policy != null) {
                            modelRepo.setReleases(toMavenRepoPolicy(policy));
                        }
                        modelRepos.add(modelRepo);
                    }
                    outcome.setArtifactRepos(modelRepos);
                }
                modelResolver = bsResolver;
            } else {
                modelResolver.relink(appArtifact, appJar);
            }
            outcome.setAppModelResolver(modelResolver);

            if (ctx.getDepsOrigin() == DependenciesOrigin.LAST_UPDATE) {
                log.info("Looking for the state of the last update");
                Path statePath = null;
                try {
                    AppArtifact stateArtifact = ModelUtils.getStateArtifact(appArtifact);
                    final String latest = modelResolver.getLatestVersion(stateArtifact, null, false);
                    if (!stateArtifact.getVersion().equals(latest)) {
                        stateArtifact = new AppArtifact(stateArtifact.getGroupId(), stateArtifact.getArtifactId(),
                                stateArtifact.getClassifier(), stateArtifact.getType(), latest);
                    }
                    statePath = modelResolver.resolve(stateArtifact);
                    outcome.setStateArtifact(stateArtifact);
                    log.info("- located the state at " + statePath);
                } catch (AppModelResolverException e) {
                    // for now let's assume this means artifact does not exist
                    // System.out.println(" no state found");
                }

                if (statePath != null) {
                    Model model;
                    try {
                        model = ModelUtils.readModel(statePath);
                    } catch (IOException e) {
                        throw new AppCreatorException("Failed to read application state " + statePath, e);
                    }
                    /*
                     * final Properties props = model.getProperties(); final String appGroupId =
                     * props.getProperty(CurateOutcome.CREATOR_APP_GROUP_ID); final String appArtifactId =
                     * props.getProperty(CurateOutcome.CREATOR_APP_ARTIFACT_ID); final String appClassifier =
                     * props.getProperty(CurateOutcome.CREATOR_APP_CLASSIFIER); final String appType =
                     * props.getProperty(CurateOutcome.CREATOR_APP_TYPE); final String appVersion =
                     * props.getProperty(CurateOutcome.CREATOR_APP_VERSION); final AppArtifact modelAppArtifact = new
                     * AppArtifact(appGroupId, appArtifactId, appClassifier, appType, appVersion);
                     */
                    final List<Dependency> modelStateDeps = model.getDependencies();
                    final List<AppDependency> updatedDeps = new ArrayList<>(modelStateDeps.size());
                    final String groupIdProp = "${" + CurateOutcome.CREATOR_APP_GROUP_ID + "}";
                    for (Dependency modelDep : modelStateDeps) {
                        if (modelDep.getGroupId().equals(groupIdProp)) {
                            continue;
                        }
                        updatedDeps.add(new AppDependency(new AppArtifact(modelDep.getGroupId(), modelDep.getArtifactId(),
                                modelDep.getClassifier(), modelDep.getType(), modelDep.getVersion()), modelDep.getScope(),
                                modelDep.isOptional()));
                    }
                    initialDepsList = modelResolver.resolveModel(appArtifact, updatedDeps);
                    outcome.setLoadedFromState();
                } else {
                    initialDepsList = modelResolver.resolveModel(appArtifact);
                }
            } else {
                initialDepsList = modelResolver.resolveModel(appArtifact);
            }
        } catch (AppModelResolverException e) {
            throw new AppCreatorException("Failed to resolve initial application dependencies", e);
        }

        outcome.setAppModel(initialDepsList);
        if (ctx.getUpdate() == VersionUpdate.NONE) {
            return outcome.build();
        }

        log.info("Checking for available updates");
        List<AppDependency> appDeps;
        try {
            appDeps = modelResolver.resolveUserDependencies(appArtifact, initialDepsList.getUserDependencies());
        } catch (AppModelResolverException | BootstrapDependencyProcessingException e) {
            throw new AppCreatorException("Failed to determine the list of dependencies to update", e);
        }
        final Iterator<AppDependency> depsI = appDeps.iterator();
        while (depsI.hasNext()) {
            final AppArtifact appDep = depsI.next().getArtifact();
            if (!appDep.getType().equals(AppArtifact.TYPE_JAR)) {
                depsI.remove();
                continue;
            }
            final Path path = appDep.getPath();
            if (Files.isDirectory(path)) {
                if (!Files.exists(path.resolve(BootstrapConstants.DESCRIPTOR_PATH))) {
                    depsI.remove();
                }
            } else {
                try (FileSystem artifactFs = ZipUtils.newFileSystem(path)) {
                    if (!Files.exists(artifactFs.getPath(BootstrapConstants.DESCRIPTOR_PATH))) {
                        depsI.remove();
                    }
                } catch (IOException e) {
                    throw new AppCreatorException("Failed to open " + path, e);
                }
            }
        }

        final UpdateDiscovery ud = new DefaultUpdateDiscovery(modelResolver, ctx.getUpdateNumber());
        List<AppDependency> availableUpdates = null;
        int i = 0;
        while (i < appDeps.size()) {
            final AppDependency dep = appDeps.get(i++);
            final AppArtifact depArtifact = dep.getArtifact();
            final String updatedVersion = ctx.getUpdate() == VersionUpdate.NEXT ? ud.getNextVersion(depArtifact)
                    : ud.getLatestVersion(depArtifact);
            if (updatedVersion == null || depArtifact.getVersion().equals(updatedVersion)) {
                continue;
            }
            log.info(dep.getArtifact() + " -> " + updatedVersion);
            if (availableUpdates == null) {
                availableUpdates = new ArrayList<>();
            }
            availableUpdates.add(new AppDependency(new AppArtifact(depArtifact.getGroupId(), depArtifact.getArtifactId(),
                    depArtifact.getClassifier(), depArtifact.getType(), updatedVersion), dep.getScope()));
        }

        if (availableUpdates != null) {
            outcome.setUpdatedDeps(availableUpdates);
            return outcome.build();
        } else {
            log.info("- no updates available");
            return outcome.build();
        }
    }

    private static org.apache.maven.model.RepositoryPolicy toMavenRepoPolicy(RepositoryPolicy policy) {
        final org.apache.maven.model.RepositoryPolicy mvnPolicy = new org.apache.maven.model.RepositoryPolicy();
        mvnPolicy.setEnabled(policy.isEnabled());
        mvnPolicy.setChecksumPolicy(policy.getChecksumPolicy());
        mvnPolicy.setUpdatePolicy(policy.getUpdatePolicy());
        return mvnPolicy;
    }
}
