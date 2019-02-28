package io.quarkus.panache.jpa;

/**
 * <p>
 * Represents a Repository for a specific type of entity {@code Entity}, with an ID type
 * of {@code Long}. Implementing this repository will gain you the exact same useful methods
 * that are on {@link PanacheEntityBase}. If you have a custom ID strategy, you should
 * implement {@link PanacheRepositoryBase} instead.
 * </p>
 *
 * @author Stéphane Épardaud
 * @param <Entity> The type of entity to operate on
 */
public interface PanacheRepository<Entity> extends PanacheRepositoryBase<Entity, Long> {

}
