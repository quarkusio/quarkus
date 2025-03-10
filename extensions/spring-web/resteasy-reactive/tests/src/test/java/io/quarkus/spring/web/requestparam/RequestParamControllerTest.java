package io.quarkus.spring.web.requestparam;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class RequestParamControllerTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(RequestParamController.class));

    @Test
    public void testSimpleMapping() throws Exception {
        when().get("/api/foos?id=abc")
                .then()
                .statusCode(200)
                .body(is("ID: abc"));

        // In Spring, method parameters annotated with @RequestParam are required by default.
        when().get("/api/foos")
                .then()
                .statusCode(400);
    }

    @Test
    public void testSimpleMappingSpecifyingName() throws Exception {
        when().post("/api/foos?id=abc&name=bar")
                .then()
                .statusCode(200)
                .body(is("ID: abc Name: bar"));

        when().post("/api/foos")
                .then()
                .statusCode(400);

    }

    @Test
    public void testNotRequiredParam() throws Exception {
        when().get("/api/foos/notParamRequired?id=abc")
                .then()
                .statusCode(200)
                .body(is("ID: abc"));

        when().get("/api/foos/notParamRequired")
                .then()
                .statusCode(200)
                .body(is("ID: null"));
    }

    @Test
    public void testOptionalParam() throws Exception {
        when().get("/api/foos/optional?id=abc")
                .then()
                .statusCode(200)
                .body(is("ID: abc"));

        when().get("/api/foos/optional")
                .then()
                .statusCode(200)
                .body(is("ID: not provided"));
    }

    @Test
    public void testDefaultValueForParam() throws Exception {
        when().get("/api/foos/defaultValue?id=abc")
                .then()
                .statusCode(200)
                .body(is("ID: abc"));

        when().get("/api/foos/defaultValue")
                .then()
                .statusCode(200)
                .body(is("ID: test"));
    }

    @Test
    public void testMultipleMapping() throws Exception {
        when().post("/api/foos/map?id=abc&name=bar")
                .then()
                .statusCode(200)
                .body(containsString("Parameters are [name=[bar], id=[abc]]"));

        when().post("/api/foos/map")
                .then()
                .statusCode(400);

    }

    @Test
    public void testMultivalue() throws Exception {
        when().get("/api/foos/multivalue?id=1,2,3")
                .then()
                .statusCode(200)
                .body(containsString("IDs are [1, 2, 3]"));

        when().get("/api/foos/multivalue?id=1,2,3&id=foo")
                .then()
                .statusCode(200)
                .body(containsString("IDs are [1, 2, 3, foo]"));

        when().get("/api/foos/multivalue")
                .then()
                .statusCode(400);
    }

    @Test
    public void testMultiMap() throws Exception {
        when().post("/api/foos/multiMap?id=abc&id=123")
                .then()
                .statusCode(200)
                .body(containsString("Parameters are id=abc, 123"));

        when().post("/api/foos/multiMap")
                .then()
                .statusCode(400);
    }

}
