package io.quarkus.arc.deployment;

import io.quarkus.arc.processor.QualifierRegistrar;
import io.quarkus.builder.item.MultiBuildItem;

/**
 * Makes it possible to register annotations that should be considered qualifiers but are not annotated with
 * {@code jakarta.inject.Qualifier}.
 */
public final class QualifierRegistrarBuildItem extends MultiBuildItem {

    private final QualifierRegistrar registrar;

    public QualifierRegistrarBuildItem(QualifierRegistrar registrar) {
        this.registrar = registrar;
    }

    public QualifierRegistrar getQualifierRegistrar() {
        return registrar;
    }
}
