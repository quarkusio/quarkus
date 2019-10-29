package io.quarkus.undertow.test;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * tests that basic web.xml security is applied. We don't actually have
 * the security subsystem installed here, so this is the fallback behaviour that
 * will always deny
 */
public class ServletWebXmlSecurityTestCase {

    static final String WEB_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "\n" +
            "<web-app version=\"3.0\"\n" +
            "         xmlns=\"http://java.sun.com/xml/ns/javaee\"\n" +
            "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"\n"
            +
            "         metadata-complete=\"false\">\n" +
            "\n" +
            "<security-constraint>\n" +
            "    <web-resource-collection>\n" +
            "        <web-resource-name>test</web-resource-name>\n" +
            "        <url-pattern>/secure/*</url-pattern>\n" +
            "        <http-method>GET</http-method>\n" +
            "        <http-method>POST</http-method>\n" +
            "    </web-resource-collection>\n" +
            "    <auth-constraint>\n" +
            "        <role-name>admin</role-name>\n" +
            "    </auth-constraint>\n" +
            "</security-constraint>" +
            "</web-app>";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(SecuredServlet.class)
                    .addAsManifestResource(new StringAsset(WEB_XML), "web.xml"));

    @Test
    public void testWebXmlSecurityConstraints() {
        RestAssured.when().get("/secure/servlet").then()
                .statusCode(401);
    }

}
