package io.quarkus.axon.deployment;

public final class AggregateBuildItem extends AxonBuildItem {
    public AggregateBuildItem(Class<?> axonAnnotatedClass) {
        super(axonAnnotatedClass);
    }
}
