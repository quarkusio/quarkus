package io.quarkus.it.hibernate.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;

import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response;

import org.jboss.logmanager.formatters.PatternFormatter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyViolationExceptionImpl;
import io.quarkus.test.InMemoryLogHandler;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;

/**
 * Test various Bean Validation operations running in Quarkus
 */
@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
public class HibernateValidatorFunctionalityTest {
    private static final Formatter LOG_FORMATTER = new PatternFormatter("%s");
    private static final java.util.logging.Logger rootLogger = LogManager.getLogManager().getLogger("io.quarkus");
    private static final InMemoryLogHandler inMemoryLogHandler = new InMemoryLogHandler(
            record -> record.getLevel().intValue() >= Level.WARNING.intValue());

    @BeforeEach
    public void setLogHandler() {
        if (isLogChecksPossible()) {
            inMemoryLogHandler.getRecords().clear();
            rootLogger.addHandler(inMemoryLogHandler);
        }
    }

    @AfterEach
    public void removeLogHandler() {
        if (isLogChecksPossible()) {
            rootLogger.removeHandler(inMemoryLogHandler);
        }
    }

    private boolean isInternalErrorExceptionLeakedInQuarkusErrorHandlerResponse() {
        // True by default when running tests in the JVM, with the test/dev profile.
        // When running in native mode, the application runs with the prod profile (sort of?):
        // the stack trace isn't included in QuarkusErrorHandler responses.
        return isTestsInJVM();
    }

    private boolean isLogChecksPossible() {
        // True by default when running tests in the JVM, with the test/dev profile.
        // When running in native mode, we cannot easily spy on logs.
        return isTestsInJVM();
    }

    protected boolean isTestsInJVM() {
        return true;
    }

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
    public void testCDIBeanMethodValidationUncaught() {
        // https://github.com/quarkusio/quarkus/issues/9174
        // Uncaught constraint validation exceptions thrown by user beans
        // are internal errors and should be reported as such.

        // The returned body should be the standard one produced by QuarkusErrorHandler,
        // with all the necessary information (stack trace, ...).
        ValidatableResponse response = RestAssured.when()
                .get("/hibernate-validator/test/cdi-bean-method-validation-uncaught")
                .then()
                .body(containsString("Error id"))
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        if (isInternalErrorExceptionLeakedInQuarkusErrorHandlerResponse()) {
            response
                    .body(containsString(ConstraintViolationException.class.getName())) // Exception type
                    .body(containsString("message: must not be null")) // Exception message
                    .body(containsString("property path: greeting.name"))
                    .body(containsString(EnhancedGreetingService.class.getName()))
                    .body(containsString(HibernateValidatorTestResource.class.getName())); // Stack trace
        }

        if (isLogChecksPossible()) {
            // There should also be some logs to raise the internal error to the developer's attention.
            assertThat(inMemoryLogHandler.getRecords())
                    .extracting(LOG_FORMATTER::formatMessage)
                    .hasSize(1);
            assertThat(inMemoryLogHandler.getRecords())
                    .element(0).satisfies(record -> {
                        assertThat(record.getLevel()).isEqualTo(Level.SEVERE);
                        assertThat(LOG_FORMATTER.formatMessage(record))
                                .contains(
                                        "HTTP Request to /hibernate-validator/test/cdi-bean-method-validation-uncaught failed, error id:");
                    });
        }
    }

    @Test
    public void testRestEndPointValidation() {
        // https://github.com/quarkusio/quarkus/issues/9174
        // Constraint validation exceptions thrown by Resteasy and related to input values
        // are user errors and should be reported as such.

        // Bad request
        RestAssured.when()
                .get("/hibernate-validator/test/rest-end-point-validation/plop/")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body(containsString("numeric value out of bounds"));

        if (isLogChecksPossible()) {
            // There should not be any warning/error logs since user errors do not require the developer's attention.
            assertThat(inMemoryLogHandler.getRecords())
                    .extracting(LOG_FORMATTER::formatMessage)
                    .isEmpty();
        }

        RestAssured.when()
                .get("/hibernate-validator/test/rest-end-point-validation/42/")
                .then()
                .body(is("42"));
    }

    @Test
    public void testRestEndPointReturnValueValidation() {
        // https://github.com/quarkusio/quarkus/issues/9174
        // Constraint validation exceptions thrown by Resteasy and related to return values
        // are internal errors and should be reported as such.

        // The returned body should be the standard one produced by QuarkusErrorHandler,
        // with all the necessary information (stack trace, ...).
        ValidatableResponse response = RestAssured.when()
                .get("/hibernate-validator/test/rest-end-point-return-value-validation/plop/")
                .then()
                .body(containsString("Error id"))
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        if (isInternalErrorExceptionLeakedInQuarkusErrorHandlerResponse()) {
            response
                    .body(containsString(ResteasyViolationExceptionImpl.class.getName())) // Exception type
                    .body(containsString("numeric value out of bounds")) // Exception message
                    .body(containsString("testRestEndPointReturnValueValidation.<return value>"))
                    .body(containsString(HibernateValidatorTestResource.class.getName())); // Stack trace
        }

        if (isLogChecksPossible()) {
            // There should also be some logs to raise the internal error to the developer's attention.
            assertThat(inMemoryLogHandler.getRecords())
                    .extracting(LOG_FORMATTER::formatMessage)
                    .hasSize(1);
            assertThat(inMemoryLogHandler.getRecords())
                    .element(0).satisfies(record -> {
                        assertThat(record.getLevel()).isEqualTo(Level.SEVERE);
                        assertThat(LOG_FORMATTER.formatMessage(record))
                                .contains(
                                        "HTTP Request to /hibernate-validator/test/rest-end-point-return-value-validation/plop/ failed, error id:");
                    });
        }

        RestAssured.when()
                .get("/hibernate-validator/test/rest-end-point-validation/42/")
                .then()
                .body(is("42"));
    }

