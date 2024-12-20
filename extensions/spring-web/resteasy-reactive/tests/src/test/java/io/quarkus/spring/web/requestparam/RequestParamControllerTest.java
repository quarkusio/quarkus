package io.quarkus.spring.web.requestparam;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class RequestParamControllerTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(RequestParamController.class));

    @Disabled
    @Test
    public void whenInvokeGetQueryStringThenTheOriginQueryStringIsReturned() throws Exception {
        when().get("/api/foos?id=abc")
                .then()
                .statusCode(200)
                .body(is("ID: abc"));

        // should return 400 because, in Spring, method parameters annotated with @RequestParam are required by default.
        //        when().get("/api/foos")
        //                .then()
        //                .statusCode(400);
    }

    @Disabled
    @Test
    public void whenConfigureParamToBeOptionalThenTheGetQueryWorksWithAndWithoutRequestParam() throws Exception {
        when().get("/api/foosNotParamRequired?id=abc")
                .then()
                .statusCode(200)
                .body(is("ID: abc"));

        // should return 400 because, in Spring, method parameters annotated with @RequestParam are required by default.
        when().get("/api/foosNotParamRequired")
                .then()
                .statusCode(200)
                .body(is("ID: null"));
    }

    @Disabled
    @Test
    public void whenWrapingParamInOptionalThenTheGetQueryWorksWithAndWithoutRequestParam() throws Exception {
        when().get("/api/foosOptional?id=abc")
                .then()
                .statusCode(200)
                .body(is("ID: abc"));

        // should return 400 because, in Spring, method parameters annotated with @RequestParam are required by default.
        when().get("/api/foosOptional")
                .then()
                .statusCode(200)
                .body(is("ID: not provided"));
    }

    @Test
    public void whenDefaultValueProvidedThenItIsReturnedIfRequestParamIsAbsent() throws Exception {
        when().get("/api/foosDefaultValue?id=abc")
                .then()
                .statusCode(200)
                .body(is("ID: abc"));

        // should return 400 because, in Spring, method parameters annotated with @RequestParam are required by default.
        //        when().get("/api/foosDefaultValue")
        //                .then()
        //                .statusCode(200)
        //                .body(is("ID: test"));
    }

    //    @Test
    //    public void whensfasquestParamIsAbsent() throws Exception {
    //        given()
    //                //                .accept("application/json")
    //                .queryParam("name", "abc")
    //                .queryParam("id", "123")
    //                .when().post("/api/foos1").then().statusCode(201).body(is("Parameters are {[name=abc], [id=123]}"));
    //
    //    }

    //    @Disabled
    @Test
    public void whenInvokePostQueryWithSpecificParamNameThenTheOriginQueryStringIsReturned() throws Exception {
        when().post("/api/foos1?id=abc&name=bar")
                .then()
                .statusCode(200)
                .body(is("ID: abc Name: bar"));

    }

}
