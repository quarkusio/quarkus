package io.quarkus.kubernetes.deployment;

import io.dekorate.deps.kubernetes.api.model.ContainerBuilder;
import io.dekorate.kubernetes.annotation.Protocol;
import io.dekorate.kubernetes.config.Port;
import io.dekorate.kubernetes.decorator.AddSidecarDecorator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;

/**
 * Adapted from Dekorate's {@link io.dekorate.kubernetes.decorator.AddPortDecorator} in order to
 * address the limitation that the port was not being added (due to missing metadata)
 */
class AddContainerPortDecorator extends Decorator<ContainerBuilder> {

    private final Port port;

    AddContainerPortDecorator(Port port) {
        this.port = port;
    }

    @Override
    public void visit(ContainerBuilder containerBuilder) {
        containerBuilder.addNewPort()
                .withName(port.getName())
                .withHostPort(port.getHostPort() > 0 ? port.getHostPort() : null)
                .withContainerPort(port.getContainerPort())
                .withProtocol(port.getProtocol() != null ? port.getProtocol().name() : Protocol.TCP.name())
                .endPort();
    }

    public Class<? extends Decorator>[] after() {
        return new Class[] { ResourceProvidingDecorator.class, AddSidecarDecorator.class };
    }
}
