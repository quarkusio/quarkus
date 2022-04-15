package io.quarkus.hibernate.reactive.rest.data.panache;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.rest.data.panache.MethodProperties;
import io.quarkus.rest.data.panache.ResourceProperties;
import io.quarkus.rest.data.panache.RestDataResource;

/**
 * REST data Panache resource that uses {@link PanacheRepositoryBase} instance for data access and exposes it as a JAX-RS
 * resource.
 * <p>
 * See {@link RestDataResource} for the methods provided by this resource.
 * <p>
 * See {@link ResourceProperties} and {@link MethodProperties} for the ways to customize this resource.
 *
 * @param <Repository> {@link PanacheRepositoryBase} instance that should be used for data access.
 * @param <Entity> Entity type that is handled by this resource and the linked {@link PanacheRepositoryBase} instance.
 * @param <ID> ID type of the entity.
 */
public interface PanacheRepositoryResource<Repository extends PanacheRepositoryBase<Entity, ID>, Entity, ID>
        extends RestDataResource<Entity, ID> {

}