    @Test
    public void testRestEndPointInterfaceValidation() {
        RestAssured.when()
                .get("/hibernate-validator/test/rest-end-point-interface-validation/plop/")
                .then()
                .statusCode(400)
                .body(containsString("numeric value out of bounds"));

        RestAssured.when()
                .get("/hibernate-validator/test/rest-end-point-interface-validation/42/")
                .then()
                .body(is("42"));
    }

    @Test
    public void testRestEndPointInterfaceValidationWithAnnotationOnImplMethod() {
        RestAssured.when()
                .get("/hibernate-validator/test/rest-end-point-interface-validation-annotation-on-impl-method/plop/")
                .then()
                .statusCode(400)
                .body(containsString("numeric value out of bounds"));

        RestAssured.when()
                .get("/hibernate-validator/test/rest-end-point-interface-validation-annotation-on-impl-method/42/")
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

        if (isLogChecksPossible()) {
            assertThat(inMemoryLogHandler.getRecords()).isEmpty();
        }

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

    @Test
    public void testHibernateOrmIntegration() {
        RestAssured.when()
                .get("/hibernate-validator/test/test-hibernate-orm-integration")
                .then()
                .statusCode(500);
    }

    @Test
    public void testInheritance() {
        RestAssured.given()
                .get("/books/science")
                .then()
                .statusCode(400)
                .body(containsString("must not be null"));
    }

    @Test
    public void testRestEndPointValidationGroups_parameters() {
        // PUT: input id must be null
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"id\": 1, \"name\": \"b\"}")
                .when()
                .put("/hibernate-validator/test/rest-end-point-validation-groups/")
                .then()
                .statusCode(400)
                .body(containsString("must be null"));
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"b\"}")
                .when()
                .put("/hibernate-validator/test/rest-end-point-validation-groups/")
                .then()
                .statusCode(200)
                .body(containsString("passed"));

        // POST: input id must not be null
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"b\"}")
                .when()
                .post("/hibernate-validator/test/rest-end-point-validation-groups/")
                .then()
                .statusCode(400)
                .body(containsString("must not be null"));
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"id\": 1, \"name\": \"b\"}")
                .when()
                .post("/hibernate-validator/test/rest-end-point-validation-groups/")
                .then()
                .statusCode(200)
                .body(containsString("passed"));

        // Also check that constraints using the default group still work
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .put("/hibernate-validator/test/rest-end-point-validation-groups/")
                .then()
                .statusCode(400)
                .body(containsString("must not be null"));
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"id\":1}")
                .when()
                .post("/hibernate-validator/test/rest-end-point-validation-groups/")
                .then()
                .statusCode(400)
                .body(containsString("must not be null"));
    }

    @Test
    public void testRestEndPointValidationGroups_result() {
        // GET: deleted must be false
        ValidatableResponse response = RestAssured.given()
                .param("simulateDeleted", true)
                .when()
                .get("/hibernate-validator/test/rest-end-point-validation-groups/1/")
                .then()
                .statusCode(500);
        if (isInternalErrorExceptionLeakedInQuarkusErrorHandlerResponse()) {
            response.body(containsString("must be false"));
        }
        RestAssured.given()
                .param("simulateDeleted", false)
                .when()
                .get("/hibernate-validator/test/rest-end-point-validation-groups/1/")
                .then()
                .statusCode(200)
                .body(containsString("\"deleted\":false"));

        // DELETE: deleted must be true
        response = RestAssured.given()
                .param("simulateDeleted", false)
                .when()
                .delete("/hibernate-validator/test/rest-end-point-validation-groups/1/")
                .then()
                .statusCode(500);
        if (isInternalErrorExceptionLeakedInQuarkusErrorHandlerResponse()) {
            response.body(containsString("must be true"));
        }
        RestAssured.given()
                .param("simulateDeleted", true)
                .when()
                .delete("/hibernate-validator/test/rest-end-point-validation-groups/1/")
                .then()
                .statusCode(200)
                .body(containsString("\"deleted\":true"));

        // Also check that constraints using the default group still work
        response = RestAssured.given()
                .param("simulateDeleted", false)
                .param("simulateNullName", true)
                .when()
                .get("/hibernate-validator/test/rest-end-point-validation-groups/1/")
                .then()
                .statusCode(500);
        if (isInternalErrorExceptionLeakedInQuarkusErrorHandlerResponse()) {
            response.body(containsString("must not be null"));
        }
        response = RestAssured.given()
                .param("simulateDeleted", true)
                .param("simulateNullName", true)
                .when()
                .delete("/hibernate-validator/test/rest-end-point-validation-groups/1/")
                .then()
                .statusCode(500);
        if (isInternalErrorExceptionLeakedInQuarkusErrorHandlerResponse()) {
            response.body(containsString("must not be null"));
        }
    }
}
