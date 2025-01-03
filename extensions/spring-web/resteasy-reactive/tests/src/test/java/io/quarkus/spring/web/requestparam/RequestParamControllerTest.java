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
    public void whenInvokeGetQueryStringThenTheOriginQueryStringIsReturned() throws Exception {
        when().get("/api/foos?id=abc")
                .then()
                .statusCode(200)
                .body(is("ID: abc"));

        // should return 400 because, in Spring, method parameters annotated with @RequestParam are required by default.
        // see SpringWebResteasyReactiveProcessor L298
        when().get("/api/foos")
                .then()
                .statusCode(200);
    }

    @Test
    public void whenConfigureParamToBeOptionalThenTheGetQueryWorksWithAndWithoutRequestParam() throws Exception {
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
    public void whenWrapingParamInOptionalThenTheGetQueryWorksWithAndWithoutRequestParam() throws Exception {
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
    public void whenDefaultValueProvidedThenItIsReturnedIfRequestParamIsAbsent() throws Exception {
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
    public void whenInvokePostQueryWithSpecificParamNameThenTheOriginQueryStringIsReturned() throws Exception {
        when().post("/api/foos/map?id=abc&name=bar")
                .then()
                .statusCode(200)
                .body(containsString("Parameters are [name=bar, id=abc]"));

    }

    @Test
    public void testMultivalue() throws Exception {
        when().get("/api/foos/multivalue?id=1,2,3")
                .then()
                .statusCode(200)
                .body(containsString("IDs are [1,2,3]"));

        when().get("/api/foos/multivalue?id=1&id=2")
                .then()
                .statusCode(200).log().body()
                .body(containsString("IDs are [1, 2]"));
    }

}
