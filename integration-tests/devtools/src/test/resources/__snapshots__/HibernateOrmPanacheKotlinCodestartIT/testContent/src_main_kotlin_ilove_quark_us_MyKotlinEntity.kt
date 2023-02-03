package ilove.quark.us

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanion
import jakarta.persistence.Entity

/**
 * Example JPA entity defined as a Kotlin Panache Entity.
 * An ID field of Long type is provided, if you want to define your own ID field extends <code>PanacheEntityBase</code> instead.
 *
 * This uses the active record pattern, you can also use the repository pattern instead:
 * .
 *
 * Usage (more example on the documentation)
 *
 * {@code
 *
 *      fun doSomething() {
 *          val entity1 = MyKotlinEntity();
 *          entity1.field = "field-1"
 *          entity1.persist()
 *
 *         val entities:List<MyKotlinEntity>  = MyKotlinEntity.listAll()
 *     }
 * }
 */
@Entity
class MyKotlinEntity: PanacheEntity() {
    companion object: PanacheCompanion<MyKotlinEntity> {
        fun byName(name: String) = list("name", name)
    }

    lateinit var field: String
}
