package io.quarkus.cxf.deployment.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Supplier;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

public class CxfServiceTest {

    @RegisterExtension
    public static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClass(HelloWebServiceImpl.class)
                            .addClass(HelloWebService.class)
                            .addAsResource("application.properties");
                }
            });

    private static QName SERVICE_NAME = new QName("http://test.deployment.cxf.quarkus.io/", "HelloWebServiceImpl");
    private static QName PORT_NAME = new QName("http://test.deployment.cxf.quarkus.io/", "HelloWebServiceImplPortType");

    private Service service;
    private HelloWebService helloProxy;
    private HelloWebServiceImpl helloImpl;

    {
        service = Service.create(SERVICE_NAME);
        final String endpointAddress = "http://localhost:8080/hello";
        service.addPort(PORT_NAME, SOAPBinding.SOAP11HTTP_BINDING, endpointAddress);
    }

    @BeforeEach
    public void reinstantiateBaeldungInstances() {
        helloImpl = new HelloWebServiceImpl();
        helloProxy = service.getPort(PORT_NAME, HelloWebService.class);
    }

    @Test
    public void whenUsingHelloMethod_thenCorrect() {
        final String endpointResponse = helloProxy.sayHi("Quarkus");
        final String localResponse = helloImpl.sayHi("Quarkus");
        assertEquals(localResponse, endpointResponse);
    }
}
