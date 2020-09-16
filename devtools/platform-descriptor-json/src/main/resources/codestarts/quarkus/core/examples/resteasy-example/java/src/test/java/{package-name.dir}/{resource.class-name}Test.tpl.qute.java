package {package-name};

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class {resource.class-name}Test {

    @Test
    public void testHelloEndpoint() {
        given()
          .when().get("{resource.path}")
          .then()
             .statusCode(200)
             .body(is("{resource.response}"));
    }

}