package io.quarkus.hibernate.orm.deployment.spi;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.component.ComponentLookup;

/**
 * Provides a {@link ComponentLookup lookup} to determine
 * if a client can reasonably be {@link HibernateOrmClientRequestBuildItem requested}.
 * <p>
 * Assembled by the Hibernate ORM extension from {@link HibernateOrmClientLookupHandlerBuildItem}
 * contributions provided by client extensions (e.g. quarkus-mongodb-hibernate).
 * A client is deemed available if <em>any</em> handler reports it as available.
 * <p>
 * Extensions can consume this build item early on (before {@link HibernateOrmClientRequestBuildItem} is processed)
 * if they intend to use a certain client only if there is a chance for it to exist.
 *
 * @see HibernateOrmClientLookupHandlerBuildItem
 * @see HibernateOrmClientRequestBuildItem
 * @see HibernateOrmClientDefinedBuildItem
 */
public final class HibernateOrmClientLookupBuildItem extends SimpleBuildItem {

    private final ComponentLookup lookup;

    public HibernateOrmClientLookupBuildItem(ComponentLookup lookup) {
        this.lookup = lookup;
    }

    public ComponentLookup getLookup() {
        return lookup;
    }
}
