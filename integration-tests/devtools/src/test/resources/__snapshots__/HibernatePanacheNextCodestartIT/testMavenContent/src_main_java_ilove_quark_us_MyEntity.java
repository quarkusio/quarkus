package ilove.quark.us;

import io.quarkus.hibernate.panache.PanacheEntity;
import jakarta.persistence.Entity;


/**
 * Example JPA entity defined as a Panache Entity.
 * An ID field of Long type is provided, if you want to define your own ID field extends <code>WithId</code> instead.
 *
 * Documentation: {@see https://quarkus.io/guides/hibernate-panache-next}
 *
 * Usage:
 *
 * {@code
 *     public void doSomething() {
 *         MyEntity entity1 = new MyEntity();
 *         entity1.field = "field-1";
 *         entity1.persist();
 *
 *         List<MyEntity> entities = MyEntity_.managedBlocking().listAll();
 *     }
 * }
 */
@Entity
public class MyEntity extends PanacheEntity {
    public String field;
}
