package io.quarkus.deployment.builditem;

import java.util.function.BiFunction;

import io.quarkus.builder.item.MultiBuildItem;

public final class DevServicesCustomizerBuildItem extends MultiBuildItem {

    private final BiFunction<DevServicesResultBuildItem, Startable, Startable> customizer;

    public DevServicesCustomizerBuildItem(BiFunction<DevServicesResultBuildItem, Startable, Startable> customizer) {
        this.customizer = customizer;
    }

    public Startable apply(DevServicesResultBuildItem devservice, Startable startable) {
        return customizer.apply(devservice, startable);
    }
}
