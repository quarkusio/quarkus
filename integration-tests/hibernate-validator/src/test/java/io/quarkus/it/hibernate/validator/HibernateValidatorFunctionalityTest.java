package io.quarkus.it.hibernate.validator;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Test various Bean Validation operations running in Quarkus
 */

@QuarkusTest
public class HibernateValidatorFunctionalityTest {

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
                .get("/hibernate-validator/test/basic-features")
                .then()
                .body(is(expected.toString()));
    }

    @Test
    public void testCustomClassLevelConstraint() throws Exception {
        StringBuilder expected = new StringBuilder();
        expected.append("failed:  (invalid MyOtherBean)").append("\n");
        expected.append("passed");

        RestAssured.when()
                .get("/hibernate-validator/test/custom-class-level-constraint")
                .then()
                .body(is(expected.toString()));
    }

    @Test
    public void testCDIBeanMethodValidation() {
        StringBuilder expected = new StringBuilder();
        expected.append("passed").append("\n");
        expected.append("failed: greeting.arg0 (must not be null)");

        RestAssured.when()
                .get("/hibernate-validator/test/cdi-bean-method-validation")
                .then()
                .body(is(expected.toString()));
    }

    @Test
    public void testRestEndPointValidation() {
        RestAssured.when()
                .get("/hibernate-validator/test/rest-end-point-validation/plop/")
                .then()
                .statusCode(400)
                .body(containsString("numeric value out of bounds"));

        RestAssured.when()
                .get("/hibernate-validator/test/rest-end-point-validation/42/")
                .then()
                .body(is("42"));
    }

    @Test
    public void testNoProduces() {
        RestAssured.when()
                .get("/hibernate-validator/test/no-produces/plop/")
                .then()
                .statusCode(400)
                .body(containsString("numeric value out of bounds"));
    }
}
