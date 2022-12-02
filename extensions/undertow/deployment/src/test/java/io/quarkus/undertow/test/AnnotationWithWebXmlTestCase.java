package io.quarkus.undertow.test;

import static org.hamcrest.Matchers.containsString;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class AnnotationWithWebXmlTestCase {

    static final String WEB_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "\n" +
            "<web-app version=\"3.0\"\n" +
            "         xmlns=\"http://java.sun.com/xml/ns/javaee\"\n" +
            "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"\n"
            +
            "         metadata-complete=\"false\">\n" +
            "  <servlet>\n" +
            "    <servlet-name>mapped</servlet-name>\n" +
            "    <servlet-class>" + WebXmlServlet.class.getName() + "</servlet-class>\n" +
            "  </servlet>\n" +
            "\n" +
            "  <servlet-mapping>\n" +
            "    <servlet-name>mapped</servlet-name>\n" +
            "    <url-pattern>/mapped</url-pattern>\n" +
            "  </servlet-mapping>" +
            "\n" +
            "  <filter> \n" +
            "    <filter-name>mapped-filter</filter-name>\n" +
            "    <filter-class>" + WebXmlFilter.class.getName() + "</filter-class> \n" +
            "  </filter> \n" +
            "  <filter-mapping> \n" +
            "    <filter-name>mapped-filter</filter-name>\n" +
            "    <url-pattern>/*</url-pattern> \n" +
            "  </filter-mapping> " +
            "\n" +
            "  <listener>\n" +
            "    <listener-class>" + WebXmlListener.class.getName() + "</listener-class>\n" +
            "  </listener>" +
            "</web-app>";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(WebXmlServlet.class, TestServlet.class,
                            WebXmlFilter.class, AnnotatedFilter.class,
                            WebXmlListener.class, AnnotatedListener.class, TestGreeter.class)
                    .addAsManifestResource(new StringAsset(WEB_XML), "web.xml"));

    @Test
    public void testWebXmlServlet() {
        RestAssured.when().get("/mapped").then()
                .statusCode(200)
                .body(containsString("web xml listener"))
                .body(containsString("annotated listener"))
                .body(containsString("web xml filter"))
                .body(containsString("annotated filter"))
                .body(containsString("web xml servlet"));
    }

    @Test
    public void testAnnotatedServlet() {
        RestAssured.when().get("/test").then()
                .statusCode(200)
                .body(containsString("web xml listener"))
                .body(containsString("annotated listener"))
                .body(containsString("web xml filter"))
                .body(containsString("annotated filter"))
                .body(containsString("test servlet"));
    }

}
