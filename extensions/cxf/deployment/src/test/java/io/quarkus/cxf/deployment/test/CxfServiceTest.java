package io.quarkus.cxf.deployment.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.function.Supplier;

import javax.xml.namespace.QName;
import javax.xml.parsers.*;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;

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
                            .addAsResource("application.properties");
                }
            });

    private static QName SERVICE_NAME = new QName("http://test.deployment.cxf.quarkus.io/", "FruitWebServiceImpl");
    private static QName PORT_NAME = new QName("http://test.deployment.cxf.quarkus.io/", "FruitWebServiceImplPortType");

    private static Service service;
    private FruitWebService fruitProxy;
    private FruitWebServiceImpl fruitImpl;

    @BeforeAll
    public static void init() {
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
    public void whenUsingHelloMethod_thenCorrect()
            throws XPathExpressionException, IOException, SAXException, ParserConfigurationException {
        String xml = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:tem=\"http://tempuri.org/\">\n"
                +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "      <tem:count>\n" +
                "      </tem:count>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>";
        String cnt = "";

        Response response = RestAssured.given().header("Content-Type", "text/xml").and().body(xml).when().post("/fruit");
        response.then().statusCode(200);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(response.body().asInputStream());
        doc.getDocumentElement().normalize();
        XPath xpath = XPathFactory.newInstance().newXPath();

        cnt = xpath.compile("/Envelope/Body/countResponse/return").evaluate(doc);

        Assertions.assertEquals("2", cnt);
    }
}
