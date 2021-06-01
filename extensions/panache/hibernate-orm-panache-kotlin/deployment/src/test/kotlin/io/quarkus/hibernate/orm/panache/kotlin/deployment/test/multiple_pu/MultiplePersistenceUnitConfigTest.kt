package io.quarkus.hibernate.orm.panache.kotlin.deployment.test.multiple_pu

import io.quarkus.hibernate.orm.panache.kotlin.deployment.test.multiple_pu.first.FirstEntity
import io.quarkus.hibernate.orm.panache.kotlin.deployment.test.multiple_pu.second.SecondEntity
import io.quarkus.test.QuarkusUnitTest
import io.restassured.RestAssured
import org.hamcrest.Matchers
import org.jboss.shrinkwrap.api.ShrinkWrap
import org.jboss.shrinkwrap.api.spec.JavaArchive
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class MultiplePersistenceUnitConfigTest {
    @Test
    @Disabled("fix in a separate PR")
    fun panacheOperations() {
        /**
         * First entity operations
         */
        RestAssured.`when`()["/persistence-unit/first/name-1"].then().body(Matchers.`is`("name-1"))
        RestAssured.`when`()["/persistence-unit/first/name-2"].then().body(Matchers.`is`("name-2"))
        /**
         * second entity operations
         */
        RestAssured.`when`()["/persistence-unit/second/name-1"].then().body(Matchers.`is`("name-1"))
        RestAssured.`when`()["/persistence-unit/second/name-2"].then().body(Matchers.`is`("name-2"))
    }

    companion object {
        @RegisterExtension
        @JvmField
        var runner = QuarkusUnitTest()
                .setArchiveProducer {
                    ShrinkWrap.create(JavaArchive::class.java)
                            .addClasses(FirstEntity::class.java, SecondEntity::class.java, PanacheTestResource::class.java)
                            .addAsResource("application-multiple-persistence-units.properties", "application.properties")
                }
    }
}
