package {package-name}.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class {class-name-base}ResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/{extension.id}")
                .then()
                .statusCode(200)
                .body(is("Hello {extension.id}"));
    }
}
