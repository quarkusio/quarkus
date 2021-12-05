package io.quarkus.undertow.test;

import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * tests a web fragment. Normally this would be in a different jar however we don't really have that facility yet in
 * QuarkusUnitTest
 */
public class ServletWebFragmentXmlTestCase {

    static final String WEB_FRAGMENT_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "\n" +
            "<web-fragment version=\"3.0\"\n" +
            "         xmlns=\"http://java.sun.com/xml/ns/javaee\"\n" +
            "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-fragment_3_0.xsd\"\n"
            +
            "         metadata-complete=\"false\">\n" +
            "<servlet>\n" +
            "    <servlet-name>mapped</servlet-name>\n" +
            "    <servlet-class>" + WebXmlServlet.class.getName() + "</servlet-class>\n" +
            "  </servlet>\n" +
            "\n" +
            "  <servlet-mapping>\n" +
            "    <servlet-name>mapped</servlet-name>\n" +
            "    <url-pattern>/mapped</url-pattern>\n" +
            "  </servlet-mapping>" +
            "</web-fragment>";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(WebXmlServlet.class)
                    .addAsManifestResource(new StringAsset(WEB_FRAGMENT_XML), "web-fragment.xml"));

    @Test
    public void testWebFragment() {
        RestAssured.when().get("/mapped").then()
                .statusCode(200)
                .body(is("web xml servlet"));
    }

}
