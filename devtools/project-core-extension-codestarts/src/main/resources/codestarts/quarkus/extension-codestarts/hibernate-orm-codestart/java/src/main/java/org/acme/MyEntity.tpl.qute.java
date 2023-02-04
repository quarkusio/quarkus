package org.acme;

{#if !input.selected-extensions-ga.contains('io.quarkus:quarkus-hibernate-orm-panache')}
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Example JPA entity.
 *
 * To use it, get access to a JPA EntityManager via injection.
 *
 * \{@code
 *     @Inject
 *     EntityManager em;
 *
 *     public void doSomething() {
 *         MyEntity entity1 = new MyEntity();
 *         entity1.field = "field-1";
 *         em.persist(entity1);
 *
 *         List<MyEntity> entities = em.createQuery("from MyEntity", MyEntity.class).getResultList();
 *     }
 * }
 */
@Entity
public class MyEntity {
    @Id
    @GeneratedValue
    public Long id;

    public String field;
}
{#else}
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;


/**
 * Example JPA entity defined as a Panache Entity.
 * An ID field of Long type is provided, if you want to define your own ID field extends <code>PanacheEntityBase</code> instead.
 *
 * This uses the active record pattern, you can also use the repository pattern instead:
 * {@see https://quarkus.io/guides/hibernate-orm-panache#solution-2-using-the-repository-pattern}.
 *
 * Usage (more example on the documentation)
 *
 * \{@code
 *     public void doSomething() {
 *         MyEntity entity1 = new MyEntity();
 *         entity1.field = "field-1";
 *         entity1.persist();
 *
 *         List<MyEntity> entities = MyEntity.listAll();
 *     }
 * }
 */
@Entity
public class MyEntity extends PanacheEntity {
    public String field;
}
{/if}
