package io.quarkus.smallrye.reactivemessaging.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class EmitterBuildItem extends MultiBuildItem {

    /**
     * The name of the stream the emitter is connected to.
     */
    private final String name;

    public EmitterBuildItem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
