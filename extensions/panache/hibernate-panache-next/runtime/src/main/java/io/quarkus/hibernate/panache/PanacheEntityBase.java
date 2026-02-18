package io.quarkus.hibernate.panache;

import io.quarkus.hibernate.panache.managed.blocking.PanacheManagedBlockingEntity;

/**
 * This is just an alias for {@link PanacheManagedBlockingEntity} for people coming from Panache 1
 */
public class PanacheEntityBase<Entity extends PanacheEntityMarker<Entity>> implements PanacheManagedBlockingEntity<Entity> {
}
