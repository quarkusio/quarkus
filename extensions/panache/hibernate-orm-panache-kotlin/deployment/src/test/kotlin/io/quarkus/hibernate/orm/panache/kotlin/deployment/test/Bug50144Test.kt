package io.quarkus.hibernate.orm.panache.kotlin.deployment.test

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.test.QuarkusUnitTest
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.transaction.Transactional
import org.jboss.shrinkwrap.api.ShrinkWrap
import org.jboss.shrinkwrap.api.spec.JavaArchive
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID

class Bug50144Test {
    companion object {
        @RegisterExtension
        @JvmField
        var runner = QuarkusUnitTest()
            .setArchiveProducer {
                ShrinkWrap.create(JavaArchive::class.java)
                    .addClasses(WorkingAssignmentRepository::class.java, BrokenAssignmentRepository::class.java,
                        WorkingBaseRepository::class.java, BrokenBaseRepository::class.java,
                        Assignment::class.java, NotFoundException::class.java)
            }
    }

    @Inject
    private lateinit var workingAssignmentRepository: WorkingAssignmentRepository

    @Inject
    private lateinit var brokenAssignmentRepository: BrokenAssignmentRepository

    @Transactional
    @Test
    fun `A working example`() {
        val id: UUID = UUID.randomUUID()

        assertThrows<NotFoundException> {
            workingAssignmentRepository.findByIdOrThrow(id)
        }
    }

    @Transactional
    @Test
    fun `This breaks for some reason`() {
        val id: UUID = UUID.randomUUID()

        assertThrows<NotFoundException> {
            brokenAssignmentRepository.findByIdOrThrow(id)
        }
    }

    class NotFoundException: Exception()

    @Entity
    class Assignment {
        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        private lateinit var _id: UUID
    }

    abstract class WorkingBaseRepository<EntityT : Any, IdT: Any> : PanacheRepositoryBase<EntityT, IdT> {
        fun findByIdOrThrow(
            id: IdT,
            exception: (IdT) -> Exception,
        ): EntityT = findById(id) ?: throw exception(id)
    }

    @ApplicationScoped
    class WorkingAssignmentRepository : WorkingBaseRepository<Assignment, UUID>() {
        fun findByIdOrThrow(assignmentId: UUID): Assignment {
            return findByIdOrThrow(assignmentId) {
                NotFoundException()
            }
        }
    }

    abstract class BrokenBaseRepository<EntityT : Any, IdT: UUID> : PanacheRepositoryBase<EntityT, IdT> {
        fun findByIdOrThrow(
            id: IdT,
            exception: (IdT) -> Exception,
        ): EntityT = findById(id) ?: throw exception(id)
    }

    @ApplicationScoped
    class BrokenAssignmentRepository : BrokenBaseRepository<Assignment, UUID>() {
        fun findByIdOrThrow(assignmentId: UUID): Assignment {
            return findByIdOrThrow(assignmentId) {
                NotFoundException()
            }
        }
    }


}
