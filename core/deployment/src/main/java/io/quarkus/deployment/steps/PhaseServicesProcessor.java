package io.quarkus.deployment.steps;

import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.annotations.BuildStep;

/**
 * Automatically registers synthetic void-like typed services for each Phase constant.
 * These serve as completion markers for their respective phases.
 */
public class PhaseServicesProcessor {

    @BuildStep
    public void registerPhaseServices(ServiceRegistrar reg) {
        for (Phase phase : Phase.values()) {
            var builder = reg.forService(Phase.class, phase.name())
                    .atPhase(phase);

            // To chain the phases, each phase (except STATIC_INIT) depends on the previous phase
            if (phase != Phase.STATIC_INIT) {
                Phase prevPhase = Phase.values()[phase.ordinal() - 1];
                builder.after(prevPhase);
            }

            // Register a service that returns the phase enum constant
            builder.onStart(ctx -> phase);
        }
    }
}
