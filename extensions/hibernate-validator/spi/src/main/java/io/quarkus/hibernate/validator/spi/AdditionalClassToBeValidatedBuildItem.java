package io.quarkus.hibernate.validator.spi;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Meant to be used by extensions that generate classes that they nonetheless need validation for as these generated
 * classes would normally not be picked by the scanning process.
 */
public final class AdditionalClassToBeValidatedBuildItem extends MultiBuildItem {

    private final DotName dotName;

    public AdditionalClassToBeValidatedBuildItem(DotName dotName) {
        this.dotName = dotName;
    }

    public DotName getDotName() {
        return dotName;
    }
}
