package io.quarkus.builder;

import java.util.Set;

final class StepInfo {
    private final BuildStep buildStep;
    private final int dependencies;
    private final Set<StepInfo> dependents;
    private final Set<ItemId> consumes;
    private final Set<ItemId> produces;
    private final int ordinal;

    StepInfo(BuildStep buildStep, Set<ItemId> consumes, Set<ItemId> produces, int dependencies, Set<StepInfo> dependents,
            int ordinal) {
        this.buildStep = buildStep;
        this.consumes = consumes;
        this.produces = produces;
        this.dependencies = dependencies;
        this.dependents = dependents;
        this.ordinal = ordinal;
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

    /**
     * @return an index in the total topological ordering of all {@link StepInfo}s present in the given {@link Execution}.
     */
    int getOrdinal() {
        return ordinal;
    }

    @Override
    public String toString() {
        return buildStep.toString() + " (" + ordinal + ")";
    }
}
