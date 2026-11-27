package io.quarkus.hibernate.orm.panache.kotlin.deployment.test

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.test.QuarkusExtensionTest
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.transaction.Transactional
import java.util.UUID
import org.jboss.shrinkwrap.api.ShrinkWrap
import org.jboss.shrinkwrap.api.spec.JavaArchive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ValueClassIdTest {
    companion object {
        @RegisterExtension
        @JvmField
        var runner = QuarkusExtensionTest()
            .setArchiveProducer {
                ShrinkWrap.create(JavaArchive::class.java)
                    .addClasses(ValueClassIdEntity::class.java, ValueClassIdRepository::class.java, EntityId::class.java)
            }
    }

    @Inject
    private lateinit var repository: ValueClassIdRepository

    @Transactional
    @Test
    fun `findById accepts a value class id`() {
        val entity = ValueClassIdEntity()
        entity.name = "found by value class id"
        repository.persist(entity)

        val found = repository.findById(entity.id)
        assertNotNull(found)
        assertEquals("found by value class id", found!!.name)
        assertEquals(entity.id, found.id)

        assertNull(repository.findById(EntityId(UUID.randomUUID())))
    }

    @Transactional
    @Test
    fun `companion findById and deleteById accept a value class id`() {
        val entity = ValueClassIdEntity()
        entity.name = "found by the companion"
        entity.persist()

        val found = ValueClassIdEntity.findById(entity.id)
        assertNotNull(found)
        assertEquals("found by the companion", found!!.name)
        assertNull(ValueClassIdEntity.findById(EntityId(UUID.randomUUID())))

        assertTrue(ValueClassIdEntity.deleteById(entity.id))
        assertNull(ValueClassIdEntity.findById(entity.id))
    }

    @Transactional
    @Test
    fun `deleteById accepts a value class id`() {
        val entity = ValueClassIdEntity()
        entity.name = "deleted by value class id"
        repository.persist(entity)

        assertTrue(repository.deleteById(entity.id))
        assertFalse(repository.deleteById(entity.id))
        assertNull(repository.findById(entity.id))
    }

    @JvmInline
    value class EntityId(val value: UUID)

    @Entity
    class ValueClassIdEntity : PanacheEntityBase {
        companion object : PanacheCompanionBase<ValueClassIdEntity, EntityId>

        @Id
        var id: EntityId = EntityId(UUID.randomUUID())

        var name: String? = null
    }

    @ApplicationScoped
    class ValueClassIdRepository : PanacheRepositoryBase<ValueClassIdEntity, EntityId>
}
