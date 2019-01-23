<#if package_name??>
package ${package_name};
</#if>

import org.jboss.shamrock.test.junit.ShamrockTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

<<<<<<< HEAD:maven/src/main/templates/templates/test-resource-template.ftl
@ExtendWith(ShamrockTest.class)
public class ${class_name}Test {
=======
@RunWith(ShamrockTest.class)
public class ${className}Test {
>>>>>>> merge cli branch to master:cli/common/src/main/resources/templates/test-resource-template.ftl

    @Test
    public void testHelloEndpoint() {
        given()
<<<<<<< HEAD:maven/src/main/templates/templates/test-resource-template.ftl
          .when().get("${path}")
=======
          .when().get("${docRoot}${path}")
>>>>>>> merge cli branch to master:cli/common/src/main/resources/templates/test-resource-template.ftl
          .then()
             .statusCode(200)
             .body(is("hello"));
    }

}
