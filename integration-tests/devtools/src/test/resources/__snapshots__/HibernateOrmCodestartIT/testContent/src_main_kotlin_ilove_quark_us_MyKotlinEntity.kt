package ilove.quark.us

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id

/**
 * Example JPA entity.
 *
 * To use it, get access to a Hibernate ORM Session via injection.
 *
 * {@code
 *    @Inject
 *    lateinit var session: Session;
 *
 *     fun doSomething() {
 *         val entity1 = MyKotlinEntity();
 *         entity1.field = "field-1"
 *         session.persist(entity1);
 *
 *         val entities:List<MyKotlinEntity>  = session.createQuery("from MyEntity", MyKotlinEntity::class.java).getResultList()
 *     }
 * }
 */
@Entity
class MyKotlinEntity {
    @get:GeneratedValue
    @get:Id
    var id: Long? = null
    var field: String? = null
}
