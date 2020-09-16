package {package-name}

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.`is`
import org.junit.jupiter.api.Test

@QuarkusTest
class {resource.class-name}Test {

    @Test
    def testHelloEndpoint() = {
        given()
          .`when`().get("{resource.path}")
          .then()
             .statusCode(200)
             .body(`is`("{resource.response}"))
    }

}