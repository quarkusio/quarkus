package io.quarkus.hibernate.orm.deployment;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Additional Jpa model class that we need to index
 *
 * @author Stéphane Épardaud
 */
public final class AdditionalJpaModelBuildItem extends MultiBuildItem {

    private final String className;

    public AdditionalJpaModelBuildItem(String className) {
        Objects.requireNonNull(className);
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
