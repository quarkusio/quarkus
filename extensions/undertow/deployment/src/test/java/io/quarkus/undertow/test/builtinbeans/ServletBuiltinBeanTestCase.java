package io.quarkus.undertow.test.builtinbeans;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.response.Response;

public class ServletBuiltinBeanTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(TestServlet.class,
                    ServletBuiltinBeanInjectingBean.class, Counter.class));

    @Test
    public void testHttpServletRequest() {
        String uri = "/foo/request";
        given().param("foo", "bar")
                .when().get(uri)
                .then()
                .statusCode(200)
                .body(is("foo=bar"));
    }

    @Test
    public void testHttpSession() {
        String uri = "/foo/session";

        Response response = when().get(uri);
        response.then()
                .statusCode(200)
                .body(is("foo=bar"))
                .header("counter", "1");
        String sessionId = response.sessionId();

        // another request reusing sessionId
        given().sessionId(sessionId)
                .when().get(uri)
                .then()
                .statusCode(200)
                .body(is("foo=bar"))
                .header("counter", "2");

        // Destroy session
        given().sessionId(sessionId).param("destroy", "true")
                .when().get(uri)
                .then()
                .statusCode(200);

        // New session
        response = when().get(uri);
        response.then()
                .statusCode(200)
                .body(is("foo=bar"))
                .header("counter", "1");
        assertNotEquals(sessionId, response.sessionId());
    }

    @Test
    public void testServletContext() {
        String uri = "/foo/context";
        when().get(uri)
                .then()
                .statusCode(200)
                .body(is("foo=bar"));
    }
}
