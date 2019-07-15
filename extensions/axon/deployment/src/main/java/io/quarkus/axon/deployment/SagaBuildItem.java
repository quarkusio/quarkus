package io.quarkus.axon.deployment;

public final class SagaBuildItem extends AxonBuildItem {
    public SagaBuildItem(Class<?> axonAnnotatedClass) {
        super(axonAnnotatedClass);
    }
}
