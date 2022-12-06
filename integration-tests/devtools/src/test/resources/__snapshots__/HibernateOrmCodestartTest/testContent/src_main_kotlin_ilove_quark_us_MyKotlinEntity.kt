package ilove.quark.us

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id

/**
 * Example JPA entity.
 *
 * To use it, get access to a JPA EntityManager via injection.
 *
 * ```kotlin
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
 * ```
 */
@Entity
class MyKotlinEntity {
    @get:GeneratedValue
    @get:Id
    var id: Long? = null
    var field: String? = null
}