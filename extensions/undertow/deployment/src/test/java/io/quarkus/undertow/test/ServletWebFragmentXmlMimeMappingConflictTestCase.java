package io.quarkus.undertow.test;

import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * A web fragment defining a mime mapping that conflicts with the one in web.xml must not fail the build with an NPE.
 */
public class ServletWebFragmentXmlMimeMappingConflictTestCase {

    static final String WEB_FRAGMENT_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<web-fragment version=\"3.0\"\n" +
            "         xmlns=\"http://java.sun.com/xml/ns/javaee\"\n" +
            "         metadata-complete=\"false\">\n" +
            "  <servlet>\n" +
            "    <servlet-name>mapped</servlet-name>\n" +
            "    <servlet-class>" + WebXmlServlet.class.getName() + "</servlet-class>\n" +
            "  </servlet>\n" +
            "  <servlet-mapping>\n" +
            "    <servlet-name>mapped</servlet-name>\n" +
            "    <url-pattern>/mapped</url-pattern>\n" +
            "  </servlet-mapping>\n" +
            "  <mime-mapping>\n" +
            "    <extension>woff2</extension>\n" +
            "    <mime-type>application/x-font-woff2</mime-type>\n" +
            "  </mime-mapping>\n" +
            "</web-fragment>";

    static final String WEB_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<web-app version=\"3.0\"\n" +
            "         xmlns=\"http://java.sun.com/xml/ns/javaee\"\n" +
            "         metadata-complete=\"false\">\n" +
            "  <mime-mapping>\n" +
            "    <extension>woff2</extension>\n" +
            "    <mime-type>font/woff2</mime-type>\n" +
            "  </mime-mapping>\n" +
            "</web-app>";

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(WebXmlServlet.class)
                    .addAsManifestResource(new StringAsset(WEB_FRAGMENT_XML), "web-fragment.xml")
                    .addAsManifestResource(new StringAsset(WEB_XML), "web.xml"));

    @Test
    public void testConflictingMimeMappingDoesNotFailBuild() {
        RestAssured.when().get("/mapped").then()
                .statusCode(200)
                .body(is("web xml servlet"));
    }

}
