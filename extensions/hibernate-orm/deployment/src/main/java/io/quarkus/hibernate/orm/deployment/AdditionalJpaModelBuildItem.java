package io.quarkus.hibernate.orm.deployment;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Additional Jpa model class that we need to index
 *
 * @author Stéphane Épardaud
 *
 * @deprecated Use {@link io.quarkus.hibernate.orm.deployment.spi.AdditionalJpaModelBuildItem} instead. Kept only
 *             temporarily for backwards compatibility with external extensions e.g.
 *             <a href="https://github.com/quarkiverse/quarkus-jberet">quarkus-jberet</a> See
 *             <a href="https://github.com/search?q=org%3Aquarkiverse%20AdditionalJpaModelBuildItem&type=code">here</a>
 *             for a full list.
 */
@Deprecated
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
