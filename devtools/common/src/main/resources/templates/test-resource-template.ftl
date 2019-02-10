package ${package_name};

import org.jboss.shamrock.test.junit.ShamrockTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@ShamrockTest
public class ${class_name}Test {

    @Test
    public void testHelloEndpoint() {
        given()
          .when().get("${path}")
          .then()
             .statusCode(200)
             .body(is("hello"));
    }

}
