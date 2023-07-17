package io.quarkus.hibernate.orm.panache.kotlin.deployment.test.config

import io.quarkus.arc.Arc
import io.quarkus.hibernate.orm.panache.kotlin.deployment.test.MyEntity
import io.quarkus.test.QuarkusUnitTest
import org.jboss.shrinkwrap.api.spec.JavaArchive
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import jakarta.persistence.EntityManagerFactory

class ConfigEnabledFalseTest {
    companion object {
        @RegisterExtension
        val config = QuarkusUnitTest()
            .withApplicationRoot { jar: JavaArchive -> jar.addClass(MyEntity::class.java) }
            .withConfigurationResource("application-test.properties")
            // We shouldn't get any build error caused by Panache consuming build items that are not produced
            // See https://github.com/quarkusio/quarkus/issues/28842
            .overrideConfigKey("quarkus.hibernate-orm.enabled", "false")
    }

    @Test
    fun startsWithoutError() {
        // Quarkus started without problem, even though the Panache extension is present.
        // Just check that Hibernate ORM is disabled.
        Assertions.assertNull(Arc.container().instance(EntityManagerFactory::class.java).get())
    }
}