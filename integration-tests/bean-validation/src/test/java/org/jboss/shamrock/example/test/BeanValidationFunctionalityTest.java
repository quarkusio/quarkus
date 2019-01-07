package org.jboss.shamrock.example.test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.jboss.shamrock.test.ShamrockTest;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.RestAssured;

/**
 * Test various Bean Validation operations running in Shamrock
 */
@RunWith(ShamrockTest.class)
public class BeanValidationFunctionalityTest {

    @Test
    public void testBasicFeatures() throws Exception {
        StringBuilder expected = new StringBuilder();
        expected.append("failed: additionalEmails[0].<list element> (must be a well-formed email address)").append(", ")
                .append("categorizedEmails<K>[a].<map key> (length must be between 3 and 2147483647)").append(", ")
                .append("categorizedEmails[a].<map value>[0].<list element> (must be a well-formed email address)").append(", ")
                .append("email (must be a well-formed email address)").append(", ")
                .append("score (must be greater than or equal to 0)").append("\n");
        expected.append("passed");

        RestAssured.when()
                .get("/bean-validation/test/basic-features")
                .then()
                        .body(is(expected.toString()));
    }

    @Test
    public void testCustomClassLevelConstraint() throws Exception {
        StringBuilder expected = new StringBuilder();
        expected.append("failed:  (invalid MyOtherBean)").append("\n");
        expected.append("passed");

        RestAssured.when()
                .get("/bean-validation/test/custom-class-level-constraint")
                .then()
                        .body(is(expected.toString()));
    }

    @Test
    public void testCDIBeanMethodValidation() {
        StringBuilder expected = new StringBuilder();
        expected.append("passed").append("\n");
        expected.append("failed: greeting.arg0 (must not be null)");

        RestAssured.when()
                .get("/bean-validation/test/cdi-bean-method-validation")
                .then()
                        .body(is(expected.toString()));
    }

    @Test
    public void testRestEndPointValidation() {
        RestAssured.when()
                .get("/bean-validation/test/rest-end-point-validation/plop/")
                .then()
                        .statusCode(400)
                        .body(containsString("numeric value out of bounds"));

        RestAssured.when()
                .get("/bean-validation/test/rest-end-point-validation/42/")
                .then()
                        .body(is("42"));
    }
}
