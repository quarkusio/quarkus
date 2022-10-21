package org.acme

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.`is`
import org.junit.jupiter.api.Test

@QuarkusTest
class AdminResourceTest {

    @Test
    fun testHelloEndpoint() {
        given()
          .`when`()
          .contentType("application/json")
          .body("{}")
          .post("/admin/api/v1.0/tenants/1/accounts")
          .then()
          .log().ifValidationFails()
          .statusCode(204)
    }

}
