package io.quarkus.hibernate.orm.deployment.spi.client;

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
 * the ORM extension aggregates these into an internal lookup build item.
 */
public final class HibernateOrmClientHandlerBuildItem extends MultiBuildItem {

    private final ComponentLookup lookup;

    public HibernateOrmClientHandlerBuildItem(ComponentLookup lookup) {
        this.lookup = lookup;
    }

    public ComponentLookup getLookup() {
        return lookup;
    }
}
