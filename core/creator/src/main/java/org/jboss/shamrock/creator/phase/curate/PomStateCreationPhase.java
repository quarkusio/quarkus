/**
 *
 */
package org.jboss.shamrock.creator.phase.curate;

import org.jboss.shamrock.creator.AppCreationPhase;
import org.jboss.shamrock.creator.AppCreator;
import org.jboss.shamrock.creator.AppCreatorException;
import org.jboss.shamrock.creator.config.reader.PropertiesHandler;
import org.jboss.shamrock.creator.outcome.OutcomeProviderRegistration;


/**
 *
 * @author Alexey Loubyansky
 */
public class PomStateCreationPhase implements AppCreationPhase<PomStateCreationPhase> {

    public static class Outcome {

    }

    @Override
    public void register(OutcomeProviderRegistration registration) throws AppCreatorException {
        registration.provides(Outcome.class);
    }

    @Override
    public void provideOutcome(AppCreator ctx) throws AppCreatorException {
/*
        final Path stateDir = ctx.createWorkDir("state");
        final Path statePom = stateDir.resolve("pom.xml");

        final AppArtifact appArtifact = ctx.getAppArtifact();
        final AppArtifact stateArtifact = Utils.getStateArtifact(appArtifact);

        final Model model = new Model();
        model.setModelVersion("4.0.0");

        model.setGroupId(stateArtifact.getGroupId());
        model.setArtifactId(stateArtifact.getArtifactId());
        model.setPackaging(stateArtifact.getType());
        model.setVersion(stateArtifact.getVersion());

        model.addProperty("creator.app.groupId", appArtifact.getGroupId());
        model.addProperty("creator.app.artifactId", appArtifact.getArtifactId());
        final String classifier = appArtifact.getClassifier();
        if (!classifier.isEmpty()) {
            model.addProperty("creator.app.classifier", classifier);
        }
        model.addProperty("creator.app.type", appArtifact.getType());
        model.addProperty("creator.app.version", appArtifact.getVersion());

        final Dependency appDep = new Dependency();
        appDep.setGroupId("${creator.app.groupId}");
        appDep.setArtifactId("${creator.app.artifactId}");
        if(!classifier.isEmpty()) {
            appDep.setClassifier("${creator.app.classifier}");
        }
        appDep.setType("${creator.app.type}");
        appDep.setVersion("${creator.app.version}");
        appDep.setScope("compile");
        model.addDependency(appDep);

        final Model appModel = Utils.readAppModel(ctx.getAppJar());
        final List<AppDependency> appDeps = Utils.getUpdateCandidates(appModel.getDependencies(), ctx.getArtifactResolver().collectDependencies(ctx.getAppArtifact()));
        for(AppDependency dep : appDeps) {
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
            if(updateClassifier != null && !updateClassifier.isEmpty()) {
                updateDep.setClassifier(updateClassifier);
            }
            updateDep.setType(depArtifact.getType());
            updateDep.setVersion(depArtifact.getVersion());
            updateDep.setScope(dep.getScope());

            model.addDependency(updateDep);
        }

        Utils.persistModel(statePom, model);

        ((AetherArtifactResolver)ctx.getArtifactResolver()).install(stateArtifact, statePom);
        //ctx.getArtifactResolver().relink(stateArtifact, statePom);

        ctx.pushOutcome(Outcome.class, new Outcome() {});
        */
    }

    @Override
    public String getConfigPropertyName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PropertiesHandler<PomStateCreationPhase> getPropertiesHandler() {
        // TODO Auto-generated method stub
        return null;
    }
}
