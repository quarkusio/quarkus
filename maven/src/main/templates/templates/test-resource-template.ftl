<#if packageName??>
package ${packageName};
</#if>

import org.jboss.shamrock.test.ShamrockTest;
import org.junit.Test;
import org.junit.runner.RunWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@RunWith(ShamrockTest.class)
public class ${classname}Test {

    @Test
    public void testHelloEndpoint() {
        given()
          .when().get("${path}")
          .then()
             .statusCode(200)
             .body(is("hello"));
    }

}
