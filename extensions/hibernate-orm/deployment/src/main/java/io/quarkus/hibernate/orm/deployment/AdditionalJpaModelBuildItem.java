package io.quarkus.hibernate.orm.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Additional Jpa model class that we need to index
 *
 * @author Stéphane Épardaud
 */
public final class AdditionalJpaModelBuildItem extends MultiBuildItem {

    private final String className;

    public AdditionalJpaModelBuildItem(Class<?> klass) {
        this.className = klass.getName();
    }

    public String getClassName() {
        return className;
    }
}
