package io.quarkus.bootstrap.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.BootstrapAppModelFactory;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;

public class CurationResult {

    private static final Logger log = Logger.getLogger(CurationResult.class);

    private final AppModel appModel;
    private final List<AppDependency> updatedDependencies;
    private final boolean fromState;
    private final AppArtifact appArtifact;
    private final AppArtifact stateArtifact;
    private boolean persisted;

    public CurationResult(AppModel appModel) {
        this(appModel, Collections.emptyList(), false, null, null);
    }

    public CurationResult(AppModel appModel, List<AppDependency> updatedDependencies, boolean fromState,
                          AppArtifact appArtifact, AppArtifact stateArtifact) {
        this.appModel = appModel;
        this.updatedDependencies = updatedDependencies;
        this.fromState = fromState;
        this.appArtifact = appArtifact;
        this.stateArtifact = stateArtifact;
    }

    public AppModel getAppModel() {
        return appModel;
    }

    public List<AppDependency> getUpdatedDependencies() {
        return updatedDependencies;
    }

    public boolean isFromState() {
        return fromState;
    }

    public AppArtifact getStateArtifact() {
        return stateArtifact;
    }

    public boolean hasUpdatedDeps() {
        return !updatedDependencies.isEmpty();
    }

    public void persist(AppModelResolver resolver) {
        if (persisted || fromState && !hasUpdatedDeps()) {
            log.info("Skipping provisioning state persistence");
            return;
        }
        log.info("Persisting provisioning state");

        final Path stateDir;
        try {
            stateDir = Files.createTempDirectory("quarkus-state");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final Path statePom = stateDir.resolve("pom.xml");

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

        model.addProperty(BootstrapAppModelFactory.CREATOR_APP_GROUP_ID, appArtifact.getGroupId());
        model.addProperty(BootstrapAppModelFactory.CREATOR_APP_ARTIFACT_ID, appArtifact.getArtifactId());
        final String classifier = appArtifact.getClassifier();
        if (!classifier.isEmpty()) {
            model.addProperty(BootstrapAppModelFactory.CREATOR_APP_CLASSIFIER, classifier);
        }
        model.addProperty(BootstrapAppModelFactory.CREATOR_APP_TYPE, appArtifact.getType());
        model.addProperty(BootstrapAppModelFactory.CREATOR_APP_VERSION, appArtifact.getVersion());

        final Dependency appDep = new Dependency();
        appDep.setGroupId("${" + BootstrapAppModelFactory.CREATOR_APP_GROUP_ID + "}");
        appDep.setArtifactId("${" + BootstrapAppModelFactory.CREATOR_APP_ARTIFACT_ID + "}");
        if (!classifier.isEmpty()) {
            appDep.setClassifier("${" + BootstrapAppModelFactory.CREATOR_APP_CLASSIFIER + "}");
        }
        appDep.setType("${" + BootstrapAppModelFactory.CREATOR_APP_TYPE + "}");
        appDep.setVersion("${" + BootstrapAppModelFactory.CREATOR_APP_VERSION + "}");
        appDep.setScope("compile");
        model.addDependency(appDep);

        if (!updatedDependencies.isEmpty()) {
            for (AppDependency dep : updatedDependencies) {
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
            throw new RuntimeException("Failed to persist application state artifact", e);
        }
        persisted = true;

        log.info("Persisted provisioning state as " + stateArtifact);
        //ctx.getArtifactResolver().relink(stateArtifact, statePom);
    }

}
