package io.quarkus.hibernate.orm.panache.kotlin.deployment.test

import io.quarkus.test.QuarkusUnitTest
import org.jboss.shrinkwrap.api.ShrinkWrap
import org.jboss.shrinkwrap.api.spec.JavaArchive
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class NoConfigTest {
    @Test
    fun testNoConfig() {
        // we should be able to start the application, even with no configuration at all
    }

    companion object {
        @RegisterExtension
        @JvmField
        val config = QuarkusUnitTest().setArchiveProducer { ShrinkWrap.create(JavaArchive::class.java) }
    }
}
