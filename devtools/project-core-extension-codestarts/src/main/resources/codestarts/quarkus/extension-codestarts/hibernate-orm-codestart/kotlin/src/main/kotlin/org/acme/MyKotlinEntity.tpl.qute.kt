package org.acme

{#if !input.selected-extensions-ga.contains('io.quarkus:quarkus-hibernate-orm-panache-kotlin')}
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id

/**
 * Example JPA entity.
 *
 * To use it, get access to a JPA EntityManager via injection.
 *
 * \{@code
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
{#else}
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanion
import jakarta.persistence.Entity

/**
 * Example JPA entity defined as a Kotlin Panache Entity.
 * An ID field of Long type is provided, if you want to define your own ID field extends <code>PanacheEntityBase</code> instead.
 *
 * This uses the active record pattern, you can also use the repository pattern instead:
 * {@see https://quarkus.io/guides/hibernate-orm-panache-kotlin#defining-your-repository}.
 *
 * Usage (more example on the documentation)
 *
 * \{@code
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
{/if}