package io.quarkus.hibernate.orm.deployment.spi;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.component.ComponentLookup;

/**
 * Declares that an extension can handle external clients for Hibernate ORM,
 * and provides a {@link ComponentLookup} to check for client availability.
 * <p>
 * Multiple extensions may produce this build item. A client is deemed available
 * if <em>any</em> handler reports it as available.
 * <p>
 * Should not be consumed except by the Hibernate ORM extension;
 * other extensions should consume {@link HibernateOrmClientLookupBuildItem}.
 *
 * @see HibernateOrmClientLookupBuildItem
 */
public final class HibernateOrmClientLookupHandlerBuildItem extends MultiBuildItem {

    private final ComponentLookup lookup;

    public HibernateOrmClientLookupHandlerBuildItem(ComponentLookup lookup) {
        this.lookup = lookup;
    }

    public ComponentLookup getLookup() {
        return lookup;
    }
}
