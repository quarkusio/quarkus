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
        expected.append("failed: greeting.name (must not be null)");

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
    public void testRestEndPointGenericMethodValidation() {
        RestAssured.when()
                .get("/hibernate-validator/test/rest-end-point-generic-method-validation/9999999/")
                .then()
                .statusCode(400)
                .body(containsString("numeric value out of bounds"));

        RestAssured.when()
                .get("/hibernate-validator/test/rest-end-point-generic-method-validation/42/")
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

    @Test
    public void testInjection() throws Exception {
        StringBuilder expected = new StringBuilder();
        expected.append("passed").append("\n");
        expected.append("failed: value (InjectedConstraintValidatorConstraint violation)");

        RestAssured.when()
                .get("/hibernate-validator/test/injection")
                .then()
                .body(is(expected.toString()));
    }

    @Test
    public void testInheritedImplementsConstraints() {
        StringBuilder expected = new StringBuilder();
        expected.append("passed").append("\n")
                .append("failed: echoZipCode.zipCode (size must be between 5 and 5)");

        RestAssured.when()
                .get("/hibernate-validator/test/test-inherited-implements-constraints")
                .then()
                .body(is(expected.toString()));
    }

    @Test
    public void testInheritedExtendsConstraints() {
        StringBuilder expected = new StringBuilder();
        expected.append("passed").append("\n");
        expected.append("failed: greeting.name (must not be null)");

        RestAssured.when()
                .get("/hibernate-validator/test/test-inherited-extends-constraints")
                .then()
                .body(is(expected.toString()));
    }

    @Test
    public void testValidationMessageLocale() {
        RestAssured.given()
                .header("Accept-Language", "en-US;q=0.25,hr-HR;q=1,fr-FR;q=0.5")
                .when()
                .get("/hibernate-validator/test/test-validation-message-locale/1")
                .then()
                .body(containsString("Vrijednost ne zadovoljava uzorak"));
    }

    @Test
    public void testValidationMessageDefaultLocale() {
        RestAssured.given()
                .when()
                .get("/hibernate-validator/test/test-validation-message-locale/1")
                .then()
                .body(containsString("Value is not in line with the pattern"));
    }

    @Test
    public void testManualValidationMessageLocale() {
        RestAssured.given()
                .header("Accept-Language", "en-US;q=0.25,hr-HR;q=1,fr-FR;q=0.5")
                .header("Content-Type", "application/json")
                .when()
                .body("{\"name\": \"b\"}")
                .post("/hibernate-validator/test/test-manual-validation-message-locale")
                .then()
                .body(containsString("Vrijednost ne zadovoljava uzorak"));
    }

}
