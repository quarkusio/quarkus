package io.quarkus.resteasy.test.security;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.function.Supplier;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import io.restassured.RestAssured;
import io.restassured.filter.cookie.CookieFilter;
import io.restassured.matcher.RestAssuredMatchers;
import io.restassured.specification.RequestSpecification;

public class AuthMechRequest {

    final String path;
    final String expectedHeaderKey;
    String expectedBody;
    Matcher<Object> expectedHeaderVal;
    int expectedStatus;
    boolean authRequired;
    Supplier<RequestSpecification> requestSpecification;
    Supplier<RequestSpecification> unauthorizedRequestSpec;
    Supplier<RequestSpecification> unauthenticatedRequestSpec = RestAssured::given;
    Supplier<RequestSpecification> requestUsingOtherAuthMech;

    public AuthMechRequest(String path) {
        this.path = path;
        this.expectedHeaderKey = AnnotationBasedAuthMechanismSelectionTest.CustomBasicAuthMechanism.CUSTOM_AUTH_HEADER_KEY;
        expectedBody = path.substring(path.lastIndexOf('/') + 1);
        expectedStatus = 200;
        authRequired = true;
    }

    AuthMechRequest basic() {
        requestSpecification = AuthMechRequest::requestWithBasicAuth;
        unauthorizedRequestSpec = AuthMechRequest::requestWithBasicAuthUser;
        requestUsingOtherAuthMech = () -> requestWithFormAuth("admin");
        expectedHeaderVal = nullValue();
        return this;
    }

    AuthMechRequest custom() {
        basic();
        expectedHeaderVal = notNullValue();
        return this;
    }

    AuthMechRequest noRbacAnnotation() {
        // no RBAC annotation == @Authenticated
        // response contains security identity principal name to verify authenticated sec. identity
        authRequest();
        expectedBody = "admin";
        return this;
    }

    AuthMechRequest defaultAuthMech() {
        // when we do not explicitly select auth mechanism, even custom auth mechanism is invoked, but no
        // Authorization header is present, so it's not used in the end
        expectedHeaderVal = Matchers.anything();
        // naturally, all mechanisms are going to be accepted
        requestUsingOtherAuthMech = null;
        return this;
    }

    AuthMechRequest denyPolicy() {
        expectedStatus = 403;
        expectedBody = "";
        return this;
    }

    AuthMechRequest authRequest() {
        // endpoint annotated with @Authenticated will not check roles, so no authZ
        unauthorizedRequestSpec = null;
        return this;
    }

    AuthMechRequest pathAnnotationDeclaredOnInterface() {
        // RBAC annotations on interfaces are ignored
        authRequired = false;
        return this;
    }

    AuthMechRequest form() {
        requestSpecification = () -> requestWithFormAuth("admin");
        unauthorizedRequestSpec = () -> requestWithFormAuth("user");
        requestUsingOtherAuthMech = AuthMechRequest::requestWithBasicAuth;
        expectedHeaderVal = nullValue();
        return this;
    }

    static RequestSpecification requestWithBasicAuth() {
        return given().auth().preemptive().basic("admin", "admin");
    }

    static RequestSpecification requestWithFormAuth(String user) {
        CookieFilter cookies = new CookieFilter();
        RestAssured
                .given()
                .filter(cookies)
                .when()
                .formParam("j_username", user)
                .formParam("j_password", user)
                .post("/j_security_check")
                .then()
                .assertThat()
                .statusCode(200)
                .cookie("quarkus-credential",
                        RestAssuredMatchers.detailedCookie().value(notNullValue()).secured(false));
        return RestAssured
                .given()
                .filter(cookies);
    }

    static RequestSpecification requestWithBasicAuthUser() {
        return given().auth().preemptive().basic("user", "user");
    }
}
