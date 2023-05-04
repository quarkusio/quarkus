package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.config.Container;

public class AddInitContainerFromUserConfigDecorator extends AddInitContainerDecorator {

    public AddInitContainerFromUserConfigDecorator(String name, Container container) {
        super(name, container);
    }
}
