package org.jboss.shamrock.undertow.test;

import org.jboss.shamrock.test.Deployment;
import org.jboss.shamrock.test.ShamrockUnitTest;
import org.jboss.shamrock.test.URLResponse;
import org.jboss.shamrock.test.URLTester;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ShamrockUnitTest.class)
public class ServletWebXmlTestCase {

    @Deployment
    public static JavaArchive deploy() {
        return ShrinkWrap.create(JavaArchive.class)
                .addClasses(WebXmlServlet.class)
                .addAsManifestResource(new StringAsset(WEB_XML), "web.xml");
    }

    @Test
    public void testWebXmlServlet() {
        URLResponse resp = URLTester.relative("/mapped").invokeURL();
        Assert.assertEquals(200, resp.statusCode());
        Assert.assertEquals("web xml servlet", resp.asString());
    }


    static final String WEB_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "\n" +
                    "<web-app version=\"3.0\"\n" +
                    "         xmlns=\"http://java.sun.com/xml/ns/javaee\"\n" +
                    "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                    "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"\n" +
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
                    "</web-app>";
}
