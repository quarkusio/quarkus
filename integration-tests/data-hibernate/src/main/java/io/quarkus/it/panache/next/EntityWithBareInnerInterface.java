package io.quarkus.it.panache.next;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.panache.PanacheEntity;

/**
 * Regression test entity for hibernate/hibernate-orm#13090.
 * <p>
 * When a PanacheEntity has an inner interface that does NOT extend
 * PanacheRepository and has no {@code @Find}/{@code @HQL} methods,
 * the Hibernate Processor must still generate a correct import for
 * the inner interface type in the CDI accessor of the metamodel class.
 * <p>
 * Without the fix, the generated {@code EntityWithBareInnerInterface_}
 * uses the unqualified name {@code Queries} without an import statement,
 * causing a compilation failure.
 */
@Entity
public class EntityWithBareInnerInterface extends PanacheEntity {
    public String name;

    public interface Queries extends CustomQueries {
    }
}
