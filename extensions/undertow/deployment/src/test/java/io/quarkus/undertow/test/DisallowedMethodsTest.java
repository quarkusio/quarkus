package io.quarkus.undertow.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.http.Method;

public class DisallowedMethodsTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClass(DisallowedMethodsTestServlet.class))
            .withConfigurationResource("application-disallowed-methods.properties");

    @TestHTTPResource("/disallowed")
    URI testUri;

    @Test
    public void traceMethodShouldBeBlockedWith405() {
        given()
                .when()
                .request(Method.TRACE, testUri)
                .then()
                .statusCode(405);
    }

    @Test
    public void getMethodShouldStillWork() {
        given()
                .when()
                .get(testUri)
                .then()
                .statusCode(200)
                .body(equalTo("ok"));
    }
}
