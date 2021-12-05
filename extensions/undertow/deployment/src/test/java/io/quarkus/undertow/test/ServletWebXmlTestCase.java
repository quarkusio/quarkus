package io.quarkus.undertow.test;

import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ServletWebXmlTestCase {

    static final String WEB_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "\n" +
            "<web-app version=\"3.0\"\n" +
            "         xmlns=\"http://java.sun.com/xml/ns/javaee\"\n" +
            "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"\n"
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
            "  </servlet-mapping>\n" +
            "\n" +
            "  <mime-mapping>\n" +
            "    <extension>wasm</extension>\n" +
            "    <mime-type>application/wasm</mime-type>\n" +
            "  </mime-mapping>" +
            "</web-app>";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(WebXmlServlet.class)
                    .addAsManifestResource(new StringAsset(WEB_XML), "web.xml")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "resources/test.wasm"));

    @Test
    public void testWebXmlServlet() {
        RestAssured.when().get("/mapped").then()
                .statusCode(200)
                .body(is("web xml servlet"));
    }

    @Test
    public void testMimeMapping() {
        RestAssured.when().get("/test.wasm").then()
                .statusCode(200)
                .contentType(is("application/wasm"));
    }
}
