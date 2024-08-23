package io.quarkus.hibernate.reactive.rest.data.panache;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.quarkus.rest.data.panache.MethodProperties;
import io.quarkus.rest.data.panache.ReactiveRestDataResource;
import io.quarkus.rest.data.panache.ResourceProperties;

/**
 * REST data Panache resource that uses {@link PanacheEntityBase} instance for data access and exposes it as a JAX-RS resource.
 * <p>
 * See {@link ReactiveRestDataResource} for the methods provided by this resource.
 * <p>
 * See {@link ResourceProperties} and {@link MethodProperties} for the ways to customize this resource.
 *
 * @param <Entity> {@link PanacheEntityBase} that is handled by this resource.
 * @param <ID> ID type of the entity.
 */
public interface PanacheEntityResource<Entity extends PanacheEntityBase, ID> extends ReactiveRestDataResource<Entity, ID> {

}
