package io.quarkus.panache.rest.hibernate.orm;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.rest.common.PanacheCrudResource;

/**
 * Panache CRUD resource that uses {@link PanacheRepositoryBase} instance for data access and exposes it as a JAX-RS resource.
 * <p>
 * See {@link PanacheCrudResource} for the methods provided by this resource.
 * <p>
 * See {@link io.quarkus.panache.rest.common.PanacheRestResource} for the ways to customize this resource.
 *
 * @param <Repository> {@link PanacheRepositoryBase} instance that should be used for data access.
 * @param <Entity> Entity type that is handled by this resource and the linked {@link PanacheRepositoryBase} instance.
 * @param <ID> ID type of the entity.
 */
public interface PanacheRepositoryCrudResource<Repository extends PanacheRepositoryBase<Entity, ID>, Entity, ID>
        extends PanacheCrudResource<Entity, ID> {

}
