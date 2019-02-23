/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.creator.phase.curate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.jboss.logging.Logger;

import io.quarkus.creator.AppArtifact;
import io.quarkus.creator.AppArtifactResolver;
import io.quarkus.creator.AppCreationPhase;
import io.quarkus.creator.AppCreator;
import io.quarkus.creator.AppCreatorException;
import io.quarkus.creator.AppDependency;
import io.quarkus.creator.config.reader.MappedPropertiesHandler;
import io.quarkus.creator.config.reader.PropertiesConfigReaderException;
import io.quarkus.creator.config.reader.PropertiesHandler;
import io.quarkus.creator.outcome.OutcomeProviderRegistration;
import io.quarkus.creator.resolver.aether.AetherArtifactResolver;

/**
 *
 * @author Alexey Loubyansky
 */
public class CuratePhase implements AppCreationPhase<CuratePhase> {

    public static final String CONFIG_PROP = "curate";
    public static final String CONFIG_PROP_DEPS_ORIGIN = "dependencies-origin";
    public static final String CONFIG_PROP_LOCAL_REPO = "local-repo";
    public static final String CONFIG_PROP_VERSION_UPDATE = "version-update";
    public static final String CONFIG_PROP_VERSION_UPDATE_NUMBER = "version-update-number";
    public static final String CONFIG_PROP_UPDATE_GROUP_ID = "update-groupId";

    private static final String GROUP_ID_SPLIT_EXPR = "\\s*(,|\\s)\\s*";

    public static String completePropertyName(String name) {
        return CONFIG_PROP + '.' + name;
    }

    private static final Logger log = Logger.getLogger(CuratePhase.class);

    private DependenciesOrigin depsOrigin = DependenciesOrigin.APPLICATION;
    private VersionUpdate update = VersionUpdate.NONE;
    private VersionUpdateNumber updateNumber = VersionUpdateNumber.MICRO;
    private Path localRepo;
    private Set<String> updateGroupIds = Collections.singleton("io.quarkus");

    public void setInitialDeps(DependenciesOrigin initialDeps) {
        this.depsOrigin = initialDeps;
    }

    public void setUpdate(VersionUpdate update) {
        this.update = update;
    }

    public void setLocalRepo(Path localRepo) {
        this.localRepo = localRepo;
    }

    @Override
    public String getConfigPropertyName() {
        return CONFIG_PROP;
    }

    @Override
    public PropertiesHandler<CuratePhase> getPropertiesHandler() {
        return new MappedPropertiesHandler<CuratePhase>() {
            @Override
            public CuratePhase getTarget() throws PropertiesConfigReaderException {
                return CuratePhase.this;
            }
        }
                .map(CONFIG_PROP_DEPS_ORIGIN, (target, value) -> {
                    depsOrigin = DependenciesOrigin.of(value);
                    if (depsOrigin == DependenciesOrigin.UNKNOWN) {
                        throw new PropertiesConfigReaderException("The value of initial-deps property is expected to be either "
                                + DependenciesOrigin.APPLICATION + " or " + DependenciesOrigin.LAST_UPDATE + " but was "
                                + value);
                    }
                })
                .map(CONFIG_PROP_LOCAL_REPO, (target, value) -> {
                    localRepo = Paths.get(value);
                })
                .map(CONFIG_PROP_VERSION_UPDATE, (target, value) -> {
                    update = VersionUpdate.of(value);
                    if (update == VersionUpdate.UNKNOWN) {
                        throw new PropertiesConfigReaderException("The value of update property is expected to be one of "
                                + VersionUpdate.LATEST + ", " + VersionUpdate.NEXT + " or " + VersionUpdate.NONE + " but was "
                                + value);
                    }
                })
                .map(CONFIG_PROP_VERSION_UPDATE_NUMBER, (target, value) -> {
                    updateNumber = VersionUpdateNumber.of(value);
                    if (updateNumber == VersionUpdateNumber.UNKNOWN) {
                        throw new PropertiesConfigReaderException(
                                "The value of update-number property is expected to be one of "
                                        + VersionUpdateNumber.MAJOR + ", " + VersionUpdateNumber.MINOR + " or "
                                        + VersionUpdateNumber.MICRO + " but was " + value);
                    }
                })
                .map(CONFIG_PROP_UPDATE_GROUP_ID, (target, value) -> {
                    updateGroupIds = new HashSet<>(Arrays.asList(value.split(GROUP_ID_SPLIT_EXPR)));
                });
    }

    @Override
    public void register(OutcomeProviderRegistration registration) throws AppCreatorException {
        registration.provides(CurateOutcome.class);
    }

