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

import io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyReactiveViolationException;
import io.quarkus.test.InMemoryLogHandler;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;

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

    @Test
    public void testBasicFeatures() {
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
        RestAssured.get("/hibernate-validator/test/rest-end-point-validation/plop/")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body(containsString("numeric value out of bounds"));

        if (isLogChecksPossible()) {
            // There should not be any warning/error logs since user errors do not require the developer's attention.
            assertThat(inMemoryLogHandler.getRecords())
                    .extracting(LOG_FORMATTER::formatMessage)
                    .isEmpty();

            RestAssured.when()
                    .get("/hibernate-validator/test/rest-end-point-validation/42/")
                    .then()
                    .body(is("42"));
        }
    }

    @Test
    public void testRestEndPointValidationUsingTextMediaType() {
        RestAssured.given()
                .accept(ContentType.TEXT)
                .get("/hibernate-validator/test/rest-end-point-validation/plop/")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .contentType(ContentType.TEXT)
                .body(containsString("numeric value out of bounds"));
    }

    @Test
    public void testRestEndPointValidationUsingXmlMediaType() {
        RestAssured.given()
                .accept(ContentType.XML)
                .get("/hibernate-validator/test/rest-end-point-validation/plop/")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .contentType(ContentType.XML)
                .body("validationReport.violations.message", containsString("numeric value out of bounds"));
    }

    @Test
    public void testRestEndPointValidationUsingJsonMediaType() {
        RestAssured.given()
                .accept(ContentType.JSON)
                .get("/hibernate-validator/test/rest-end-point-validation/plop/")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .contentType(ContentType.JSON)
                .body("violations[0].message", containsString("numeric value out of bounds"));
    }

    @Test
    public void testNoProduces() {
        RestAssured.given()
                .get("/hibernate-validator/test/no-produces/plop/")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body(containsString("numeric value out of bounds"));
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
                    .body(containsString(ResteasyReactiveViolationException.class.getName())) // Exception type
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

    protected boolean isTestsInJVM() {
        return true;
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

}
