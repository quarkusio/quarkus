package io.quarkus.spring.web.test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class BasicMappingTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(SomeClass.class, Greeting.class, TestController.class, ResponseEntityController.class,
                            ResponseStatusController.class, GreetingControllerWithNoRequestMapping.class));

    @Test
    public void verifyGetWithQueryParam() {
        when().get(TestController.CONTROLLER_PATH + "/hello?name=people")
                .then()
                .statusCode(200)
                .contentType("text/plain")
                .body(is("hello people"));
    }

    @Test
    public void verifyRequestMappingWithNoMethod() {
        when().get(TestController.CONTROLLER_PATH + "/hello4?name=people")
                .then()
                .statusCode(200)
                .contentType("text/plain")
                .body(is("hello people"));
    }

    @Test
    public void verifyGetToMethodWithoutForwardSlash() {
        when().get(TestController.CONTROLLER_PATH + "/yolo")
                .then()
                .statusCode(200)
                .body(is("yolo"));
    }

    @Test
    public void verifyGetUsingDefaultValue() {
        when().get(TestController.CONTROLLER_PATH + "/hello2")
                .then()
                .statusCode(200)
                .body(is("hello world"));
    }

    @Test
    public void verifyGetUsingNonLatinChars() {
        when().get(TestController.CONTROLLER_PATH + "/hello3?name=Γιώργος")
                .then()
                .statusCode(200)
                .body(is("hello Γιώργος"));
    }

    @Test
    public void verifyPathWithWildcard() {
        when().get(TestController.CONTROLLER_PATH + "/wildcard/whatever/world")
                .then()
                .statusCode(200)
                .body(is("world"));
    }

    @Test
    public void verifyPathWithMultipleWildcards() {
        when().get(TestController.CONTROLLER_PATH + "/wildcard2/something/folks/somethingelse")
                .then()
                .statusCode(200)
                .body(is("folks"));
    }

    @Test
    public void verifyPathWithAntStyleWildCard() {
        when().get(TestController.CONTROLLER_PATH + "/antwildcard/whatever/we/want")
                .then()
                .statusCode(200)
                .body(is("ant"));
    }

    @Test
    public void verifyPathWithCharacterWildCard() {
        for (char c : new char[] { 't', 'r' }) {
            when().get(TestController.CONTROLLER_PATH + String.format("/ca%cs", c))
                    .then()
                    .statusCode(200)
                    .body(is("single"));
        }
    }

    @Test
    public void verifyPathWithMultipleCharacterWildCards() {
        for (String path : new String[] { "/cars/shop/info", "/cart/show/info" }) {
            when().get(TestController.CONTROLLER_PATH + path)
                    .then()
                    .statusCode(200)
                    .body(is("multiple"));
        }
    }

    @Test
    public void verifyPathVariableTypeConversion() {
        when().get(TestController.CONTROLLER_PATH + "/int/9")
                .then()
                .statusCode(200)
                .body(is("10"));
    }

    @Test
    public void verifyJsonGetWithPathParamAndGettingMapping() {
        when().get(TestController.CONTROLLER_PATH + "/json/dummy")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("message", is("dummy"));
    }

    @Test
    public void verifyJsonOnRequestMappingGetWithPathParamAndRequestMapping() {
        when().get(TestController.CONTROLLER_PATH + "/json2/dummy")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("message", is("dummy"));
    }

    @Test
    public void verifyJsonPostWithPostMapping() {
        given().body("{\"message\": \"hi\"}")
                .contentType("application/json")
                .when().post(TestController.CONTROLLER_PATH + "/json")
                .then()
                .statusCode(200)
                .contentType("text/plain")
                .body(is("hi"));
    }

    @Test
    public void verifyJsonPostWithRequestMapping() {
        given().body("{\"message\": \"hi\"}")
                .contentType("application/json")
                .when().post(TestController.CONTROLLER_PATH + "/json2")
                .then()
                .statusCode(200)
                .contentType("text/plain")
                .body(is("hi"));
    }

    @Test
    public void verifyMultipleInputAndJsonResponse() {
        given().body("{\"message\": \"hi\"}")
                .contentType("application/json")
                .when().put(TestController.CONTROLLER_PATH + "/json3?suffix=!")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("message", is("hi!"));
    }

    @Test
    public void verifyEmptyContentResponseEntity() {
        when().get(ResponseEntityController.CONTROLLER_PATH + "/noContent")
                .then()
                .statusCode(204);
    }

    @Test
    public void verifyStringContentResponseEntity() {
        when().get(ResponseEntityController.CONTROLLER_PATH + "/string")
                .then()
                .statusCode(200)
                .contentType("text/plain")
                .body(is("hello world"));
    }

    @Test
    public void verifyJsonContentResponseEntity() {
        when().get(ResponseEntityController.CONTROLLER_PATH + "/json")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("message", is("dummy"))
                .header("custom-header", "somevalue");
    }

    @Test
    public void verifyJsonContentResponseEntityWithoutType() {
        when().get(ResponseEntityController.CONTROLLER_PATH + "/json2")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("message", is("dummy"));
    }

    @Test
    public void verifyEmptyContentResponseStatus() {
        when().get(ResponseStatusController.CONTROLLER_PATH + "/noContent")
                .then()
                .statusCode(200);
    }

    @Test
    public void verifyStringResponseStatus() {
        when().get(ResponseStatusController.CONTROLLER_PATH + "/string")
                .then()
                .statusCode(202)
                .contentType("text/plain")
                .body(is("accepted"));
    }

    @Test
    public void verifyControllerWithoutRequestMapping() {
        when().get("/hello")
                .then()
                .statusCode(200)
                .contentType("text/plain")
                .body(is("hello world"));
    }
}
