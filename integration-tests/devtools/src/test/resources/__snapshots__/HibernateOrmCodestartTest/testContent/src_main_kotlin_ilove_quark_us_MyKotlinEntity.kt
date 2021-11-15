package ilove.quark.us

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

/**
 * Example JPA entity.
 *
 * To use it, get access to a JPA EntityManager via injection.
 *
 * {@code
 *    @Inject
 *    lateinit var em:EntityManager;
 *
 *     fun doSomething() {
 *         val entity1 = MyKotlinEntity();
 *         entity1.field = "field-1"
 *         em.persist(entity1);
 *
 *         val entities:List<MyKotlinEntity>  = em.createQuery("from MyEntity", MyKotlinEntity::class.java).getResultList()
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