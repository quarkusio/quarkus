package ${package};

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
public class GreetingTest
{
    @Test
    public void testJaxrs() {
        RestAssured.when().get("/hello").then()
                .contentType("text/plain")
                .body(equalTo("hello jaxrs"));
    }

    @Test
    public void testServlet() {
        RestAssured.when().get("/servlet/hello").then()
                .contentType("text/plain")
                .body(equalTo("hello servlet"));
    }

    @Test
    public void testVertx() {
        RestAssured.when().get("/vertx/hello").then()
                .contentType("text/plain")
                .body(equalTo("hello vertx"));
    }

}
