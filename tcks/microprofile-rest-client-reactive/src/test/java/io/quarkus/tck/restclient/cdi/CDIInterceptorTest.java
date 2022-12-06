package io.quarkus.tck.restclient.cdi;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.eclipse.microprofile.rest.client.tck.providers.ReturnWithURLRequestFilter;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

public class CDIInterceptorTest extends Arquillian {

    @Inject
    @RestClient
    private ClientWithURIAndInterceptor client;

    @Deployment
    public static WebArchive createDeployment() {
        String simpleName = CDIInterceptorTest.class.getSimpleName();
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, simpleName + ".jar")
                .addClasses(ClientWithURIAndInterceptor.class,
                        Loggable.class,
                        LoggableInterceptor.class,
                        ReturnWithURLRequestFilter.class)
                .addAsManifestResource(new StringAsset(
                        "<beans xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
                                "       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                                "       xsi:schemaLocation=\"" +
                                "          http://java.sun.com/xml/ns/javaee" +
                                "          http://java.sun.com/xml/ns/javaee/beans_1_0.xsd\">" +
                                "       <interceptors>" +
                                "           <class>io.quarkus.tck.restclient.cdi.LoggableInterceptor</class>"
                                +
                                "       </interceptors>" +
                                "</beans>"),
                        "beans.xml");
        return ShrinkWrap.create(WebArchive.class, simpleName + ".war")
                .addAsLibrary(jar)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @AfterTest
    public void cleanUp() {
        LoggableInterceptor.reset();
    }

    @Test
    public void testInterceptorInvoked() throws Exception {
        String expectedResponse = "GET http://localhost:5017/myBaseUri/hello";
        assertEquals(client.get(), expectedResponse);

        assertTrue(ClientWithURIAndInterceptor.class.isAssignableFrom(LoggableInterceptor.getInvocationClass()),
                "Invalid declaring class of the intercepted method. Expected " + ClientWithURIAndInterceptor.class.getName()
                        + " or a subclass, found: " + LoggableInterceptor.getInvocationClass());
        assertEquals(LoggableInterceptor.getInvocationMethod(), "get");
        assertEquals(LoggableInterceptor.getResult(), expectedResponse);
    }

    @Test
    public void testInterceptorNotInvokedWhenNoAnnotationApplied() throws Exception {
        String expectedResponse = "GET http://localhost:5017/myBaseUri/hello";
        assertEquals(client.getNoInterceptor(), expectedResponse);

        assertNull(LoggableInterceptor.getInvocationClass());
        assertNull(LoggableInterceptor.getInvocationMethod());
        assertNull(LoggableInterceptor.getResult());
    }
}
