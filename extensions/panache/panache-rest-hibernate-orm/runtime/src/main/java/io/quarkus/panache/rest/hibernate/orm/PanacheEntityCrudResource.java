package io.quarkus.panache.rest.hibernate.orm;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.rest.common.PanacheCrudResource;

/**
 * Panache CRUD resource that uses {@link PanacheEntityBase} instance for data access and exposes it as a JAX-RS resource.
 * <p>
 * See {@link PanacheCrudResource} for the methods provided by this resource.
 * <p>
 * See {@link io.quarkus.panache.rest.common.PanacheRestResource} for the ways to customize this resource.
 *
 * @param <Entity> {@link PanacheEntityBase} that is handled by this resource.
 * @param <ID> ID type of the entity.
 */
public interface PanacheEntityCrudResource<Entity extends PanacheEntityBase, ID> extends PanacheCrudResource<Entity, ID> {

}
