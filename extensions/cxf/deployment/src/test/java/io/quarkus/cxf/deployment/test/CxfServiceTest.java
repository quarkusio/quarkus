package io.quarkus.cxf.deployment.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import java.util.function.Supplier;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cxf.deployment.test.impl.FruitImpl;
import io.quarkus.cxf.deployment.test.impl.FruitWebServiceImpl;
import io.quarkus.test.QuarkusDevModeTest;

public class CxfServiceTest {

    @RegisterExtension
    public static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClass(FruitWebServiceImpl.class)
                            .addClass(FruitWebService.class)
                            .addClass(Fruit.class)
                            .addClass(FruitImpl.class)
                            .addClass(FruitAdapter.class)
                            .addAsResource("application.properties");
                }
            });

    private static QName SERVICE_NAME = new QName("http://test.deployment.cxf.quarkus.io/", "FruitWebServiceImpl");
    private static QName PORT_NAME = new QName("http://test.deployment.cxf.quarkus.io/", "FruitWebServiceImplPortType");

    private Service service;
    private FruitWebService fruitProxy;
    private FruitWebServiceImpl fruitImpl;

    {
        service = Service.create(SERVICE_NAME);
        final String endpointAddress = "http://localhost:8080/fruit";
        service.addPort(PORT_NAME, SOAPBinding.SOAP11HTTP_BINDING, endpointAddress);
    }

    @BeforeEach
    public void reinstantiateBaeldungInstances() {
        fruitImpl = new FruitWebServiceImpl();
        fruitProxy = service.getPort(PORT_NAME, FruitWebService.class);
    }

    @Test
    public void whenUsingHelloMethod_thenCorrect() {
        final Set<Fruit> endpointResponse = fruitProxy.list();
        final Set<Fruit> localResponse = fruitImpl.list();
        assertEquals(localResponse, endpointResponse);
    }
}
