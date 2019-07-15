package io.quarkus.axon.deployment;

public final class CommandHandlerBuildItem extends AxonBuildItem {
    public CommandHandlerBuildItem(Class<?> axonAnnotatedClass) {
        super(axonAnnotatedClass);
    }
}
