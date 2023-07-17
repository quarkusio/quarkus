package io.quarkus.undertow.test;

import static org.hamcrest.CoreMatchers.is;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ServletWebXmlInitParamsTestCase {

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
            "    <servlet-class>" + WebXmlInitParamsServlet.class.getName() + "</servlet-class>\n" +
            "    <init-param>\n" +
            "      <param-name>ThisIsParameterName1</param-name>\n" +
            "      <param-value>ThisIsParameterValue1</param-value>\n" +
            "    </init-param>\n" +
            "    <init-param>\n" +
            "      <param-name>ThisIsParameterName2</param-name>\n" +
            "      <param-value>ThisIsParameterValue2</param-value>\n" +
            "    </init-param>\n" +
            "  </servlet>\n" +
            "\n" +
            "  <servlet-mapping>\n" +
            "    <servlet-name>mapped</servlet-name>\n" +
            "    <url-pattern>/mapped</url-pattern>\n" +
            "  </servlet-mapping>\n" +
            "\n" +
            "<filter>\n" +
            "  <filter-name>MyTestFilter</filter-name>\n" +
            "  <filter-class>" + WebXmlInitParamsFilter.class.getName() + "</filter-class>\n" +
            "  <init-param>\n" +
            "    <param-name>MyFilterParamName1</param-name>\n" +
            "    <param-value>MyFilterParamValue1</param-value>\n" +
            "  </init-param>\n" +
            "  <init-param>\n" +
            "    <param-name>MyFilterParamName2</param-name>\n" +
            "    <param-value>MyFilterParamValue2</param-value>\n" +
            "  </init-param>\n" +
            "</filter>\n" +
            "<filter-mapping>\n" +
            "  <filter-name>MyTestFilter</filter-name>\n" +
            "  <url-pattern>/mapped</url-pattern>\n" +
            "</filter-mapping>\n" +
            "\n" +
            "  <context-param>\n" +
            "    <param-name>MyContextParamName1</param-name>\n" +
            "    <param-value>MyContextParamValue1</param-value>\n" +
            "  </context-param>\n" +
            "\n" +
            "  <context-param>\n" +
            "    <param-name>MyContextParamName2</param-name>\n" +
            "    <param-value>MyContextParamValue2</param-value>\n" +
            "  </context-param>\n" +
            "\n" +
            "</web-app>";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(WebXmlInitParamsServlet.class, WebXmlInitParamsFilter.class)
                    .addAsManifestResource(new StringAsset(WEB_XML), "web.xml"));

    @Test
    public void testWebXmlServlet() {
        RestAssured.when().get("/mapped").then()
                .statusCode(200)
                .body(is("invoked-before-chain\n" +
                        "MyFilterParamValue1\n" +
                        "MyFilterParamValue2\n" +
                        "ThisIsParameterValue1\n" +
                        "ThisIsParameterValue2\n" +
                        "MyContextParamValue1\n" +
                        "MyContextParamValue2\n" +
                        "invoked-after-chain\n"));
    }
}
