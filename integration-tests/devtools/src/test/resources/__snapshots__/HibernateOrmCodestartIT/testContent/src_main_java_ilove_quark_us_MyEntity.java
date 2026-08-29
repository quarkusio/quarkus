package ilove.quark.us;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Example JPA entity.
 *
 * To use it, get access to a Hibernate ORM Session via injection.
 *
 * {@code
 *     @Inject
 *     Session session;
 *
 *     public void doSomething() {
 *         MyEntity entity1 = new MyEntity();
 *         entity1.field = "field-1";
 *         session.persist(entity1);
 *
 *         List<MyEntity> entities = session.createQuery("from MyEntity", MyEntity.class).getResultList();
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
