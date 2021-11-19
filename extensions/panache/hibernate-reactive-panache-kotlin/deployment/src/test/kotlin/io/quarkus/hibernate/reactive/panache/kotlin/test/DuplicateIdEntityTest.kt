package io.quarkus.hibernate.reactive.panache.kotlin.test

import io.quarkus.builder.BuildException
import io.quarkus.test.QuarkusUnitTest
import org.jboss.shrinkwrap.api.spec.JavaArchive
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
            .overrideConfigKey("quarkus.datasource.devservices", "false")
            .setExpectedException(BuildException::class.java)
            .withApplicationRoot { jar: JavaArchive ->
                jar
                    .addClasses(DuplicateIdEntity::class.java)
            }
    }
}