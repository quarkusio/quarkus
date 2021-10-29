package ilove.quark.us

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.`is`
import org.junit.jupiter.api.Test

@QuarkusTest
class MyDeclarativeRoutesTest {
    @Test
    fun testHelloRouteEndpointWithNameParameter() {
        given()
                .`when`().get("/hello-route?name=Quarkus")
                .then()
                .statusCode(200)
                .body(`is`("Hello Quarkus !!"))
    }

    @Test
    fun testHelloRouteEndpointWithoutNameParameter() {
        given()
                .`when`().get("/hello-route")
                .then()
                .statusCode(200)
                .body(`is`("Hello Reactive Route !!"))
    }
}