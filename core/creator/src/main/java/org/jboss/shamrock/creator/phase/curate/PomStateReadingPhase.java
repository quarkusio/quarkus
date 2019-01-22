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
public class PomStateReadingPhase implements AppCreationPhase<PomStateReadingPhase> {

    public static class Outcome {

    }

    @Override
    public void register(OutcomeProviderRegistration registration) throws AppCreatorException {
        registration.provides(Outcome.class);
    }

    @Override
    public void provideOutcome(AppCreator ctx) throws AppCreatorException {
/*
        System.out.println("PomStateReadingPhase.process");

        ctx.resolveOutcome(PomStateCreationPhase.Outcome.class);

        final AppArtifact stateArtifact = Utils.getStateArtifact(ctx.getAppArtifact());
        List<AppDependency> stateDeps = ctx.getArtifactResolver().collectDependencies(stateArtifact);
        for(AppDependency stateDep : stateDeps) {
            System.out.println("- " + stateDep);
        }

        ctx.pushOutcome(Outcome.class, new Outcome() {});
        */
    }

    @Override
    public String getConfigPropertyName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PropertiesHandler<PomStateReadingPhase> getPropertiesHandler() {
        // TODO Auto-generated method stub
        return null;
    }
}
