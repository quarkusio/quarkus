package io.quarkus.deployment.ide;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Contains the IDE to be opened when a request to open a class is made
 */
public final class EffectiveIdeBuildItem extends SimpleBuildItem {

    private final Ide ide;

    public EffectiveIdeBuildItem(Ide ide) {
        this.ide = ide;
    }

    public Ide getIde() {
        return ide;
    }
}
