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

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;
import io.quarkus.creator.AppCreator;
import io.quarkus.creator.AppCreatorException;

/**
 *
 * @author Alexey Loubyansky
 */
public class CurateOutcome {

    static final String CREATOR_APP_GROUP_ID = "creator.app.groupId";
    static final String CREATOR_APP_ARTIFACT_ID = "creator.app.artifactId";
    static final String CREATOR_APP_CLASSIFIER = "creator.app.classifier";
    static final String CREATOR_APP_TYPE = "creator.app.type";
    static final String CREATOR_APP_VERSION = "creator.app.version";

    private static final Logger log = Logger.getLogger(CurateOutcome.class);

    public static class Builder {

        private AppArtifact stateArtifact;
        private AppModel appModel;
        private List<AppDependency> updatedDeps = Collections.emptyList();
        private AppModelResolver resolver;
        private List<Repository> artifactRepos = Collections.emptyList();
        private boolean loadedFromState;

        private Builder() {
        }

        public Builder setStateArtifact(AppArtifact stateArtifact) {
            this.stateArtifact = stateArtifact;
            return this;
        }

        public Builder setAppModelResolver(AppModelResolver resolver) {
            this.resolver = resolver;
            return this;
        }

        public Builder setAppModel(AppModel appModel) {
            this.appModel = appModel;
            return this;
        }

        public Builder setUpdatedDeps(List<AppDependency> deps) {
            this.updatedDeps = deps;
            return this;
        }

        public void setArtifactRepos(List<Repository> artifactRepos) {
            this.artifactRepos = artifactRepos;
        }

        public void setLoadedFromState() {
            this.loadedFromState = true;
        }

        public CurateOutcome build() {
            return new CurateOutcome(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    protected final AppArtifact stateArtifact;
    protected final AppModel initialModel;
    protected final List<AppDependency> updatedDeps;
    protected final AppModelResolver resolver;
    protected final List<Repository> artifactRepos;
    protected final boolean loadedFromState;
    protected AppModel effectiveModel;
    protected boolean persisted;

    public CurateOutcome(Builder builder) {
        this.stateArtifact = builder.stateArtifact;
        this.initialModel = builder.appModel;
        this.updatedDeps = builder.updatedDeps.isEmpty() ? builder.updatedDeps
                : Collections.unmodifiableList(builder.updatedDeps);
        this.resolver = builder.resolver;
        this.artifactRepos = builder.artifactRepos;
        this.loadedFromState = builder.loadedFromState;
    }

    public AppModelResolver getArtifactResolver() {
        return resolver;
    }

    public AppArtifact getAppArtifact() {
        return initialModel.getAppArtifact();
    }

    public AppModel getInitialModel() {
        return initialModel;
    }

    public boolean hasUpdatedDeps() {
        return !updatedDeps.isEmpty();
    }

    public List<AppDependency> getUpdatedDeps() {
        return updatedDeps;
    }

    public AppModel getEffectiveModel() throws AppCreatorException {
        if (effectiveModel != null) {
            return effectiveModel;
        }
        if (updatedDeps.isEmpty()) {
            return effectiveModel = initialModel;
        }
        try {
            return effectiveModel = resolver.resolveModel(initialModel.getAppArtifact(), updatedDeps);
        } catch (AppModelResolverException e) {
            throw new AppCreatorException("Failed to resolve effective application dependencies", e);
        }
    }

    public boolean isPersisted() {
        return persisted;
    }

    public void persist(AppCreator ctx) throws AppCreatorException {
        if (persisted || loadedFromState && !hasUpdatedDeps()) {
            log.info("Skipping provisioning state persistence");
            return;
        }
        log.info("Persisting provisioning state");

        final Path stateDir = ctx.createWorkDir("state");
        final Path statePom = stateDir.resolve("pom.xml");

        final AppArtifact appArtifact = initialModel.getAppArtifact();
        AppArtifact stateArtifact;
        if (this.stateArtifact == null) {
            stateArtifact = ModelUtils.getStateArtifact(appArtifact);
        } else {
            stateArtifact = new AppArtifact(this.stateArtifact.getGroupId(),
                    this.stateArtifact.getArtifactId(),
                    this.stateArtifact.getClassifier(),
                    this.stateArtifact.getType(),
                    String.valueOf(Long.valueOf(this.stateArtifact.getVersion()) + 1));
        }

        final Model model = new Model();
        model.setModelVersion("4.0.0");

        model.setGroupId(stateArtifact.getGroupId());
        model.setArtifactId(stateArtifact.getArtifactId());
        model.setPackaging(stateArtifact.getType());
        model.setVersion(stateArtifact.getVersion());

        model.addProperty(CREATOR_APP_GROUP_ID, appArtifact.getGroupId());
        model.addProperty(CREATOR_APP_ARTIFACT_ID, appArtifact.getArtifactId());
        final String classifier = appArtifact.getClassifier();
        if (!classifier.isEmpty()) {
            model.addProperty(CREATOR_APP_CLASSIFIER, classifier);
        }
        model.addProperty(CREATOR_APP_TYPE, appArtifact.getType());
        model.addProperty(CREATOR_APP_VERSION, appArtifact.getVersion());

        final Dependency appDep = new Dependency();
        appDep.setGroupId("${" + CREATOR_APP_GROUP_ID + "}");
        appDep.setArtifactId("${" + CREATOR_APP_ARTIFACT_ID + "}");
        if (!classifier.isEmpty()) {
            appDep.setClassifier("${" + CREATOR_APP_CLASSIFIER + "}");
        }
        appDep.setType("${" + CREATOR_APP_TYPE + "}");
        appDep.setVersion("${" + CREATOR_APP_VERSION + "}");
        appDep.setScope("compile");
        model.addDependency(appDep);

        if (!updatedDeps.isEmpty()) {
            for (AppDependency dep : getUpdatedDeps()) {
                final AppArtifact depArtifact = dep.getArtifact();
                final String groupId = depArtifact.getGroupId();

                final Exclusion exclusion = new Exclusion();
                exclusion.setGroupId(groupId);
                exclusion.setArtifactId(depArtifact.getArtifactId());
                appDep.addExclusion(exclusion);

                final Dependency updateDep = new Dependency();
                updateDep.setGroupId(groupId);
                updateDep.setArtifactId(depArtifact.getArtifactId());
                final String updateClassifier = depArtifact.getClassifier();
                if (updateClassifier != null && !updateClassifier.isEmpty()) {
                    updateDep.setClassifier(updateClassifier);
                }
                updateDep.setType(depArtifact.getType());
                updateDep.setVersion(depArtifact.getVersion());
                updateDep.setScope(dep.getScope());

                model.addDependency(updateDep);
            }
        }
        /*
         * if(!artifactRepos.isEmpty()) {
         * for(Repository repo : artifactRepos) {
         * model.addRepository(repo);
         * }
         * }
         */

        try {
            ModelUtils.persistModel(statePom, model);
            ((BootstrapAppModelResolver) resolver).install(stateArtifact, statePom);
        } catch (Exception e) {
            throw new AppCreatorException("Failed to persist application state artifact", e);
        }

        log.info("Persisted provisioning state as " + stateArtifact);
        //ctx.getArtifactResolver().relink(stateArtifact, statePom);
    }
}
