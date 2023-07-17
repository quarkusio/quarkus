// tag::test[]
package org.acme.getting.started.testing;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class MyAcmeApplicationTest {

    @Test
    void testHelloEndpoint() {
        when().get("/hello").then().statusCode(200)
                .body(is("Hello, World!"));
    }

    // tag::goodbye[]
    @Test
    void testGoodbyeEndpoint() {
        when().get("/longGoodbye").then().statusCode(200)
                .body(containsString(
                        "data: Goodbye\nid: 0\n\n"
                                + "data: ,\nid: 1\n\n"))
                                + "data: Sweet\nid: 2\n\n"
                                + "data: World\nid: 3\n\n"
                                + "data: !\nid: 4\n\n"))
                .header("content-type", "text/event-stream");
    }
    // end::goodbye[]
}
// end::test[]