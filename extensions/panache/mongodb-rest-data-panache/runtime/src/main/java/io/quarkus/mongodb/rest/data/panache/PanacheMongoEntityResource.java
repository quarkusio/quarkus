package io.quarkus.mongodb.rest.data.panache;

import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import io.quarkus.rest.data.panache.MethodProperties;
import io.quarkus.rest.data.panache.ResourceProperties;
import io.quarkus.rest.data.panache.RestDataResource;

/**
 * REST data Panache resource that uses {@link PanacheMongoEntityBase} instance for data access and exposes it as a JAX-RS
 * resource.
 * <p>
 * See {@link RestDataResource} for the methods provided by this resource.
 * <p>
 * See {@link ResourceProperties} and {@link MethodProperties} for the ways to customize this resource.
 *
 * @param <Entity> {@link PanacheMongoEntityBase} that is handled by this resource.
 * @param <ID> ID type of the entity.
 */
public interface PanacheMongoEntityResource<Entity extends PanacheMongoEntityBase, ID> extends RestDataResource<Entity, ID> {

}
