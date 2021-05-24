package io.quarkus.hibernate.orm.panache.kotlin.deployment.test.multiple_pu

import io.quarkus.hibernate.orm.panache.kotlin.deployment.test.multiple_pu.first.FirstEntity
import io.quarkus.hibernate.orm.panache.kotlin.deployment.test.multiple_pu.second.SecondEntity
import io.quarkus.test.QuarkusUnitTest
import org.jboss.shrinkwrap.api.ShrinkWrap
import org.jboss.shrinkwrap.api.spec.JavaArchive
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ErroneousPersistenceUnitConfigTest {
    @Test
    fun shouldNotReachHere() {
        Assertions.fail<Any>()
    }

    companion object {
        @RegisterExtension
        @JvmField
        var runner = QuarkusUnitTest()
                .setExpectedException(IllegalStateException::class.java)
                .setArchiveProducer {
                    ShrinkWrap.create(JavaArchive::class.java)
                            .addClasses(FirstEntity::class.java, SecondEntity::class.java, PanacheTestResource::class.java)
                            .addAsResource("application-erroneous-multiple-persistence-units.properties", "application.properties")
                }
    }
}
