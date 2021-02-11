package io.quarkus.hibernate.orm.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Model class that JPA should ignore
 *
 * @author Stéphane Épardaud
 */
public final class NonJpaModelBuildItem extends MultiBuildItem {

    private final String className;

    public NonJpaModelBuildItem(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
