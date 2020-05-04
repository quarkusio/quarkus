package io.quarkus.builder;

import java.util.Set;

/**
 */
final class StepInfo {
    private final BuildStep buildStep;
    private final int dependencies;
    private final Set<StepInfo> dependents;
    private final Set<ItemId> consumes;
    private final Set<ItemId> produces;
    private final StepDependencyInfo dependencyInfo;

    StepInfo(final BuildStepBuilder builder, int dependencies, Set<StepInfo> dependents, StepDependencyInfo dependencyInfo) {
        buildStep = builder.getBuildStep();
        consumes = builder.getRealConsumes();
        produces = builder.getRealProduces();
        this.dependencies = dependencies;
        this.dependents = dependents;
        this.dependencyInfo = dependencyInfo;
    }

    BuildStep getBuildStep() {
        return buildStep;
    }

    int getDependencies() {
        return dependencies;
    }

    Set<StepInfo> getDependents() {
        return dependents;
    }

    Set<ItemId> getConsumes() {
        return consumes;
    }

    Set<ItemId> getProduces() {
        return produces;
    }

    public StepDependencyInfo getDependencyInfo() {
        return dependencyInfo;
    }
}
