package io.quarkus.undertow.test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;
import io.restassured.RestAssured;

/**
 * The default error page must not leak the message of an uncaught exception in prod mode, where stack traces
 * are disabled. Messages explicitly passed to {@link HttpServletResponse#sendError(int, String)} are chosen by
 * the application and must still be shown.
 */
public class ErrorPageInformationExposureProdModeTestCase {

    static final String SENSITIVE_MESSAGE = "jdbc:postgresql://internal-db.prod:5432/customers user=admin";

    @RegisterExtension
    static final QuarkusProdModeTest runner = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ThrowingServlet.class, SendErrorServlet.class))
            .setRun(true);

    @Test
    public void testExceptionMessageIsNotLeakedInHtml() {
        RestAssured.given().accept("text/html")
                .when().get("/throwing").then()
                .statusCode(500)
                .body(not(containsString(SENSITIVE_MESSAGE)))
                .body(containsString("Error id"));
    }

    @Test
    public void testExceptionMessageIsNotLeakedInJson() {
        RestAssured.given().accept("application/json")
                .when().get("/throwing").then()
                .statusCode(500)
                .body(not(containsString("internal-db.prod")))
                .body(containsString("Error id"));
    }

    @Test
    public void testSendErrorMessageIsStillShown() {
        RestAssured.given().accept("application/json")
                .when().get("/send-error").then()
                .statusCode(500)
                .body(containsString("application chosen message"));
    }

    @WebServlet(urlPatterns = "/throwing")
    public static class ThrowingServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            throw new RuntimeException(SENSITIVE_MESSAGE);
        }
    }

    @WebServlet(urlPatterns = "/send-error")
    public static class SendErrorServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.sendError(500, "application chosen message");
        }
    }
}
