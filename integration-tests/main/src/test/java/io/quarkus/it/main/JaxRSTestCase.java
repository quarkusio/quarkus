package io.quarkus.it.main;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.zip.GZIPOutputStream;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import io.quarkus.it.rest.TestResource;
import io.quarkus.it.rest.TestResourceWithVariable;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.DisabledOnIntegrationTest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;

@QuarkusTest
public class JaxRSTestCase {

    @Test
    public void testJAXRS() {
        RestAssured.when().get("/test").then().body(is("TEST"));
    }

    @Test
    @TestHTTPEndpoint(TestResource.class)
    public void testInteger() {
        RestAssured.when().get("/int/10").then().body(is("11"));
    }

    @Test
    @DisabledOnIntegrationTest //the native image tests will bind to 0.0.0.0 as the image is a production image, but the test datasource in the JVM will want to use localhost
    public void testConfigInjectionOfPort() {
        String host = ConfigProvider.getConfig().getOptionalValue("quarkus.http.host", String.class).orElse("0.0.0.0");
        RestAssured.when().get("/test/config/host").then().body(is(host));
    }

    @Test
    public void testConfigInjectionOfMessage() {
        RestAssured.when().get("/test/config/message").then().body(is("A message"));
    }

    @Test
    public void testConfigInjectionOfStringArray() {
        RestAssured.when().get("/test/config/names").then().body(is("quarkus,redhat"));
    }

    @Test
    public void testAnnotatedInterface() {
        RestAssured.when().get("/interface").then().body(is("interface endpoint"));

    }

    @Test
    public void testNonCdiBeansAreApplicationScoped() {
        RestAssured.when().get("/test/count").then().body(is("1"));
        RestAssured.when().get("/test/count").then().body(is("2"));
        RestAssured.when().get("/test/count").then().body(is("3"));
    }

    @Test
    public void testContextInjection() {
        RestAssured.when().get("/test/request-test").then().body(is("/test/request-test"));
    }

    @Test
    public void testJsonp() {
        RestAssured.when().get("/test/jsonp").then()
                .body("name", is("Stuart"),
                        "value", is("A Value"));
    }

    @Test
    public void testJackson() {
        RestAssured.when().get("/test/jackson").then()
                .body("name", is("Stuart"),
                        "value", is("A Value"));
    }

