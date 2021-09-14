package io.quarkus.hibernate.orm.panache.kotlin.deployment.test

import io.quarkus.builder.BuildException
import io.quarkus.test.QuarkusUnitTest
import org.jboss.shrinkwrap.api.spec.JavaArchive
import org.jboss.shrinkwrap.api.ShrinkWrap
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class DuplicateIdEntityTest {
    @Test
    fun shouldThrow() {
        Assertions.fail<Any>("A BuildException should have been thrown due to duplicate entity ID")
    }

    companion object {
        @RegisterExtension
        @JvmField
        var runner = QuarkusUnitTest()
                .setExpectedException(BuildException::class.java)
                .setArchiveProducer {
                    ShrinkWrap.create(JavaArchive::class.java)
                            .addClasses(DuplicateIdEntity::class.java)
                }
    }
}
