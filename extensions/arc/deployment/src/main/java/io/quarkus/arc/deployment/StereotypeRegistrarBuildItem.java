package io.quarkus.arc.deployment;

import io.quarkus.arc.processor.StereotypeRegistrar;
import io.quarkus.builder.item.MultiBuildItem;

/**
 * Makes it possible to register annotations that should be considered stereotypes but are not annotated with
 * {@code jakarta.enterprise.inject.Stereotype}.
 */
public final class StereotypeRegistrarBuildItem extends MultiBuildItem {

    private final StereotypeRegistrar registrar;

    public StereotypeRegistrarBuildItem(StereotypeRegistrar registrar) {
        this.registrar = registrar;
    }

    public StereotypeRegistrar getStereotypeRegistrar() {
        return registrar;
    }
}