    @Test
    public void testJaxb() throws Exception {
        try {
            // in the native image case, the right parser is not chosen, despite the content-type being correct
            RestAssured.defaultParser = Parser.XML;

            RestAssured.when().get("/test/xml").then()
                    .body("xmlObject.value.text()", is("A Value"));
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void testJaxbConsumption() throws Exception {
        TestResource.XmlObject xmlObject = new TestResource.XmlObject();
        xmlObject.setValue("test");

        StringWriter writer = new StringWriter();
        JAXBContext context = JAXBContext.newInstance(TestResource.XmlObject.class);
        Marshaller m = context.createMarshaller();
        m.marshal(xmlObject, writer);

        try {
            // in the native image case, the right parser is not chosen, despite the content-type being correct
            RestAssured.defaultParser = Parser.XML;

            RestAssured.given().contentType("application/xml").body(writer.toString()).when().post("/test/consumeXml").then()
                    .body(is("test"));
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void testBytecodeTransformation() {
        RestAssured.when().get("/test/transformed").then().body(is("Transformed Endpoint"));
    }

    @Test
    public void testRxJava() {
        RestAssured.when().get("/test/rx").then().body(is("Hello"));
    }

    @Test
    public void testCustomProvider() {
        RestAssured.when().get("/test/fooprovider").then().body(is("hello-foo"));
    }

    @Test
    public void testComplexObjectReflectionRegistration() {
        RestAssured.when().get("/test/complex").then()
                .body("$.size()", is(1),
                        "[0].value", is("component value"),
                        "[0].collectionTypes.size()", is(1),
                        "[0].collectionTypes[0].value", is("collection type"),
                        "[0].subComponent.data.size()", is(1),
                        "[0].subComponent.data[0]", is("sub component list value"));
    }

    @Test
    public void testSubclass() {
        RestAssured.when().get("/test/subclass").then()
                .body("name", is("my name"),
                        "value", is("my value"));
    }

    @Test
    public void testImplementor() {
        RestAssured.when().get("/test/implementor").then()
                .body("name", is("my name"),
                        "value", is("my value"));
    }

    @Test
    public void testResponse() {
        RestAssured.when().get("/test/response").then()
                .body("name", is("my entity name"),
                        "value", is("my entity value"));
    }

    @Test
    public void testFromJson() {
        RestAssured.when().get("/test/from-json").then()
                .body("name", is("my entity name"),
                        "value", is("my entity value"));
    }

    @Test
    public void testOpenApiSchemaResponse() {
        RestAssured.when().get("/test/openapi/responses").then()
                .body("name", is("my openapi entity name"));
    }

    @Test
    public void testOpenApiSchemaResponsesV1() {
        RestAssured.when().get("/test/openapi/responses/v1").then()
                .body("name", is("my openapi entity version one name"));
    }

    @Test
    public void testOpenApiSchemaResponseV2() {
        RestAssured.when().get("/test/openapi/responses/v2").then()
                .body("name", is("my openapi entity version two name"));
    }

    @Test
    public void testOpenApiSchema() {
        RestAssured.when().get("/test/openapi/schema").then()
                .body("name", is("my openapi schema"));
    }

    @Test
    public void testOpenApiResponsesWithNoContent() {
        RestAssured.when().get("/test/openapi/no-content/api-responses").then()
                .body(emptyString());
    }

    @Test
    public void testOpenApiResponseWithNoContent() {
        RestAssured.when().get("/test/openapi/no-content/api-response").then()
                .body(emptyString());
    }

    @Test
    public void testGzipConfig() throws Exception {
        //gzip.maxInput set to 10 and expects 413 status code
        ByteArrayOutputStream obj = new ByteArrayOutputStream(12);
        GZIPOutputStream gzip = new GZIPOutputStream(obj);
        gzip.write("1234567890AB".getBytes("UTF-8"));
        gzip.close();
        RestAssured.given()
                .header("Content-Encoding", "gzip")
                .body(obj.toByteArray())
                .post("/test/gzip")
                .then().statusCode(413);
        obj.close();
    }

    @Test
    public void testReturnTypeWithGenericArgument() {
        RestAssured.when().get("/envelope/payload").then()
                .body(containsString("content"), containsString("hello"));
    }

    @Test
    public void testMaxEntityServerOptionConfig() {
        // maxEntitySize set to 1K sending a payload larger than the limit should return 400 status code
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1100; i++) {
            sb.append("q");
        }

        try {
            RestAssured.given()
                    .body(sb.toString())
                    .post("/test/max-body-size")
                    .then().statusCode(413);
        } catch (Exception ignore) {
            //because the connection is force closed after sending the 413
            //sometimes the client can return an IOException if it is still
            //trying to send the body
        }

        // while sending a payload within the limit should return 200
        RestAssured.given()
                .body("Hello Quarkus")
                .post("/test/max-body-size")
                .then().statusCode(200);
    }

    @Test
    public void testSSE() throws Exception {
        RestAssured.when().get("/sse/stream")
                .then()
                .contentType("text/event-stream")
                .body(containsString("0"),
                        containsString("1"),
                        containsString("2"));

        RestAssured.when().get("/sse/stream-html")
                .then()
                .contentType("text/event-stream")
                .body(containsString("<html><body>0</body></html>"),
                        containsString("<html><body>1</body></html>"),
                        containsString("<html><body>2</body></html>"));

        RestAssured.when().get("/sse/stream-xml")
                .then()
                .contentType("text/event-stream")
                .body(containsString("<settings><foo bar=\"0\"/></settings>"),
                        containsString("<settings><foo bar=\"1\"/></settings>"),
                        containsString("<settings><foo bar=\"2\"/></settings>"));
    }

    @Test
    @TestHTTPEndpoint(TestResourceWithVariable.class)
    public void testPathVariable() {
        RestAssured.given()
                .pathParam("name", "yolo")
                .when().get()
                .then()
                .statusCode(200)
                .body(is("hello yolo"));
    }
}
