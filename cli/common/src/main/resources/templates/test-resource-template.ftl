<#if package_name??>
package ${package_name};
</#if>

import org.jboss.shamrock.test.junit.ShamrockTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@ExtendWith(ShamrockTest.class)
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
