package io.quarkus.resteasy.reactive.server.test.resource.basic;

import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Supplier;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.server.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class SubResourceMediaTypeTest {
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);
                    war.addClasses(StoreResource.class);
                    war.addClasses(AddressesResource.class);
                    return war;
                }
            });

    @Test
    public void basicTest() throws IOException {
        // Test that produces and consumes on sub resource takes effect
        {
            Client client = ClientBuilder.newClient();
            Response response = client.target(
                    PortProviderUtil.generateURL(
                            "/store/addresses",
                            SubResourceMediaTypeTest.class.getSimpleName()))
                    .request().accept("text/csv").post(Entity.xml("<resultCount>1</resultCount>"));
            Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            Assertions.assertEquals("name1,street1,city1,state1,zip", response.readEntity(String.class),
                    "Wrong content of response");
            response.close();
            client.close();
        }

        // Test that produces and consumes on sub resource takes effect
        {
            Client client = ClientBuilder.newClient();
            Response response = client.target(
                    PortProviderUtil.generateURL(
                            "/store/addresses",
                            SubResourceMediaTypeTest.class.getSimpleName()))
                    .request().accept("application/xml").post(Entity.xml("<resultCount>1</resultCount>"));
            Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            Assertions.assertEquals("<addresses><address><name>name1</name></address></addresses>",
                    response.readEntity(String.class),
                    "Wrong content of response");
            response.close();
            client.close();
        }

        // Test that the produces(text/html) on the locator does not influence the
        // produced response types of getAsHTMLWithDefaultResultCount, which should be text/plain, since quarkus-rest
        // automatically uses that type for String if none other declared
        {
            Client client = ClientBuilder.newClient();
            Response response = client.target(
                    PortProviderUtil.generateURL(
                            "/store/addresses",
                            SubResourceMediaTypeTest.class.getSimpleName()))
                    .request().accept("text/html").get();
            Assertions.assertEquals(Response.Status.NOT_ACCEPTABLE.getStatusCode(), response.getStatus());
            response.close();
            client.close();
        }

        // Test file upload, since that was the test case from 44922
        // quarkus-rest does not implement EntityPart.Builder right now, therefore use restassured for this case
        {
            RestAssured.given().accept("text/plain").multiPart("file", "name1,street1,city1,state1,zip")
                    .post("/store/addresses").then().statusCode(200).body(equalTo("name1,street1,city1,state1,zip"));
        }
    }

    @Path("store")
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.TEXT_HTML)
    public static class StoreResource {
        @Path("addresses")
        public AddressesResource get() {
            return new AddressesResource();
        }
    }

    public static class AddressesResource {
        @POST
        @Produces("text/csv")
        @Consumes(MediaType.APPLICATION_XML)
        public String getAsCSV(String body) {
            return "name1,street1,city1,state1,zip";
        }

        @POST
        @Produces(MediaType.APPLICATION_XML)
        @Consumes(MediaType.APPLICATION_XML)
        public String getAsXML(String body) {
            return "<addresses><address><name>name1</name></address></addresses>";
        }

        @GET
        public String getAsHTMLWithDefaultResultCount() {
            return "<body><h1>name1</h1></body>";
        }

        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.TEXT_PLAIN)
        public String uploadAddresses(@RestForm("file") FileUpload file) throws IOException {
            return Files.readString(file.uploadedFile());
        }
    }
}
