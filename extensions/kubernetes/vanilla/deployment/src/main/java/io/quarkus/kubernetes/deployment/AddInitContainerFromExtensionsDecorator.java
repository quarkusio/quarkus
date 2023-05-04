package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.config.Container;
import io.dekorate.kubernetes.decorator.Decorator;

public class AddInitContainerFromExtensionsDecorator extends AddInitContainerDecorator {

    public AddInitContainerFromExtensionsDecorator(String deployment, Container container) {
        super(deployment, container);
    }

    public Class<? extends Decorator>[] before() {
        return new Class[] { AddInitContainerFromUserConfigDecorator.class };
    }
}