    @Override
    public void provideOutcome(AppCreator ctx) throws AppCreatorException {

        log.info("provideOutcome depsOrigin=" + depsOrigin + ", versionUpdate=" + update + ", versionUpdateNumber="
                + updateNumber);

        final Path appJar = ctx.getAppJar();
        if (appJar == null) {
            throw new AppCreatorException("Application JAR has not been provided");
        }
        if (!Files.exists(appJar)) {
            throw new AppCreatorException("Application " + appJar + " does not exist on disk");
        }

        final CurateOutcome.Builder outcome = CurateOutcome.builder();

        final AppArtifact appArtifact = Utils.resolveAppArtifact(appJar);
        outcome.setAppArtifact(appArtifact);

        AppArtifactResolver resolver = ctx.getArtifactResolver();
        if (resolver == null) {
            final AetherArtifactResolver aetherResolver = AetherArtifactResolver
                    .getInstance(this.localRepo == null ? ctx.getWorkPath("repo") : this.localRepo);
            aetherResolver.relink(appArtifact, appJar);
            final List<RemoteRepository> artifactRepos = aetherResolver.resolveArtifactRepos(appArtifact);
            if (!artifactRepos.isEmpty()) {
                aetherResolver.addRemoteRepositories(artifactRepos);
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
            resolver = aetherResolver;
        } else {
            resolver.relink(appArtifact, appJar);
        }
        outcome.setArtifactResolver(resolver);

        final List<AppDependency> initialDepsList;
        if (depsOrigin == DependenciesOrigin.LAST_UPDATE) {
            log.info("Looking for the state of the last update");
            Path statePath = null;
            try {
                AppArtifact stateArtifact = Utils.getStateArtifact(appArtifact);
                final String latest = resolver.getLatestVersion(stateArtifact, null, false);
                if (!stateArtifact.getVersion().equals(latest)) {
                    stateArtifact = new AppArtifact(stateArtifact.getGroupId(),
                            stateArtifact.getArtifactId(),
                            stateArtifact.getClassifier(),
                            stateArtifact.getType(),
                            latest);
                }
                statePath = resolver.resolve(stateArtifact);
                outcome.setStateArtifact(stateArtifact);
                log.info("- located the state at " + statePath);
            } catch (AppCreatorException e) {
                // for now let's assume this means artifact does not exist
                //System.out.println(" no state found");
            }

            if (statePath != null) {
                try {
                    final Model model = Utils.readModel(statePath);
                    /*
                     * final Properties props = model.getProperties();
                     * final String appGroupId = props.getProperty(CurateOutcome.CREATOR_APP_GROUP_ID);
                     * final String appArtifactId = props.getProperty(CurateOutcome.CREATOR_APP_ARTIFACT_ID);
                     * final String appClassifier = props.getProperty(CurateOutcome.CREATOR_APP_CLASSIFIER);
                     * final String appType = props.getProperty(CurateOutcome.CREATOR_APP_TYPE);
                     * final String appVersion = props.getProperty(CurateOutcome.CREATOR_APP_VERSION);
                     * final AppArtifact modelAppArtifact = new AppArtifact(appGroupId, appArtifactId, appClassifier, appType,
                     * appVersion);
                     */
                    final List<Dependency> modelStateDeps = model.getDependencies();
                    final List<AppDependency> updatedDeps = new ArrayList<>(modelStateDeps.size());
                    final String groupIdProp = "${" + CurateOutcome.CREATOR_APP_GROUP_ID + "}";
                    for (Dependency modelDep : modelStateDeps) {
                        if (modelDep.getGroupId().equals(groupIdProp)) {
                            continue;
                        }
                        updatedDeps.add(new AppDependency(new AppArtifact(modelDep.getGroupId(), modelDep.getArtifactId(),
                                modelDep.getClassifier(), modelDep.getType(), modelDep.getVersion()), modelDep.getScope()));
                    }
                    initialDepsList = resolver.collectDependencies(appArtifact, updatedDeps);
                    outcome.setLoadedFromState();
                } catch (IOException e) {
                    throw new AppCreatorException("Failed to load application state POM " + statePath, e);
                }
            } else {
                initialDepsList = resolver.collectDependencies(appArtifact);
            }
        } else {
            initialDepsList = resolver.collectDependencies(appArtifact);
        }

        //logDeps("INITIAL:", initialDepsList);

        outcome.setInitialDeps(initialDepsList);
        if (update == VersionUpdate.NONE) {
            ctx.pushOutcome(outcome.build());
            return;
        }

        log.info("Checking for available updates");
        final List<AppDependency> appDeps = Utils.getUpdateCandidates(Utils.readAppModel(appJar, appArtifact).getDependencies(),
                initialDepsList, updateGroupIds);
        final UpdateDiscovery ud = new DefaultUpdateDiscovery(resolver, updateNumber);
        List<AppDependency> availableUpdates = null;
        int i = 0;
        while (i < appDeps.size()) {
            final AppDependency dep = appDeps.get(i++);
            final AppArtifact depArtifact = dep.getArtifact();
            final String updatedVersion = update == VersionUpdate.NEXT ? ud.getNextVersion(depArtifact)
                    : ud.getLatestVersion(depArtifact);
            if (depArtifact.getVersion().equals(updatedVersion)) {
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
            ctx.pushOutcome(outcome.build());
        } else {
            log.info("- no updates available");
            ctx.pushOutcome(outcome.build());
        }
    }

    private static org.apache.maven.model.RepositoryPolicy toMavenRepoPolicy(RepositoryPolicy policy) {
        final org.apache.maven.model.RepositoryPolicy mvnPolicy = new org.apache.maven.model.RepositoryPolicy();
        mvnPolicy.setEnabled(policy.isEnabled());
        mvnPolicy.setChecksumPolicy(policy.getChecksumPolicy());
        mvnPolicy.setUpdatePolicy(policy.getUpdatePolicy());
        return mvnPolicy;
    }

    private static void logDeps(String header, List<AppDependency> deps) {
        final List<String> list = new ArrayList<>(deps.size());
        for (AppDependency dep : deps) {
            list.add(dep.toString());
        }
        Collections.sort(list);
        System.out.println(header);
        for (String str : list) {
            System.out.println("- " + str);
        }
    }
}
