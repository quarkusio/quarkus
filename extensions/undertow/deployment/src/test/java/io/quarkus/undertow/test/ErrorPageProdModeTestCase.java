package io.quarkus.undertow.test;

import static org.hamcrest.Matchers.is;

import java.io.IOException;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;
import io.restassured.RestAssured;

/**
 * In prod mode, an application mapping its own 500 error page does not get the Quarkus exception handler,
 * so this cannot be tested with {@code QuarkusExtensionTest}, which runs in test mode.
 */
public class ErrorPageProdModeTestCase {

    static final String WEB_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "\n" +
            "<web-app version=\"3.0\"\n" +
            "         xmlns=\"http://java.sun.com/xml/ns/javaee\"\n" +
            "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"\n"
            +
            "         metadata-complete=\"false\">\n" +
            "  <servlet>\n" +
            "    <servlet-name>error-page</servlet-name>\n" +
            "    <servlet-class>" + ErrorPageServlet.class.getName() + "</servlet-class>\n" +
            "  </servlet>\n" +
            "\n" +
            "  <servlet-mapping>\n" +
            "    <servlet-name>error-page</servlet-name>\n" +
            "    <url-pattern>/error-page</url-pattern>\n" +
            "  </servlet-mapping>\n" +
            "\n" +
            "  <error-page>\n" +
            "    <error-code>500</error-code>\n" +
            "    <location>/error-page</location>\n" +
            "  </error-page>\n" +
            "</web-app>";

    @RegisterExtension
    static final QuarkusProdModeTest runner = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ErrorServlet.class, ErrorPageServlet.class)
                    .addAsManifestResource(new StringAsset(WEB_XML), "web.xml"))
            .setRun(true);

    @Test
    public void testMapped500ErrorPage() {
        RestAssured.when().get("/error").then()
                .statusCode(500)
                .body(is("custom error page"));
    }

    public static class ErrorPageServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.getWriter().write("custom error page");
        }
    }
}
