package io.quarkus.it.panache

import io.quarkus.test.junit.QuarkusIntegrationTest
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test

// Native tests
@QuarkusIntegrationTest class ProjectionIT : ProjectionTest()

// Quarkus/JVM tests
@QuarkusTest
open class ProjectionTest {

    @Test
    fun testProject() {
        RestAssured.`when`()["/test/project"].then().body(Matchers.`is`("OK"))
        RestAssured.`when`()["/test/projection-constructor-annotation"].then()
            .body(Matchers.`is`("OK"))
        RestAssured.`when`()["/test/projection-projected-field-name"].then()
            .body(Matchers.`is`("OK"))
        RestAssured.`when`()["/test/projection-no-arguments-constructor"].then()
            .body(Matchers.`is`("OK"))
    }
}
