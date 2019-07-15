package io.quarkus.axon.deployment;

public final class EventHandlerBuildItem extends AxonBuildItem {
    public EventHandlerBuildItem(Class<?> axonAnnotatedClass) {
        super(axonAnnotatedClass);
    }
}
